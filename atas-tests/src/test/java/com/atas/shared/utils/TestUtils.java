package com.atas.shared.utils;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared utility class for test operations. Contains common methods used across different feature
 * tests.
 */
@Slf4j
public class TestUtils {

  /** Creates a test execution with the given parameters. */
  public static TestExecution createTestExecution(String suiteName, String environment) {
    return TestExecution.builder()
        .executionId(UUID.randomUUID().toString())
        .suiteName(suiteName)
        .status(TestStatus.RUNNING)
        .startTime(LocalDateTime.now())
        .environment(environment)
        .build();
  }

  /** Creates a test result with the given parameters. */
  public static TestResult createTestResult(
      TestExecution execution, String testId, String testName, TestStatus status) {
    return TestResult.builder()
        .execution(execution)
        .testId(testId)
        .testName(testName)
        .status(status)
        .startTime(LocalDateTime.now())
        .endTime(LocalDateTime.now())
        .build();
  }

  /** Generates a unique test ID with a prefix. */
  public static String generateTestId(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  /**
   * Waits for a specified number of milliseconds. Use sparingly and prefer explicit waits in UI
   * tests.
   */
  public static void waitFor(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Sleep interrupted", e);
    }
  }

  /** Logs test execution information. */
  public static void logTestExecution(TestExecution execution) {
    log.info(
        "Test Execution: ID={}, Suite={}, Status={}, Environment={}",
        execution.getExecutionId(),
        execution.getSuiteName(),
        execution.getStatus(),
        execution.getEnvironment());
  }

  /** Logs test result information. */
  public static void logTestResult(TestResult result) {
    log.info(
        "Test Result: ID={}, Name={}, Status={}",
        result.getTestId(),
        result.getTestName(),
        result.getStatus());
  }
}
