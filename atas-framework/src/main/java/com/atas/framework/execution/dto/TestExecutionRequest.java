package com.atas.framework.execution.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for test execution requests with various filtering options */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecutionRequest {

  /** Type of test execution filter */
  @NotNull private ExecutionType type;

  /** Specific test class name (for individual test execution) */
  private String testClass;

  /** Specific test method name (for individual test execution) */
  private String testMethod;

  /** Test tags to filter by (e.g., @Tag("smoke"), @Tag("regression")) */
  private List<String> tags;

  /** Grep pattern to match test names or classes */
  private String grepPattern;

  /** Test suite name (e.g., "authentication-ui", "monitoring-api") */
  private String suiteName;

  /**
   * Environment where tests should run (dev, staging, prod). Defaults to ATAS_TEST_ENVIRONMENT env
   * var, then atas.test.environment property, then "dev". Also respects SPRING_PROFILES_ACTIVE if
   * ATAS_TEST_ENVIRONMENT is not set.
   */
  @Builder.Default private String environment = getDefaultEnvironment();

  /** Additional execution parameters */
  private Map<String, String> parameters;

  /** Whether to enable video recording */
  @Builder.Default private boolean recordVideo = true;

  /** Whether to enable screenshots on failure */
  @Builder.Default private boolean captureScreenshots = true;

  /** Browser type for UI tests (CHROMIUM, FIREFOX, WEBKIT) */
  private String browserType;

  /**
   * Maximum execution timeout in minutes. Defaults to ATAS_TEST_TIMEOUT_MINUTES env var, then
   * atas.test.timeout.minutes property, then 30.
   */
  @Builder.Default private int timeoutMinutes = getDefaultTimeoutMinutes();

  /**
   * Get default environment from environment variable, system property, or Spring profile.
   * Priority: ATAS_TEST_ENVIRONMENT > atas.test.environment > SPRING_PROFILES_ACTIVE > "dev"
   */
  private static String getDefaultEnvironment() {
    String env = System.getenv("ATAS_TEST_ENVIRONMENT");
    if (env != null && !env.isEmpty() && !"null".equalsIgnoreCase(env)) {
      return env;
    }
    env = System.getProperty("atas.test.environment");
    if (env != null && !env.isEmpty() && !"null".equalsIgnoreCase(env)) {
      return env;
    }
    env = System.getenv("SPRING_PROFILES_ACTIVE");
    if (env != null && !env.isEmpty() && !"null".equalsIgnoreCase(env)) {
      return env.split(",")[0].trim(); // Use first profile if multiple
    }
    env = System.getProperty("spring.profiles.active");
    if (env != null && !env.isEmpty() && !"null".equalsIgnoreCase(env)) {
      return env.split(",")[0].trim(); // Use first profile if multiple
    }
    return "dev";
  }

  /**
   * Get default timeout from environment variable or system property. Priority:
   * ATAS_TEST_TIMEOUT_MINUTES > atas.test.timeout.minutes > 30
   */
  private static int getDefaultTimeoutMinutes() {
    String timeoutStr = System.getenv("ATAS_TEST_TIMEOUT_MINUTES");
    if (timeoutStr != null && !timeoutStr.isEmpty() && !"null".equalsIgnoreCase(timeoutStr)) {
      try {
        return Integer.parseInt(timeoutStr);
      } catch (NumberFormatException e) {
        // Fall through to next option
      }
    }
    timeoutStr = System.getProperty("atas.test.timeout.minutes");
    if (timeoutStr != null && !timeoutStr.isEmpty() && !"null".equalsIgnoreCase(timeoutStr)) {
      try {
        return Integer.parseInt(timeoutStr);
      } catch (NumberFormatException e) {
        // Fall through to default
      }
    }
    return 30;
  }

  /** Execution types supported by the API */
  public enum ExecutionType {
    INDIVIDUAL_TEST, // Run a specific test class/method
    TAGS, // Run tests matching specific tags
    GREP, // Run tests matching grep pattern
    SUITE // Run a specific test suite
  }
}
