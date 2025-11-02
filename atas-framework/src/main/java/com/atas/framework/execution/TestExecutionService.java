package com.atas.framework.execution;

import com.atas.framework.execution.dto.TestDiscoveryResponse;
import com.atas.framework.execution.dto.TestExecutionRequest;
import com.atas.framework.execution.dto.TestExecutionResponse;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service for executing tests with various filtering options */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestExecutionService {

  private final TestExecutionRepository executionRepository;
  private final TestDiscoveryService testDiscoveryService;
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  @Value("${atas.mvnw.path:/app/mvnw}")
  private String mvnwPath;

  /** Execute tests based on the provided request */
  public TestExecutionResponse executeTests(TestExecutionRequest request) {
    log.info("Starting test execution with type: {}", request.getType());

    String executionId = UUID.randomUUID().toString();
    LocalDateTime startTime = LocalDateTime.now();

    TestExecution execution =
        TestExecution.builder()
            .executionId(executionId)
            .suiteName(generateSuiteName(request))
            .status(TestStatus.RUNNING)
            .startTime(startTime)
            .environment(request.getEnvironment())
            .build();

    executionRepository.save(execution);

    // Build response
    TestExecutionResponse response =
        TestExecutionResponse.builder()
            .executionId(executionId)
            .status("RUNNING")
            .executionType(request.getType().name())
            .description(generateDescription(request))
            .startTime(startTime)
            .timeoutMinutes(request.getTimeoutMinutes())
            .testsToExecute(discoverTestsToExecute(request))
            .environment(request.getEnvironment())
            .browserType(request.getBrowserType())
            .recordVideo(request.isRecordVideo())
            .captureScreenshots(request.isCaptureScreenshots())
            .monitoringUrl("/api/v1/test-execution/status?executionId=" + executionId)
            .liveUpdatesUrl("/api/v1/test-execution/live?executionId=" + executionId)
            .resultsUrl("/api/v1/test-execution/results/" + executionId)
            .build();

    // Execute tests asynchronously
    CompletableFuture.runAsync(
        () -> {
          try {
            executeTestsAsync(executionId, request);
          } catch (Exception e) {
            log.error("Error executing tests for executionId: {}", executionId, e);
            updateExecutionStatus(executionId, TestStatus.ERROR);
          }
        },
        executorService);

    return response;
  }

  /** Discover available tests, suites, and tags */
  public TestDiscoveryResponse discoverTests() {
    return testDiscoveryService.discoverTests();
  }

  /** Get available test classes */
  public List<TestDiscoveryResponse.TestClassInfo> getTestClasses() {
    return testDiscoveryService.getTestClasses();
  }

  /** Get available test suites */
  public List<TestDiscoveryResponse.TestSuiteInfo> getTestSuites() {
    return testDiscoveryService.getTestSuites();
  }

  /** Get available tags */
  public List<String> getAvailableTags() {
    return testDiscoveryService.getAvailableTags();
  }

  /** Execute tests asynchronously */
  private void executeTestsAsync(String executionId, TestExecutionRequest request) {
    try {
      log.info("Executing tests for executionId: {}", executionId);

      // Build Maven command based on request type
      List<String> mavenArgs = buildMavenCommand(request);

      // Execute Maven command
      ProcessBuilder processBuilder = new ProcessBuilder(mavenArgs);
      processBuilder.directory(new java.io.File("/app/atas-tests"));
      processBuilder.environment().put("ATAS_EXECUTION_ID", executionId);
      // Set environment for Maven execution
      processBuilder.environment().put("MAVEN_HOME", "/usr/share/maven");
      processBuilder.environment().put("PATH", "/usr/share/maven/bin:" + System.getenv("PATH"));

      Process process = processBuilder.start();

      // Monitor process
      int exitCode = process.waitFor();

      if (exitCode == 0) {
        updateExecutionStatus(executionId, TestStatus.PASSED);
        log.info("Tests completed successfully for executionId: {}", executionId);
      } else {
        updateExecutionStatus(executionId, TestStatus.FAILED);
        log.warn("Tests failed for executionId: {} with exit code: {}", executionId, exitCode);
      }

    } catch (Exception e) {
      log.error("Error executing tests for executionId: {}", executionId, e);
      updateExecutionStatus(executionId, TestStatus.ERROR);
    }
  }

  /** Build Maven command based on request type */
  private List<String> buildMavenCommand(TestExecutionRequest request) {
    List<String> args = new ArrayList<>();
    // Use configured Maven wrapper path (from Spring properties or environment variable)
    args.add(mvnwPath);
    args.add("test");
    args.add("-pl");
    args.add("atas-tests");

    switch (request.getType()) {
      case INDIVIDUAL_TEST:
        if (request.getTestClass() != null) {
          args.add("-Dtest=" + request.getTestClass());
          if (request.getTestMethod() != null) {
            args.set(
                args.size() - 1,
                "-Dtest=" + request.getTestClass() + "#" + request.getTestMethod());
          }
        }
        break;

      case TAGS:
        if (request.getTags() != null && !request.getTags().isEmpty()) {
          String tagExpression = String.join(" or ", request.getTags());
          args.add("-Djunit.jupiter.includeTags=" + tagExpression);
        }
        break;

      case GREP:
        if (request.getGrepPattern() != null) {
          args.add("-Dtest=" + request.getGrepPattern());
        }
        break;

      case SUITE:
        if (request.getSuiteName() != null) {
          args.add("-Dtest=" + request.getSuiteName() + "TestSuite");
        }
        break;
    }

    // Add additional parameters
    if (request.getParameters() != null) {
      request.getParameters().forEach((key, value) -> args.add("-D" + key + "=" + value));
    }

    return args;
  }

  /** Discover tests that will be executed based on request */
  private List<String> discoverTestsToExecute(TestExecutionRequest request) {
    try {
      return testDiscoveryService.discoverTestsToExecute(request);
    } catch (Exception e) {
      log.error("Could not discover tests for request: {}", request, e);
      return Collections.emptyList();
    }
  }

  /** Generate suite name based on request */
  private String generateSuiteName(TestExecutionRequest request) {
    switch (request.getType()) {
      case INDIVIDUAL_TEST:
        return request.getTestClass() != null ? request.getTestClass() : "individual-test";
      case TAGS:
        return "tagged-tests-" + String.join("-", request.getTags());
      case GREP:
        return "grep-" + request.getGrepPattern().replaceAll("[^a-zA-Z0-9]", "-");
      case SUITE:
        return request.getSuiteName() != null ? request.getSuiteName() : "test-suite";
      default:
        return "custom-execution";
    }
  }

  /** Generate description based on request */
  private String generateDescription(TestExecutionRequest request) {
    switch (request.getType()) {
      case INDIVIDUAL_TEST:
        return String.format(
            "Individual test: %s%s",
            request.getTestClass(),
            request.getTestMethod() != null ? "." + request.getTestMethod() : "");
      case TAGS:
        return String.format("Tests with tags: %s", String.join(", ", request.getTags()));
      case GREP:
        return String.format("Tests matching pattern: %s", request.getGrepPattern());
      case SUITE:
        return String.format("Test suite: %s", request.getSuiteName());
      default:
        return "Custom test execution";
    }
  }

  /** Update execution status */
  private void updateExecutionStatus(String executionId, TestStatus status) {
    try {
      TestExecution execution =
          executionRepository
              .findByExecutionId(executionId)
              .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

      execution.setStatus(status);
      if (status == TestStatus.PASSED
          || status == TestStatus.FAILED
          || status == TestStatus.ERROR) {
        execution.setEndTime(LocalDateTime.now());
      }

      executionRepository.save(execution);
      log.info("Updated execution {} status to {}", executionId, status);
    } catch (Exception e) {
      log.error("Error updating execution status for {}: {}", executionId, e.getMessage());
    }
  }
}
