package com.atas.framework.execution;

import com.atas.framework.execution.dto.TestDiscoveryResponse;
import com.atas.framework.execution.dto.TestExecutionRequest;
import com.atas.framework.execution.dto.TestExecutionResponse;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
import com.atas.framework.security.AuditService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** Service for executing tests with various filtering options */
@Service
@Slf4j
public class TestExecutionService {

  private final TestExecutionRepository executionRepository;
  private final TestResultRepository resultRepository;
  private final TestDiscoveryService testDiscoveryService;
  private final TestInputValidator inputValidator;
  private final AuditService auditService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ExecutorService executorService;
  private final ExecutorService outputCaptureExecutor;

  @Value("${atas.mvnw.path:/app/mvnw}")
  private String mvnwPath;

  public TestExecutionService(
      TestExecutionRepository executionRepository,
      TestResultRepository resultRepository,
      TestDiscoveryService testDiscoveryService,
      TestInputValidator inputValidator,
      AuditService auditService,
      RedisTemplate<String, Object> redisTemplate,
      @Qualifier("testExecutionExecutor") ExecutorService executorService,
      @Qualifier("outputCaptureExecutor") ExecutorService outputCaptureExecutor) {
    this.executionRepository = executionRepository;
    this.resultRepository = resultRepository;
    this.testDiscoveryService = testDiscoveryService;
    this.inputValidator = inputValidator;
    this.auditService = auditService;
    this.redisTemplate = redisTemplate;
    this.executorService = executorService;
    this.outputCaptureExecutor = outputCaptureExecutor;
  }

