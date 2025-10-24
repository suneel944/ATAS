package com.atas.features.monitoring.api;

import com.atas.framework.AtasFrameworkApplication;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API tests for the monitoring endpoints.  These tests run against a
 * random port Spring Boot instance and use RestTemplate to call
 * REST endpoints.  They verify that the status endpoint aggregates
 * results correctly and returns the expected fields.
 */
@SpringBootTest(classes = AtasFrameworkApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
@Slf4j
class TestMonitoringApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestExecutionRepository executionRepository;

    @Autowired
    private TestResultRepository resultRepository;

    private TestExecution execution;

    @BeforeEach
    void setup() {
        // Clean up existing data for isolation
        resultRepository.deleteAll();
        executionRepository.deleteAll();
        // Create an execution with 2 passed and 1 failed tests
        execution = TestExecution.builder()
                .executionId(UUID.randomUUID().toString())
                .suiteName("api-test-suite")
                .status(TestStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .environment("test")
                .build();
        execution = executionRepository.save(execution);
        TestResult r1 = TestResult.builder()
                .execution(execution)
                .testId("test1")
                .testName("Test 1")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
        TestResult r2 = TestResult.builder()
                .execution(execution)
                .testId("test2")
                .testName("Test 2")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
        TestResult r3 = TestResult.builder()
                .execution(execution)
                .testId("test3")
                .testName("Test 3")
                .status(TestStatus.FAILED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
        execution.getResults().add(r1);
        execution.getResults().add(r2);
        execution.getResults().add(r3);
        resultRepository.saveAll(execution.getResults());
    }

    @Test
    void statusEndpointReturnsAggregatedCounts() {
        RestTemplate restTemplate = new RestTemplate();
        String url = String.format("http://localhost:%d/api/v1/test-execution/status?executionId=%s", port, execution.getExecutionId());
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        String body = response.getBody();
        assertThat(body).contains("\"total\":3");
        assertThat(body).contains("\"passed\":2");
        assertThat(body).contains("\"failed\":1");
    }
}