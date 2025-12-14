package com.atas.framework.execution;

import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Validates test execution inputs to prevent command injection and ensure data integrity. */
@Component
@Slf4j
public class TestInputValidator {

  // Allow alphanumeric, dots, underscores, hyphens for class names
  // Examples: "LoginTest", "com.atas.LoginTest", "Login-Ui-Test"
  private static final Pattern VALID_TEST_CLASS = Pattern.compile("^[a-zA-Z0-9._-]+$");

  // Allow alphanumeric and underscores for method names
  // Example: "test_login_success", "loginShouldSucceed"
  private static final Pattern VALID_TEST_METHOD = Pattern.compile("^[a-zA-Z0-9_]+$");

  // Allow alphanumeric, underscores, hyphens for tags
  // Example: "smoke", "regression-test", "ui_test"
  private static final Pattern VALID_TAG = Pattern.compile("^[a-zA-Z0-9_-]+$");

  // Allow alphanumeric, dots, underscores, hyphens, wildcards for grep patterns
  // Example: "*Login*", "com.atas.*", "Test*"
  private static final Pattern VALID_GREP_PATTERN = Pattern.compile("^[a-zA-Z0-9._*?-]+$");

  // Maximum lengths to prevent DoS
  private static final int MAX_CLASS_NAME_LENGTH = 500;
  private static final int MAX_METHOD_NAME_LENGTH = 200;
  private static final int MAX_TAG_LENGTH = 100;
  private static final int MAX_GREP_PATTERN_LENGTH = 500;
  private static final int MAX_TAGS_COUNT = 50;

  /**
   * Validates a test class name.
   *
   * @param testClass the test class name to validate
   * @throws IllegalArgumentException if the name is invalid
   */
  public void validateTestClass(String testClass) {
    if (testClass == null || testClass.trim().isEmpty()) {
      throw new IllegalArgumentException("Test class name cannot be null or empty");
    }

    String trimmed = testClass.trim();

    if (trimmed.length() > MAX_CLASS_NAME_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Test class name exceeds maximum length of %d characters", MAX_CLASS_NAME_LENGTH));
    }

    if (!VALID_TEST_CLASS.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid test class name: '%s'. Only alphanumeric characters, dots, underscores, and hyphens are allowed.",
              trimmed));
    }
  }

  /**
   * Validates a test method name.
   *
   * @param testMethod the test method name to validate
   * @throws IllegalArgumentException if the name is invalid
   */
  public void validateTestMethod(String testMethod) {
    if (testMethod == null || testMethod.trim().isEmpty()) {
      throw new IllegalArgumentException("Test method name cannot be null or empty");
    }

    String trimmed = testMethod.trim();

    if (trimmed.length() > MAX_METHOD_NAME_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Test method name exceeds maximum length of %d characters", MAX_METHOD_NAME_LENGTH));
    }

    if (!VALID_TEST_METHOD.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid test method name: '%s'. Only alphanumeric characters and underscores are allowed.",
              trimmed));
    }
  }

  /**
   * Validates a list of test tags.
   *
   * @param tags the list of tags to validate
   * @throws IllegalArgumentException if any tag is invalid
   */
  public void validateTags(List<String> tags) {
    if (tags == null) {
      return; // Null is allowed (optional parameter)
    }

    if (tags.isEmpty()) {
      throw new IllegalArgumentException("Tags list cannot be empty");
    }

    if (tags.size() > MAX_TAGS_COUNT) {
      throw new IllegalArgumentException(
          String.format("Too many tags: %d. Maximum allowed is %d", tags.size(), MAX_TAGS_COUNT));
    }

    for (String tag : tags) {
      if (tag == null || tag.trim().isEmpty()) {
        throw new IllegalArgumentException("Tag cannot be null or empty");
      }

      String trimmed = tag.trim();

      if (trimmed.length() > MAX_TAG_LENGTH) {
        throw new IllegalArgumentException(
            String.format(
                "Tag '%s' exceeds maximum length of %d characters", trimmed, MAX_TAG_LENGTH));
      }

      if (!VALID_TAG.matcher(trimmed).matches()) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid tag: '%s'. Only alphanumeric characters, underscores, and hyphens are allowed.",
                trimmed));
      }
    }
  }

  /**
   * Validates a grep pattern.
   *
   * @param grepPattern the grep pattern to validate
   * @throws IllegalArgumentException if the pattern is invalid
   */
  public void validateGrepPattern(String grepPattern) {
    if (grepPattern == null || grepPattern.trim().isEmpty()) {
      throw new IllegalArgumentException("Grep pattern cannot be null or empty");
    }

    String trimmed = grepPattern.trim();

    if (trimmed.length() > MAX_GREP_PATTERN_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Grep pattern exceeds maximum length of %d characters", MAX_GREP_PATTERN_LENGTH));
    }

    if (!VALID_GREP_PATTERN.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid grep pattern: '%s'. Only alphanumeric characters, dots, underscores, hyphens, wildcards (*, ?) are allowed.",
              trimmed));
    }
  }

  /**
   * Validates a test suite name.
   *
   * @param suiteName the suite name to validate
   * @throws IllegalArgumentException if the name is invalid
   */
  public void validateSuiteName(String suiteName) {
    if (suiteName == null || suiteName.trim().isEmpty()) {
      throw new IllegalArgumentException("Test suite name cannot be null or empty");
    }

    String trimmed = suiteName.trim();

    if (trimmed.length() > MAX_CLASS_NAME_LENGTH) {
      throw new IllegalArgumentException(
          String.format(
              "Test suite name exceeds maximum length of %d characters", MAX_CLASS_NAME_LENGTH));
    }

    if (!VALID_TEST_CLASS.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid test suite name: '%s'. Only alphanumeric characters, dots, underscores, and hyphens are allowed.",
              trimmed));
    }
  }
}
