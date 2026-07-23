package com.curlrunner.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.curlrunner.engine.AggregatedResults;
import com.curlrunner.engine.ExecutionConfig;
import com.curlrunner.engine.ExecutionEngineService;
import com.curlrunner.engine.ExecutionProgressListener;
import com.curlrunner.engine.ResultsAggregatorService;
import com.curlrunner.model.ExecutionRun;
import com.curlrunner.model.ExecutionStatus;
import com.curlrunner.model.RequestResult;
import com.curlrunner.parser.CurlParserService;
import com.curlrunner.parser.ParsedRequest;
import com.curlrunner.repository.RequestResultRepository;
import com.curlrunner.ui.InputPanel;
import com.curlrunner.ui.ResultsPanel;
import com.curlrunner.ui.SwingMainFrame;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Integration test that verifies the complete flow:
 * Parse cURL → Execute against mock server → Verify persistence → Verify aggregation.
 */
@SpringBootTest(properties = {
    "spring.main.web-application-type=none",
    "java.awt.headless=true",
    "spring.datasource.url=jdbc:h2:mem:curlrunner_integration;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
})
class FullFlowIntegrationTest {

    @MockBean
    private SwingMainFrame mainFrame;

    @MockBean
    private InputPanel inputPanel;

    @MockBean
    private ResultsPanel resultsPanel;

    @Autowired
    private CurlParserService curlParserService;

    @Autowired
    private ExecutionEngineService executionEngineService;

    @Autowired
    private ResultsAggregatorService resultsAggregatorService;

    @Autowired
    private RequestResultRepository requestResultRepository;

    @Test
    void fullFlow_parseExecutePersistAggregate() {
        // 1. Start WireMock on a random port
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMockServer.start();

        try {
            // Stub GET /api/test to return 200 with a body
            wireMockServer.stubFor(get(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"message\":\"hello\"}")));

            int port = wireMockServer.port();

            // 2. Build cURL command pointing to WireMock
            String curlCommand = "curl http://localhost:" + port + "/api/test";

            // 3. Parse the command
            ParsedRequest parsedRequest = curlParserService.parse(curlCommand);
            assertThat(parsedRequest).isNotNull();
            assertThat(parsedRequest.getUrl()).isEqualTo("http://localhost:" + port + "/api/test");
            assertThat(parsedRequest.getMethod()).isEqualTo("GET");

            // 4. Create ExecutionConfig: 5 iterations, 0 delay, sequential
            ExecutionConfig config = new ExecutionConfig(5, 0, false);

            // 5. Execute with a no-op progress listener
            ExecutionProgressListener noOpListener = new ExecutionProgressListener() {
                @Override
                public void onProgress(int completed, int total) {}
                @Override
                public void onIterationComplete(RequestResult result) {}
                @Override
                public void onExecutionComplete(ExecutionRun run) {}
                @Override
                public void onError(String message) {}
            };

            ExecutionRun run = executionEngineService.execute(parsedRequest, config, noOpListener);

            // 6. Verify ExecutionRun has status COMPLETED
            assertThat(run).isNotNull();
            assertThat(run.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);

            // 7. Verify exactly 5 RequestResults in the database for this run
            List<RequestResult> results = requestResultRepository.findByRunId(run.getId());
            assertThat(results).hasSize(5);

            // 8. Verify all results have statusCode=200 and success=true
            for (RequestResult result : results) {
                assertThat(result.getStatusCode()).isEqualTo(200);
                assertThat(result.getSuccess()).isTrue();
            }

            // 9. Aggregate results and verify
            AggregatedResults aggregated = resultsAggregatorService.aggregate(run.getId());
            assertThat(aggregated.getSuccessCount()).isEqualTo(5);
            assertThat(aggregated.getFailureCount()).isEqualTo(0);
            assertThat(aggregated.getAvgResponseTimeMs()).isGreaterThanOrEqualTo(0);

        } finally {
            wireMockServer.stop();
        }
    }
}
