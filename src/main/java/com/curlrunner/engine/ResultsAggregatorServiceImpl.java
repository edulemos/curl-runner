package com.curlrunner.engine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.curlrunner.model.RequestResult;
import com.curlrunner.repository.ExecutionRunRepository;
import com.curlrunner.repository.RequestResultRepository;

@Service
public class ResultsAggregatorServiceImpl implements ResultsAggregatorService {

    private final RequestResultRepository requestResultRepository;
    private final ExecutionRunRepository executionRunRepository;

    public ResultsAggregatorServiceImpl(RequestResultRepository requestResultRepository,
                                        ExecutionRunRepository executionRunRepository) {
        this.requestResultRepository = requestResultRepository;
        this.executionRunRepository = executionRunRepository;
    }

    @Override
    public AggregatedResults aggregate(Long runId) {
        AggregatedResults results = new AggregatedResults();
        results.setRunId(runId);

        List<RequestResult> requestResults = requestResultRepository.findByRunId(runId);

        if (requestResults.isEmpty()) {
            results.setTotalRequests(0);
            results.setSuccessCount(0);
            results.setFailureCount(0);
            results.setAvgResponseTimeMs(0);
            results.setMinResponseTimeMs(0);
            results.setMaxResponseTimeMs(0);
            results.setP50ResponseTimeMs(0);
            results.setP95ResponseTimeMs(0);
            results.setP99ResponseTimeMs(0);
            results.setStatusCodeDistribution(new HashMap<>());
            results.setTotalDurationMs(0);
            results.setRequestsPerSecond(0.0);
            return results;
        }

        int totalRequests = requestResults.size();
        results.setTotalRequests(totalRequests);

        int successCount = 0;
        int failureCount = 0;
        long totalResponseTime = 0;
        long minResponseTime = Long.MAX_VALUE;
        long maxResponseTime = Long.MIN_VALUE;
        Map<Integer, Integer> statusCodeDistribution = new HashMap<>();
        List<Long> responseTimes = new ArrayList<>();
        LocalDateTime earliest = null;
        LocalDateTime latest = null;

        for (RequestResult result : requestResults) {
            if (Boolean.TRUE.equals(result.getSuccess())) {
                successCount++;
            } else {
                failureCount++;
            }

            long responseTime = result.getResponseTimeMs();
            totalResponseTime += responseTime;
            responseTimes.add(responseTime);

            if (responseTime < minResponseTime) {
                minResponseTime = responseTime;
            }
            if (responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }

            if (result.getStatusCode() != null) {
                statusCodeDistribution.merge(result.getStatusCode(), 1, Integer::sum);
            }

            LocalDateTime executedAt = result.getExecutedAt();
            if (executedAt != null) {
                if (earliest == null || executedAt.isBefore(earliest)) {
                    earliest = executedAt;
                }
                if (latest == null || executedAt.isAfter(latest)) {
                    latest = executedAt;
                }
            }
        }

        results.setSuccessCount(successCount);
        results.setFailureCount(failureCount);
        results.setAvgResponseTimeMs(totalResponseTime / totalRequests);
        results.setMinResponseTimeMs(minResponseTime);
        results.setMaxResponseTimeMs(maxResponseTime);
        results.setStatusCodeDistribution(statusCodeDistribution);

        // Sort response times for percentile calculation
        Collections.sort(responseTimes);

        results.setP50ResponseTimeMs(calculatePercentile(responseTimes, 50));
        results.setP95ResponseTimeMs(calculatePercentile(responseTimes, 95));
        results.setP99ResponseTimeMs(calculatePercentile(responseTimes, 99));

        // Calculate total duration
        long totalDurationMs = 0;
        if (earliest != null && latest != null) {
            totalDurationMs = Duration.between(earliest, latest).toMillis();
        }
        results.setTotalDurationMs(totalDurationMs);

        // Calculate requests per second
        if (totalDurationMs == 0) {
            results.setRequestsPerSecond(totalRequests);
        } else {
            results.setRequestsPerSecond(totalRequests / (totalDurationMs / 1000.0));
        }

        return results;
    }

    private long calculatePercentile(List<Long> sortedValues, int percentile) {
        int size = sortedValues.size();
        int index = (int) Math.ceil(percentile / 100.0 * size) - 1;
        index = Math.max(0, Math.min(index, size - 1));
        return sortedValues.get(index);
    }
}
