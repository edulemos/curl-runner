package com.curlrunner.engine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.curlrunner.model.RequestResult;
import com.curlrunner.repository.ExecutionRunRepository;
import com.curlrunner.repository.RequestResultRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultsAggregatorServiceTest {

    @Mock
    private RequestResultRepository requestResultRepository;

    @Mock
    private ExecutionRunRepository executionRunRepository;

    @InjectMocks
    private ResultsAggregatorServiceImpl aggregatorService;

    // --- Helper methods ---

    private RequestResult createResult(Long runId, int iteration, int statusCode,
                                       long responseTimeMs, boolean success,
                                       LocalDateTime executedAt) {
        RequestResult result = new RequestResult();
        result.setRunId(runId);
        result.setIterationNumber(iteration);
        result.setStatusCode(statusCode);
        result.setResponseTimeMs(responseTimeMs);
        result.setSuccess(success);
        result.setExecutedAt(executedAt);
        return result;
    }

    private RequestResult createSuccessResult(Long runId, int iteration, int statusCode,
                                              long responseTimeMs, LocalDateTime executedAt) {
        return createResult(runId, iteration, statusCode, responseTimeMs, true, executedAt);
    }

    private RequestResult createFailureResult(Long runId, int iteration, int statusCode,
                                              long responseTimeMs, LocalDateTime executedAt) {
        return createResult(runId, iteration, statusCode, responseTimeMs, false, executedAt);
    }

    // --- Test cases ---

    @Test
    void aggregate_emptyResults_returnsAllZeros() {
        Long runId = 1L;
        when(requestResultRepository.findByRunId(runId)).thenReturn(Collections.emptyList());

        AggregatedResults results = aggregatorService.aggregate(runId);

        assertEquals(runId, results.getRunId());
        assertEquals(0, results.getTotalRequests());
        assertEquals(0, results.getSuccessCount());
        assertEquals(0, results.getFailureCount());
        assertEquals(0, results.getAvgResponseTimeMs());
        assertEquals(0, results.getMinResponseTimeMs());
        assertEquals(0, results.getMaxResponseTimeMs());
        assertEquals(0, results.getP50ResponseTimeMs());
        assertEquals(0, results.getP95ResponseTimeMs());
        assertEquals(0, results.getP99ResponseTimeMs());
        assertEquals(0, results.getTotalDurationMs());
        assertEquals(0.0, results.getRequestsPerSecond());
        assertTrue(results.getStatusCodeDistribution().isEmpty());
    }

    @Test
    void aggregate_singleResult_allMetricsAreSameValue() {
        Long runId = 2L;
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        RequestResult single = createSuccessResult(runId, 1, 200, 150L, now);

        when(requestResultRepository.findByRunId(runId)).thenReturn(List.of(single));

        AggregatedResults results = aggregatorService.aggregate(runId);

        assertEquals(1, results.getTotalRequests());
        assertEquals(1, results.getSuccessCount());
        assertEquals(0, results.getFailureCount());
        assertEquals(150, results.getAvgResponseTimeMs());
        assertEquals(150, results.getMinResponseTimeMs());
        assertEquals(150, results.getMaxResponseTimeMs());
        assertEquals(150, results.getP50ResponseTimeMs());
        assertEquals(150, results.getP95ResponseTimeMs());
        assertEquals(150, results.getP99ResponseTimeMs());
    }

    @Test
    void aggregate_multipleResults_correctAvgMinMax() {
        Long runId = 3L;
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        List<RequestResult> results = Arrays.asList(
                createSuccessResult(runId, 1, 200, 100L, base),
                createSuccessResult(runId, 2, 200, 200L, base.plusSeconds(1)),
                createSuccessResult(runId, 3, 200, 300L, base.plusSeconds(2))
        );

        when(requestResultRepository.findByRunId(runId)).thenReturn(results);

        AggregatedResults aggregated = aggregatorService.aggregate(runId);

        assertEquals(3, aggregated.getTotalRequests());
        assertEquals(200, aggregated.getAvgResponseTimeMs()); // (100+200+300)/3 = 200
        assertEquals(100, aggregated.getMinResponseTimeMs());
        assertEquals(300, aggregated.getMaxResponseTimeMs());
    }

    @Test
    void aggregate_successAndFailureCounting() {
        Long runId = 4L;
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        List<RequestResult> results = Arrays.asList(
                createSuccessResult(runId, 1, 200, 50L, base),
                createSuccessResult(runId, 2, 201, 60L, base.plusSeconds(1)),
                createFailureResult(runId, 3, 500, 70L, base.plusSeconds(2)),
                createSuccessResult(runId, 4, 200, 80L, base.plusSeconds(3)),
                createFailureResult(runId, 5, 503, 90L, base.plusSeconds(4))
        );

        when(requestResultRepository.findByRunId(runId)).thenReturn(results);

        AggregatedResults aggregated = aggregatorService.aggregate(runId);

        assertEquals(5, aggregated.getTotalRequests());
        assertEquals(3, aggregated.getSuccessCount());
        assertEquals(2, aggregated.getFailureCount());
    }

    @Test
    void aggregate_percentileCalculation_withKnownSortedData() {
        Long runId = 5L;
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        // 20 results with response times 10, 20, 30, ..., 200
        List<RequestResult> results = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            results.add(createSuccessResult(runId, i, 200, i * 10L, base.plusSeconds(i)));
        }

        when(requestResultRepository.findByRunId(runId)).thenReturn(results);

        AggregatedResults aggregated = aggregatorService.aggregate(runId);

        // Percentile calculation: index = ceil(percentile/100 * size) - 1
        // p50: ceil(0.50 * 20) - 1 = ceil(10) - 1 = 9 → sorted[9] = 100
        assertEquals(100, aggregated.getP50ResponseTimeMs());
        // p95: ceil(0.95 * 20) - 1 = ceil(19) - 1 = 18 → sorted[18] = 190
        assertEquals(190, aggregated.getP95ResponseTimeMs());
        // p99: ceil(0.99 * 20) - 1 = ceil(19.8) - 1 = 20 - 1 = 19 → sorted[19] = 200
        assertEquals(200, aggregated.getP99ResponseTimeMs());
    }

    @Test
    void aggregate_statusCodeDistribution_correctGrouping() {
        Long runId = 6L;
        LocalDateTime base = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        List<RequestResult> results = Arrays.asList(
                createSuccessResult(runId, 1, 200, 50L, base),
                createSuccessResult(runId, 2, 200, 60L, base.plusSeconds(1)),
                createSuccessResult(runId, 3, 201, 70L, base.plusSeconds(2)),
                createFailureResult(runId, 4, 404, 80L, base.plusSeconds(3)),
                createFailureResult(runId, 5, 500, 90L, base.plusSeconds(4)),
                createFailureResult(runId, 6, 500, 100L, base.plusSeconds(5))
        );

        when(requestResultRepository.findByRunId(runId)).thenReturn(results);

        AggregatedResults aggregated = aggregatorService.aggregate(runId);

        Map<Integer, Integer> distribution = aggregated.getStatusCodeDistribution();
        assertEquals(4, distribution.size());
        assertEquals(2, distribution.get(200));
        assertEquals(1, distribution.get(201));
        assertEquals(1, distribution.get(404));
        assertEquals(2, distribution.get(500));
    }

    @Test
    void aggregate_requestsPerSecond_correctComputation() {
        Long runId = 7L;
        // 10 requests spanning 5 seconds → 10 / 5.0 = 2.0 req/s
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime end = start.plusSeconds(5);

        List<RequestResult> results = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            // Spread timestamps between start and end
            LocalDateTime ts = (i == 1) ? start : (i == 10) ? end : start.plus(Duration.ofMillis(i * 500L));
            results.add(createSuccessResult(runId, i, 200, 50L, ts));
        }

        when(requestResultRepository.findByRunId(runId)).thenReturn(results);

        AggregatedResults aggregated = aggregatorService.aggregate(runId);

        // totalDurationMs = Duration.between(earliest, latest) = 5000ms
        assertEquals(5000, aggregated.getTotalDurationMs());
        // requestsPerSecond = 10 / (5000 / 1000.0) = 2.0
        assertEquals(2.0, aggregated.getRequestsPerSecond(), 0.001);
    }

    @Test
    void aggregate_zeroDuration_requestsPerSecondEqualsTotalRequests() {
        Long runId = 8L;
        // All requests at the same time → totalDurationMs = 0
        LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        List<RequestResult> results = Arrays.asList(
                createSuccessResult(runId, 1, 200, 50L, sameTime),
                createSuccessResult(runId, 2, 200, 60L, sameTime),
                createSuccessResult(runId, 3, 200, 70L, sameTime)
        );

        when(requestResultRepository.findByRunId(runId)).thenReturn(results);

        AggregatedResults aggregated = aggregatorService.aggregate(runId);

        assertEquals(0, aggregated.getTotalDurationMs());
        // When totalDurationMs == 0, requestsPerSecond = totalRequests
        assertEquals(3.0, aggregated.getRequestsPerSecond(), 0.001);
    }
}