  /** Execute tests based on the provided request */
  public TestExecutionResponse executeTests(TestExecutionRequest request) {
    log.info("Starting test execution with type: {}", request.getType());

    String executionId = UUID.randomUUID().toString();
    LocalDateTime startTime = LocalDateTime.now(ZoneOffset.UTC);

    TestExecution execution =
        TestExecution.builder()
            .executionId(executionId)
            .suiteName(generateSuiteName(request))
            .status(TestStatus.RUNNING)
            .startTime(startTime)
            .environment(request.getEnvironment())
            .build();

    executionRepository.save(execution);

    // Log test execution asynchronously to avoid blocking HTTP response
    // Use fire-and-forget approach to prevent blocking
    try {
      CompletableFuture.runAsync(
          () -> {
            try {
              auditService.logTestExecution(executionId, "TEST_EXECUTE");
            } catch (Exception e) {
              log.warn("Failed to log test execution audit: {}", e.getMessage());
            }
          },
          executorService);
    } catch (Exception e) {
      log.warn("Failed to schedule audit logging: {}", e.getMessage());
      // Continue even if audit logging fails
    }

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

  /** Execute tests asynchronously with timeout enforcement */
  private void executeTestsAsync(String executionId, TestExecutionRequest request) {
    Process process = null;
    Future<Integer> processFuture = null;
    ExecutorService timeoutExecutor = null;

    try {
      log.info("Executing tests for executionId: {}", executionId);

      // Build Maven command based on request type
      List<String> mavenArgs = buildMavenCommand(request);
      log.info("Maven command for executionId {}: {}", executionId, String.join(" ", mavenArgs));

      // Execute Maven command
      ProcessBuilder processBuilder = new ProcessBuilder(mavenArgs);
      processBuilder.directory(new java.io.File(mvnwPath).getParentFile());

      // Inherit all environment variables from the parent process
      // This ensures ConfigMap and Secrets are available to test execution
      processBuilder.environment().putAll(System.getenv());

      // Override/add specific variables
      processBuilder.environment().put("ATAS_EXECUTION_ID", executionId);
      processBuilder.environment().put("ATAS_SUITE_NAME", generateSuiteName(request));
      // Note: MAVEN_HOME and PATH are not set here because mvnw (Maven wrapper) is self-contained
      // and will download/use Maven automatically. Setting incorrect paths can cause failures.

      // Redirect error stream to output stream for unified logging
      processBuilder.redirectErrorStream(true);

      log.info("Starting Maven process for executionId: {}", executionId);
      try {
        process = processBuilder.start();
        log.info("Maven process started for executionId: {}, PID: {}", executionId, process.pid());
      } catch (IOException e) {
        log.error(
            "Failed to start Maven process for executionId: {}. Command: {}. Error: {}",
            executionId,
            String.join(" ", mavenArgs),
            e.getMessage(),
            e);
        updateExecutionStatus(executionId, TestStatus.ERROR);
        return;
      }

      // Capture process output asynchronously
      captureProcessOutput(process, executionId);

      // Create final reference for lambda
      final Process finalProcess = process;

      // Execute with timeout
      timeoutExecutor =
          Executors.newSingleThreadExecutor(
              r -> {
                Thread t = new Thread(r, "test-timeout-" + executionId);
                t.setDaemon(true);
                return t;
              });

      processFuture =
          timeoutExecutor.submit(
              () -> {
                try {
                  return finalProcess.waitFor();
                } catch (Exception e) {
                  log.error("Error waiting for process: {}", executionId, e);
                  return -1;
                }
              });

      int timeoutMinutes = request.getTimeoutMinutes();
      log.info(
          "Waiting for test execution {} with timeout of {} minutes", executionId, timeoutMinutes);

      try {
        int exitCode = processFuture.get(timeoutMinutes, TimeUnit.MINUTES);

        // Determine final status based on actual test results in database, not Maven exit code
        // This ensures we correctly report failures even if Maven returns exit code 0
        // or if TestExecutionListener hasn't finished updating status yet
        TestStatus finalStatus = determineFinalStatusFromTestResults(executionId);

        if (finalStatus != null) {
          updateExecutionStatus(executionId, finalStatus);
          log.info(
              "Finalized execution status for {}: {} (Maven exit code: {})",
              executionId,
              finalStatus,
              exitCode);
        } else {
          // Fallback to exit code if no test results found (shouldn't happen normally)
          log.warn(
              "No test results found for executionId: {}, falling back to Maven exit code: {}",
              executionId,
              exitCode);
          if (exitCode == 0) {
            updateExecutionStatus(executionId, TestStatus.PASSED);
          } else {
            updateExecutionStatus(executionId, TestStatus.FAILED);
          }
        }
      } catch (TimeoutException e) {
        log.warn("Test execution timeout after {} minutes: {}", timeoutMinutes, executionId);

        // Forcefully terminate the process
        if (process != null && process.isAlive()) {
          log.info("Terminating process for executionId: {}", executionId);
          process.destroyForcibly();

          // Wait a bit for graceful shutdown, then force
          try {
            boolean terminated = process.waitFor(5, TimeUnit.SECONDS);
            if (!terminated) {
              log.warn("Process did not terminate gracefully, forcing kill");
            }
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for process termination", ie);
          }
        }

        updateExecutionStatus(executionId, TestStatus.TIMEOUT);
        cleanupResources(executionId);
      }

    } catch (Exception e) {
      log.error("Error executing tests for executionId: {}", executionId, e);

      // Ensure process is terminated on error
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
      }

      updateExecutionStatus(executionId, TestStatus.ERROR);
    } finally {
      // Cleanup
      if (processFuture != null && !processFuture.isDone()) {
        processFuture.cancel(true);
      }
      if (timeoutExecutor != null) {
        timeoutExecutor.shutdown();
        try {
          if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            timeoutExecutor.shutdownNow();
          }
        } catch (InterruptedException e) {
          timeoutExecutor.shutdownNow();
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Capture process output asynchronously using a separate executor. This prevents output capture
   * from consuming threads from the main test execution pool, allowing more tests to run
   * concurrently.
   */
  private void captureProcessOutput(Process process, String executionId) {
    // Capture stdout using dedicated output capture executor
    CompletableFuture.runAsync(
        () -> {
          try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
              output.append(line).append("\n");
              // Log important lines at INFO level for visibility
              if (line.contains("ERROR")
                  || line.contains("FAILURE")
                  || line.contains("Downloading")
                  || line.contains("BUILD")
                  || line.contains("Tests run:")) {
                log.info("[{}] {}", executionId, line);
              }
            }
            // Store output in database
            storeProcessOutput(executionId, "stdout", output.toString());
          } catch (IOException e) {
            log.error("Error reading process stdout for executionId: {}", executionId, e);
          }
        },
        outputCaptureExecutor);

    // Capture stderr (though redirectErrorStream is true, keep this for safety)
    CompletableFuture.runAsync(
        () -> {
          try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
              output.append(line).append("\n");
              // Log stderr at WARN level
              log.warn("[{}] {}", executionId, line);
            }
            storeProcessOutput(executionId, "stderr", output.toString());
          } catch (IOException e) {
            log.error("Error reading process stderr for executionId: {}", executionId, e);
          }
        },
        outputCaptureExecutor);
  }

  /** Store process output in database */
  private void storeProcessOutput(String executionId, String type, String output) {
    try {
      TestExecution execution = executionRepository.findByExecutionId(executionId).orElse(null);

      if (execution != null) {
        if ("stdout".equals(type)) {
          execution.setStdoutOutput(output);
        } else if ("stderr".equals(type)) {
          execution.setStderrOutput(output);
        }
        execution.setOutputComplete(true);
        executionRepository.save(execution);
      }
    } catch (Exception e) {
      log.error("Error storing process output for executionId: {}", executionId, e);
    }
  }

  /** Cleanup resources after timeout or failure */
  private void cleanupResources(String executionId) {
    // Cleanup any temporary files, connections, etc.
    log.info("Cleaning up resources for executionId: {}", executionId);
    // Add specific cleanup logic here if needed
  }

  /** Build Maven command based on request type */
  private List<String> buildMavenCommand(TestExecutionRequest request) {
    List<String> args = new ArrayList<>();
    // Execute mvnw via /bin/sh explicitly for Alpine Linux compatibility
    // Alpine Linux may not properly execute shebang scripts when running as non-root user
    args.add("/bin/sh");
    args.add(mvnwPath);
    args.add("test");
    args.add("-pl");
    args.add("atas-tests");

    switch (request.getType()) {
      case INDIVIDUAL_TEST:
        if (request.getTestClass() != null) {
          inputValidator.validateTestClass(request.getTestClass());

          String testArg = request.getTestClass();
          if (request.getTestMethod() != null) {
            inputValidator.validateTestMethod(request.getTestMethod());
            // Use # separator for class#method format
            testArg = request.getTestClass() + "#" + request.getTestMethod();
          }
          args.add("-Dtest=" + testArg);
        }
        break;

      case TAGS:
        if (request.getTags() != null && !request.getTags().isEmpty()) {
          inputValidator.validateTags(request.getTags());
          // Join tags with | (pipe) for JUnit 5 tag expression OR logic
          // JUnit 5 syntax: tag1|tag2 (OR), tag1&tag2 (AND)
          String tagExpression = String.join("|", request.getTags());
          args.add("-Djunit.jupiter.includeTags=" + tagExpression);
        }
        break;

      case GREP:
        if (request.getGrepPattern() != null) {
          inputValidator.validateGrepPattern(request.getGrepPattern());
          args.add("-Dtest=" + request.getGrepPattern());
        }
        break;

      case SUITE:
        if (request.getSuiteName() != null) {
          inputValidator.validateSuiteName(request.getSuiteName());
          args.add("-Dtest=" + request.getSuiteName() + "TestSuite");
        }
        break;
    }

    // Add additional parameters (validate these too)
    if (request.getParameters() != null) {
      for (Map.Entry<String, String> param : request.getParameters().entrySet()) {
        String key = param.getKey();
        String value = param.getValue();

        // Validate parameter key and value
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
          log.warn("Skipping invalid parameter: {}={}", key, value);
          continue;
        }

        // Basic validation for parameter values to prevent command injection
        if (key.contains(" ")
            || value.contains(";")
            || value.contains("&")
            || value.contains("|")
            || value.contains("`")) {
          log.warn("Skipping potentially unsafe parameter: {}={}", key, value);
          continue;
        }

        args.add("-D" + key.trim() + "=" + value.trim());
      }
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

  /**
   * Determine the final execution status based on actual test results in the database. This is more
   * reliable than relying on Maven exit code, as it reflects the actual test outcomes recorded by
   * TestExecutionListener.
   *
   * @param executionId the execution ID to check
   * @return the final status (FAILED if any tests failed, PASSED if all passed, null if no results
   *     found)
   */
  private TestStatus determineFinalStatusFromTestResults(String executionId) {
    try {
      // Wait a short time for TestExecutionListener to finish recording results
      // This handles the race condition where Maven process completes before afterAll() runs
      Thread.sleep(500);

      Optional<TestExecution> executionOpt =
          executionRepository.findByExecutionIdWithResults(executionId);
      if (executionOpt.isEmpty()) {
        log.warn("Execution not found: {}", executionId);
        return null;
      }

      TestExecution execution = executionOpt.get();

      // Only update if status is still RUNNING (avoid overwriting status set by
      // TestExecutionListener)
      if (execution.getStatus() != TestStatus.RUNNING) {
        return null; // Don't override already-finalized status
      }

      List<TestResult> results = execution.getResults();

      if (results.isEmpty()) {
        log.warn("No test results found for executionId: {}", executionId);
        return null; // Fallback to exit code
      }

      // Check if any tests failed or errored
      boolean hasFailures =
          results.stream()
              .anyMatch(
                  r -> r.getStatus() == TestStatus.FAILED || r.getStatus() == TestStatus.ERROR);

      if (hasFailures) {
        long failedCount =
            results.stream()
                .filter(
                    r -> r.getStatus() == TestStatus.FAILED || r.getStatus() == TestStatus.ERROR)
                .count();
        log.info(
            "Execution {} has {} failed/errored tests, setting status to FAILED",
            executionId,
            failedCount);
        return TestStatus.FAILED;
      }

      // All tests passed or were skipped
      // CRITICAL FIX: Validate that we actually have test results before marking as PASSED
      // If we have results but they're all passed/skipped, that's valid
      // But if we have no results at all, we should be more cautious
      if (results.isEmpty()) {
        log.warn(
            "Execution {} has no test results but status determination was called. "
                + "This may indicate a test execution issue.",
            executionId);
        return null; // Let fallback logic handle this
      }

      log.info(
          "Execution {} has {} tests, all passed/skipped, setting status to PASSED",
          executionId,
          results.size());
      return TestStatus.PASSED;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while determining final status for executionId: {}", executionId);
      return null;
    } catch (Exception e) {
      log.error(
          "Error determining final status from test results for executionId: {}: {}",
          executionId,
          e.getMessage(),
          e);
      return null; // Fallback to exit code on error
    }
  }

  /** Update execution status */
  @CacheEvict(
      value = {"dashboard-overview", "dashboard-recent", "execution-status", "dashboard-trends"},
      allEntries = true)
  protected void updateExecutionStatus(String executionId, TestStatus status) {
    try {
      TestExecution execution =
          executionRepository
              .findByExecutionId(executionId)
              .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

      // Don't overwrite FAILED/ERROR status with PASSED (failures take precedence)
      // But allow overwriting PASSED with FAILED if we discover failures
      if (execution.getStatus() != TestStatus.RUNNING
          && execution.getStatus() != null
          && execution.getStatus() != status) {
        // Allow upgrading from PASSED to FAILED/ERROR (failures take precedence)
        boolean isUpgradingToFailure =
            (execution.getStatus() == TestStatus.PASSED
                    || execution.getStatus() == TestStatus.SKIPPED)
                && (status == TestStatus.FAILED || status == TestStatus.ERROR);

        if (!isUpgradingToFailure) {
          return;
        } else {
          log.info(
              "Upgrading execution {} status from {} to {} (failures take precedence)",
              executionId,
              execution.getStatus(),
              status);
        }
      }

      execution.setStatus(status);
      if (status == TestStatus.PASSED
          || status == TestStatus.FAILED
          || status == TestStatus.ERROR
          || status == TestStatus.TIMEOUT) {
        execution.setEndTime(LocalDateTime.now(ZoneOffset.UTC));
      }

      executionRepository.save(execution);
      log.info("Updated execution {} status to {}", executionId, status);

      // CRITICAL FIX: If execution is marked as FAILED/ERROR/TIMEOUT, sync test result statuses
      // This ensures data consistency - if execution failed, test results should reflect that
      if (status == TestStatus.FAILED
          || status == TestStatus.ERROR
          || status == TestStatus.TIMEOUT) {
        syncTestResultStatuses(executionId, status);
      }

      // Publish execution update to Redis for SSE scaling
      publishExecutionUpdate(executionId, status);
    } catch (Exception e) {
      log.error("Error updating execution status for {}: {}", executionId, e.getMessage());
    }
  }

  /**
   * Sync test result statuses with execution status when execution fails. If execution is
   * FAILED/ERROR/TIMEOUT but test results show PASSED/SKIPPED, update test results to match
   * execution status for data consistency.
   */
  private void syncTestResultStatuses(String executionId, TestStatus executionStatus) {
    try {
      Optional<TestExecution> executionOpt =
          executionRepository.findByExecutionIdWithResults(executionId);
      if (executionOpt.isEmpty()) {
        return;
      }

      TestExecution execution = executionOpt.get();
      List<TestResult> results = execution.getResults();

      if (results.isEmpty()) {
        return;
      }

      int updatedCount = 0;
      for (TestResult result : results) {
        TestStatus resultStatus = result.getStatus();
        // Only update PASSED/SKIPPED results - don't overwrite existing FAILED/ERROR
        if ((resultStatus == TestStatus.PASSED || resultStatus == TestStatus.SKIPPED)
            && (executionStatus == TestStatus.FAILED
                || executionStatus == TestStatus.ERROR
                || executionStatus == TestStatus.TIMEOUT)) {
          log.info(
              "Syncing test result {} status from {} to {} to match execution status",
              result.getId(),
              resultStatus,
              executionStatus);
          result.setStatus(executionStatus);
          resultRepository.save(result);
          updatedCount++;
        }
      }

      if (updatedCount > 0) {
        log.info(
            "Synced {} test result statuses for execution {} to match execution status {}",
            updatedCount,
            executionId,
            executionStatus);
      }
    } catch (Exception e) {
      log.error(
          "Error syncing test result statuses for execution {}: {}",
          executionId,
          e.getMessage(),
          e);
      // Don't fail execution status update if sync fails
    }
  }

  /** Publish execution update to Redis for SSE scaling */
  private void publishExecutionUpdate(String executionId, TestStatus status) {
    try {
      Map<String, Object> update = new HashMap<>();
      update.put("executionId", executionId);
      update.put("status", status.name());
      update.put("timestamp", LocalDateTime.now(ZoneOffset.UTC).toString());

      redisTemplate.convertAndSend("atas:execution:updates", update);
    } catch (Exception e) {
      log.warn("Failed to publish execution update to Redis: {}", e.getMessage());
      // Don't fail the status update if Redis publish fails
    }
  }
}
