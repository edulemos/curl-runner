package com.curlrunner.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.curlrunner.model.RequestResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class ResultsExportServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void export_writesSummaryAndCompleteIterationDetails() throws Exception {
        AggregatedResults summary = new AggregatedResults();
        summary.setRunId(42L);
        summary.setTotalRequests(1);
        summary.setSuccessCount(1);
        summary.setAvgResponseTimeMs(15L);

        RequestResult result = new RequestResult();
        result.setIterationNumber(1);
        result.setStatusCode(201);
        result.setResponseTimeMs(15L);
        result.setSuccess(true);
        result.setExecutedAt(LocalDateTime.of(2026, 10, 14, 10, 30));
        result.setResponseBody("{\"id\":\"abc\"}");
        result.setRequestMethod("POST");
        result.setRequestUrl("https://api.example.com/users");
        result.setRequestHeaders("{\"Content-Type\":\"application/json\"}");
        result.setRequestBody("{\"name\":\"Carlos\"}");

        Path output = temporaryDirectory.resolve("execution-results.json");
        new ResultsExportService().export(output, summary, List.of(result));

        assertTrue(Files.exists(output));
        JsonNode document = new ObjectMapper().readTree(output.toFile());
        assertEquals(42L, document.path("summary").path("runId").asLong());
        assertEquals(1, document.path("summary").path("totalRequests").asInt());
        assertFalse(document.path("summary").has("p50ResponseTimeMs"));
        assertFalse(document.path("summary").has("p95ResponseTimeMs"));
        assertFalse(document.path("summary").has("p99ResponseTimeMs"));
        assertEquals(1, document.path("iterations").size());
        JsonNode iteration = document.path("iterations").get(0);
        assertEquals("POST", iteration.path("request").path("method").asText());
        assertEquals("https://api.example.com/users", iteration.path("request").path("url").asText());
        assertEquals("application/json", iteration.path("request").path("headers").path("Content-Type").asText());
        assertEquals("Carlos", iteration.path("request").path("body").path("name").asText());
        assertEquals(201, iteration.path("response").path("httpStatus").asInt());
        assertEquals("abc", iteration.path("response").path("body").path("id").asText());
        assertEquals(15L, iteration.path("durationMs").asLong());
        assertTrue(Files.readString(output).lines().count() == 1, "Export should be minified");
    }
}
