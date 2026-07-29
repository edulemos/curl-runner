package com.curlrunner.engine;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.curlrunner.model.ExecutionRun;
import com.curlrunner.model.ExecutionStatus;
import com.curlrunner.model.RequestResult;
import com.curlrunner.parser.ParsedRequest;
import com.curlrunner.repository.ExecutionRunRepository;
import com.curlrunner.repository.RequestResultRepository;

@Service
public class ExecutionEngineServiceImpl implements ExecutionEngineService {

    private final HttpClient httpClient;
    private final ExecutionRunRepository executionRunRepository;
    private final RequestResultRepository requestResultRepository;
    private final ConcurrentHashMap<Long, Boolean> cancellationFlags = new ConcurrentHashMap<>();

    public ExecutionEngineServiceImpl(ExecutionRunRepository executionRunRepository,
                                      RequestResultRepository requestResultRepository) {
        this.executionRunRepository = executionRunRepository;
        this.requestResultRepository = requestResultRepository;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public ExecutionRun execute(ParsedRequest request, ExecutionConfig config, ExecutionProgressListener listener) {
        // Create and persist ExecutionRun with status PENDING
        ExecutionRun run = new ExecutionRun();
        run.setCurlCommand(request.getUrl());
        run.setTotalIterations(config.getEffectiveIterations());
        run.setDelayMs(config.getDelayMs());
        run.setParallel(config.isParallel());
        run.setStatus(ExecutionStatus.PENDING);

        // Save to get the generated ID
        run = executionRunRepository.save(run);

        // Register cancellation flag for this run
        cancellationFlags.put(run.getId(), false);

        // Transition PENDING → RUNNING
        run.setStatus(ExecutionStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run = executionRunRepository.save(run);

        try {
            // Execute based on configuration
            if (config.isParallel()) {
                executeParallel(run, request, config, listener);
            } else {
                executeSequential(run, request, config, listener);
            }

            // After execution: determine final status
            if (isCancelled(run.getId())) {
                // Transition RUNNING → CANCELLED
                run.setStatus(ExecutionStatus.CANCELLED);
            } else {
                // Transition RUNNING → COMPLETED
                run.setStatus(ExecutionStatus.COMPLETED);
            }

        } catch (Exception e) {
            // Transition RUNNING → ERROR
            run.setStatus(ExecutionStatus.ERROR);
            listener.onError(e.getMessage());
        }

        // Calculate aggregated results from saved RequestResults
        Iterable<com.curlrunner.model.RequestResult> results = requestResultRepository.findByRunId(run.getId());
        int successCount = 0;
        int failureCount = 0;
        long totalResponseTime = 0;
        int totalResults = 0;

        for (com.curlrunner.model.RequestResult result : results) {
            if (Boolean.TRUE.equals(result.getSuccess())) {
                successCount++;
            } else {
                failureCount++;
            }
            if (result.getResponseTimeMs() != null) {
                totalResponseTime += result.getResponseTimeMs();
            }
            totalResults++;
        }

        run.setSuccessCount(successCount);
        run.setFailureCount(failureCount);
        run.setAvgResponseTimeMs(totalResults > 0 ? totalResponseTime / totalResults : 0L);

        // Set completedAt timestamp
        run.setCompletedAt(LocalDateTime.now());

        // Persist the final state
        run = executionRunRepository.save(run);

        // Notify listener of completion
        listener.onExecutionComplete(run);

        // Cleanup cancellation flag
        cancellationFlags.remove(run.getId());

        return run;
    }

    @Override
    public void cancel(Long runId) {
        cancellationFlags.put(runId, true);

        // Also update the run's status to CANCELLED in the database if it exists
        executionRunRepository.findById(runId).ifPresent(run -> {
            if (run.getStatus() == ExecutionStatus.RUNNING) {
                run.setStatus(ExecutionStatus.CANCELLED);
                run.setCompletedAt(LocalDateTime.now());
                executionRunRepository.save(run);
            }
        });
    }

    @Override
    public ExecutionStatus getStatus(Long runId) {
        return executionRunRepository.findById(runId)
                .map(ExecutionRun::getStatus)
                .orElse(null);
    }

    /**
     * Checks whether the execution for the given run has been cancelled.
     *
     * @param runId the ID of the ExecutionRun to check
     * @return true if cancellation was requested
     */
    private boolean isCancelled(Long runId) {
        return Boolean.TRUE.equals(cancellationFlags.get(runId));
    }

    /**
     * Executes iterations sequentially with optional delay between them.
     * Supports data-driven execution where each iteration uses a different data row.
     */
    private void executeSequential(ExecutionRun run, ParsedRequest request,
                                    ExecutionConfig config, ExecutionProgressListener listener) {
        int totalIterations = config.getEffectiveIterations();

        for (int i = 1; i <= totalIterations; i++) {
            if (isCancelled(run.getId())) {
                break;
            }

            // Build HttpRequest for this iteration (with or without data)
            java.util.Map<String, Object> dataRow = null;
            if (config.isDataDriven()) {
                dataRow = config.getDataList().get(i - 1);
            }
            HttpRequest httpRequest = request.toHttpRequest(dataRow);

            RequestResult result = executeIteration(run.getId(), i, httpRequest);
            requestResultRepository.save(result);
            listener.onIterationComplete(result);
            listener.onProgress(i, totalIterations);

            if (i < totalIterations && config.getDelayMs() > 0) {
                try {
                    Thread.sleep(config.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Executes iterations in parallel using a fixed thread pool with CompletableFuture.
     * Supports data-driven execution where each iteration uses a different data row.
     */
    private void executeParallel(ExecutionRun run, ParsedRequest request,
                                  ExecutionConfig config, ExecutionProgressListener listener) {
        int totalIterations = config.getEffectiveIterations();
        int poolSize = Math.min(totalIterations, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<RequestResult>> futures = new ArrayList<>();
            AtomicInteger completedCount = new AtomicInteger(0);

            for (int i = 1; i <= totalIterations; i++) {
                final int iteration = i;
                final long delayOffset = (long) (i - 1) * config.getDelayMs();
                final java.util.Map<String, Object> dataRow = config.isDataDriven()
                        ? config.getDataList().get(i - 1) : null;

                CompletableFuture<RequestResult> future = CompletableFuture.supplyAsync(() -> {
                    if (isCancelled(run.getId())) {
                        return null;
                    }

                    if (delayOffset > 0) {
                        try {
                            Thread.sleep(delayOffset);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return null;
                        }
                    }

                    if (isCancelled(run.getId())) {
                        return null;
                    }

                    HttpRequest httpRequest = request.toHttpRequest(dataRow);
                    RequestResult result = executeIteration(run.getId(), iteration, httpRequest);

                    int completed = completedCount.incrementAndGet();
                    listener.onIterationComplete(result);
                    listener.onProgress(completed, totalIterations);

                    return result;
                }, executor);

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results and save to database
            for (CompletableFuture<RequestResult> future : futures) {
                RequestResult result = future.join();
                if (result != null) {
                    requestResultRepository.save(result);
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Executes a single HTTP request iteration and returns the result.
     *
     * @param runId           the ID of the parent ExecutionRun
     * @param iterationNumber the iteration number (1-based)
     * @param httpRequest     the HTTP request to send
     * @return a RequestResult with timing, status, and success information
     */
    private RequestResult executeIteration(Long runId, int iterationNumber, HttpRequest httpRequest) {
        RequestResult result = new RequestResult();
        result.setRunId(runId);
        result.setIterationNumber(iterationNumber);
        result.setExecutedAt(LocalDateTime.now());

        long startTime = System.nanoTime();

        try {
            HttpResponse<String> response = httpClient.send(
                httpRequest, HttpResponse.BodyHandlers.ofString()
            );

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

            result.setStatusCode(response.statusCode());
            result.setResponseTimeMs(elapsedMs);
            result.setResponseSizeBytes(response.body().getBytes().length);
            result.setSuccess(response.statusCode() < 400);
            result.setResponseBody(response.body());

        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            result.setResponseTimeMs(elapsedMs);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }
}
