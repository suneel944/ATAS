package com.atas.features.monitoring.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.atas.features.monitoring.data.MonitoringTestData;
import com.atas.framework.AtasFrameworkApplication;
import com.atas.framework.model.TestExecution;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

/**
 * API tests for test execution status monitoring endpoints. Tests various scenarios for status
 * retrieval and aggregation.
 */
@SpringBootTest(
    classes = AtasFrameworkApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
public class TestExecutionStatusApiTest {

  @LocalServerPort private int port;

  @Autowired private TestExecutionRepository executionRepository;

  @Autowired private TestResultRepository resultRepository;

  private RestTemplate restTemplate;

  @BeforeEach
  void setup() {
    // Clean up existing data for isolation
    resultRepository.deleteAll();
    executionRepository.deleteAll();

    // Initialize RestTemplate
    restTemplate = new RestTemplate();
  }

  @Test
  void statusEndpointReturnsCorrectCountsForMixedResults() {
    // Given a test execution with mixed results
    TestExecution execution = MonitoringTestData.createTestExecutionWithMixedResults();
    execution = executionRepository.save(execution);
    resultRepository.saveAll(execution.getResults());

    // When I call the status endpoint
    String url =
        String.format(
            "http://localhost:%d/api/v1/test-execution/status?executionId=%s",
            port, execution.getExecutionId());
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then the response should contain correct counts
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    String body = response.getBody();
    assertThat(body).contains("\"total\":3");
    assertThat(body).contains("\"passed\":2");
    assertThat(body).contains("\"failed\":1");
    assertThat(body).contains("\"running\":0");
  }

  @Test
  void statusEndpointReturnsZeroCountsForEmptyExecution() {
    // Given an empty test execution
    TestExecution execution = MonitoringTestData.createEmptyTestExecution();
    execution = executionRepository.save(execution);

    // When I call the status endpoint
    String url =
        String.format(
            "http://localhost:%d/api/v1/test-execution/status?executionId=%s",
            port, execution.getExecutionId());
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then the response should contain zero counts
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    String body = response.getBody();
    assertThat(body).contains("\"total\":0");
    assertThat(body).contains("\"passed\":0");
    assertThat(body).contains("\"failed\":0");
    assertThat(body).contains("\"running\":0");
  }

  @Test
  void statusEndpointReturnsAllPassedCounts() {
    // Given a test execution with all passed results
    TestExecution execution = MonitoringTestData.createAllPassedTestExecution();
    execution = executionRepository.save(execution);
    resultRepository.saveAll(execution.getResults());

    // When I call the status endpoint
    String url =
        String.format(
            "http://localhost:%d/api/v1/test-execution/status?executionId=%s",
            port, execution.getExecutionId());
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // Then the response should contain all passed counts
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    String body = response.getBody();
    assertThat(body).contains("\"total\":5");
    assertThat(body).contains("\"passed\":5");
    assertThat(body).contains("\"failed\":0");
    assertThat(body).contains("\"running\":0");
  }

  @Test
  void statusEndpointHandlesInvalidExecutionId() {
    // Given an invalid execution ID
    String invalidExecutionId = "invalid-execution-id";

    // When I call the status endpoint with invalid ID
    String url =
        String.format(
            "http://localhost:%d/api/v1/test-execution/status?executionId=%s",
            port, invalidExecutionId);

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
      // If we get here, the API returned a successful response
      // This might be expected behavior - the API could return empty results for invalid IDs
      log.info("Response status: {}, Body: {}", response.getStatusCode(), response.getBody());
      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      // If the API returns 404 for invalid execution IDs, that's also acceptable
      log.info("Expected 404 response for invalid execution ID: {}", e.getStatusCode());
      assertThat(e.getStatusCode().value()).isEqualTo(404);
    }
  }
}
