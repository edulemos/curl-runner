package com.curlrunner.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.curlrunner.engine.ExecutionConfig;
import com.curlrunner.engine.ExecutionEngineService;
import com.curlrunner.engine.ExecutionProgressListener;
import com.curlrunner.model.ExecutionRun;
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
 * Integration test for parallel execution of HTTP requests.
 * Verifies thread safety and correct result counting in parallel mode.
 */
@SpringBootTest(properties = {
    "spring.main.web-application-type=none",
    "java.awt.headless=true"
})
class ParallelExecutionTest {

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
    private RequestResultRepository requestResultRepository;

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    private final ExecutionProgressListener noOpListener = new ExecutionProgressListener() {
        @Override
        public void onProgress(int completed, int total) {}

        @Override
        public void onIterationComplete(RequestResult result) {}

        @Override
        public void onExecutionComplete(ExecutionRun run) {}

        @Override
        public void onError(String message) {}
    };

    @Test
    void parallelExecutionProducesCorrectNumberOfResults() {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMockServer.start();

        try {
            // Setup WireMock stub returning 200
            wireMockServer.stubFor(get(urlEqualTo("/test-parallel"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("OK")));

            int port = wireMockServer.port();

            // Parse cURL command targeting WireMock
            String curlCommand = "curl http://localhost:" + port + "/test-parallel";
            ParsedRequest request = curlParserService.parse(curlCommand);

            // Execute with 20 iterations, 0 delay, parallel=true
            ExecutionConfig config = new ExecutionConfig(20, 0, true);
            ExecutionRun run = executionEngineService.execute(request, config, noOpListener);

            // Verify exactly 20 RequestResults exist for the run
            List<RequestResult> results = requestResultRepository.findByRunId(run.getId());
            assertThat(results).hasSize(20);

            // Verify all results have statusCode=200 and success=true
            for (RequestResult result : results) {
                assertThat(result.getStatusCode()).isEqualTo(200);
                assertThat(result.getSuccess()).isTrue();
            }
        } finally {
            wireMockServer.stop();
        }
    }

    @Test
    void parallelExecutionWithDelayDoesNotCorruptResults() {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMockServer.start();

        try {
            // Setup WireMock stub returning 200
            wireMockServer.stubFor(get(urlEqualTo("/test-delay"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("delayed response")));

            int port = wireMockServer.port();

            // Parse cURL command targeting WireMock
            String curlCommand = "curl http://localhost:" + port + "/test-delay";
            ParsedRequest request = curlParserService.parse(curlCommand);

            // Execute with 10 iterations, 50ms delay, parallel=true
            ExecutionConfig config = new ExecutionConfig(10, 50, true);
            ExecutionRun run = executionEngineService.execute(request, config, noOpListener);

            // Verify exactly 10 results
            List<RequestResult> results = requestResultRepository.findByRunId(run.getId());
            assertThat(results).hasSize(10);

            // Verify no null fields in any result
            for (RequestResult result : results) {
                assertThat(result.getRunId()).isNotNull();
                assertThat(result.getIterationNumber()).isNotNull();
                assertThat(result.getStatusCode()).isNotNull();
                assertThat(result.getResponseTimeMs()).isNotNull();
                assertThat(result.getSuccess()).isNotNull();
                assertThat(result.getExecutedAt()).isNotNull();
            }
        } finally {
            wireMockServer.stop();
        }
    }

    @Test
    void threadSafetyNoDuplicateIterationNumbers() {
        WireMockServer wireMockServer = new WireMockServer(
            WireMockConfiguration.wireMockConfig().dynamicPort()
        );
        wireMockServer.start();

        try {
            // Setup WireMock stub returning 200
            wireMockServer.stubFor(get(urlEqualTo("/test-thread-safety"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withBody("thread safe")));

            int port = wireMockServer.port();

            // Parse cURL command targeting WireMock
            String curlCommand = "curl http://localhost:" + port + "/test-thread-safety";
            ParsedRequest request = curlParserService.parse(curlCommand);

            // Execute with 50 iterations, 0 delay, parallel=true
            ExecutionConfig config = new ExecutionConfig(50, 0, true);
            ExecutionRun run = executionEngineService.execute(request, config, noOpListener);

            // Get all results
            List<RequestResult> results = requestResultRepository.findByRunId(run.getId());
            assertThat(results).hasSize(50);

            // Verify all iteration numbers are unique (1 to 50)
            Set<Integer> iterationNumbers = new HashSet<>();
            for (RequestResult result : results) {
                iterationNumbers.add(result.getIterationNumber());
            }

            // Verify no duplicates (set size should equal list size)
            assertThat(iterationNumbers).hasSize(50);

            // Verify iteration numbers cover the range 1 to 50
            for (int i = 1; i <= 50; i++) {
                assertThat(iterationNumbers).contains(i);
            }
        } finally {
            wireMockServer.stop();
        }
    }
}
