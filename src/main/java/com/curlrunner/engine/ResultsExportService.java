package com.curlrunner.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.curlrunner.model.RequestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Writes a completed execution report to a portable JSON file.
 */
@Service
public class ResultsExportService {

    private final ObjectMapper objectMapper;

    public ResultsExportService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Exports the aggregate metrics and every unabridged iteration result.
     *
     * @param target output file
     * @param summary aggregate metrics for the execution
     * @param results results for each iteration
     * @throws IOException if the report cannot be written
     */
    public void export(Path target, AggregatedResults summary, List<RequestResult> results) throws IOException {
        List<ExportedIteration> iterations = results.stream()
            .map(this::toExportedIteration)
            .toList();
        objectMapper.writeValue(target.toFile(), new ExportedResults(toReportSummary(summary), iterations));
    }

    private ReportSummary toReportSummary(AggregatedResults summary) {
        return new ReportSummary(
            summary.getRunId(),
            summary.getTotalRequests(),
            summary.getSuccessCount(),
            summary.getFailureCount(),
            summary.getAvgResponseTimeMs(),
            summary.getMinResponseTimeMs(),
            summary.getMaxResponseTimeMs(),
            summary.getStatusCodeDistribution(),
            summary.getRequestsPerSecond(),
            summary.getTotalDurationMs()
        );
    }

    /**
     * Stable JSON root structure, separated from Swing and persistence concerns.
     */
    private ExportedIteration toExportedIteration(RequestResult result) {
        return new ExportedIteration(
            result.getIterationNumber(),
            new ExportedRequest(result.getRequestMethod(), result.getRequestUrl(), parseHeaders(result.getRequestHeaders()), toJsonValue(result.getRequestBody())),
            new ExportedResponse(result.getStatusCode(), toJsonValue(result.getResponseBody()), result.getErrorMessage()),
            result.getResponseTimeMs()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseHeaders(String headers) {
        if (headers == null || headers.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(headers, Map.class);
        } catch (IOException e) {
            return Map.of("raw", headers);
        }
    }

    /**
     * Keeps JSON request/response bodies as JSON nodes, avoiding escaped JSON
     * inside the outer export document. Non-JSON content remains a string.
     */
    private Object toJsonValue(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JsonNode parsedBody = objectMapper.readTree(body);
            return parsedBody == null ? body : parsedBody;
        } catch (IOException e) {
            return body;
        }
    }

    public record ExportedResults(ReportSummary summary, List<ExportedIteration> iterations) {
    }

    /** Export-only summary; percentiles are intentionally omitted from the report. */
    public record ReportSummary(Long runId, int totalRequests, int successCount, int failureCount,
                                long avgResponseTimeMs, long minResponseTimeMs, long maxResponseTimeMs,
                                Map<Integer, Integer> statusCodeDistribution, double requestsPerSecond,
                                long totalDurationMs) {
    }

    public record ExportedIteration(int iteration, ExportedRequest request,
                                    ExportedResponse response, Long durationMs) {
    }

    public record ExportedRequest(String method, String url, Map<String, String> headers, Object body) {
    }

    public record ExportedResponse(Integer httpStatus, Object body, String error) {
    }
}
