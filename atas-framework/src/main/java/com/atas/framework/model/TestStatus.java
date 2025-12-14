package com.atas.framework.model;

/**
 * Enumeration representing the possible statuses for a test or execution. Passed and FAILED
 * indicate final outcomes while RUNNING reflects an ongoing execution and SKIPPED denotes a
 * deliberately skipped test. Additional statuses can be added if necessary, such as ERROR or
 * CANCELLED.
 */
public enum TestStatus {
  PASSED,
  FAILED,
  SKIPPED,
  RUNNING,
  ERROR,
  TIMEOUT;
}
