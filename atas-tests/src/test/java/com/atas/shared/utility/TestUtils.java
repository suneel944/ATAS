package com.atas.shared.utility;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared utility class for test operations. Contains common methods used across different feature
 * tests.
 *
 * <p><strong>Note:</strong> Test execution and result recording is handled automatically by
 * atas-framework via {@link com.atas.framework.execution.TestExecutionListener}. No manual logging
 * of test executions/results is needed.
 */
@Slf4j
public class TestUtils {

  private TestUtils() {}

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

  /**
   * Playwright-style condition-based waiting for async operations.
   *
   * <p>Waits for a condition to return a non-null value, polling at intervals until the condition
   * is met or timeout is reached. This follows Playwright's waiting pattern for API operations.
   *
   * <p>The condition is evaluated immediately on first call, then at each polling interval. Returns
   * the first non-null value returned by the condition, or throws if timeout is reached.
   *
   * @param <T> Type of value returned by the condition
   * @param condition Supplier that returns a value when ready, or null to continue waiting
   * @param timeout Maximum duration to wait
   * @return The value returned by the condition (never null)
   * @throws RuntimeException if timeout is reached before condition is met
   */
  public static <T> T waitForCondition(Supplier<T> condition, Duration timeout) {
    return waitForCondition(condition, timeout, Duration.ofMillis(200));
  }

  /**
   * Playwright-style condition-based waiting for async operations with custom polling interval.
   *
   * @param <T> Type of value returned by the condition
   * @param condition Supplier that returns a value when ready, or null to continue waiting
   * @param timeout Maximum duration to wait
   * @param interval Duration between condition checks
   * @return The value returned by the condition (never null)
   * @throws RuntimeException if timeout is reached before condition is met
   */
  public static <T> T waitForCondition(Supplier<T> condition, Duration timeout, Duration interval) {
    long startTime = System.currentTimeMillis();
    long timeoutMillis = timeout.toMillis();
    long intervalMillis = interval.toMillis();

    // Check condition immediately first
    T result = condition.get();
    if (result != null) {
      return result;
    }

    // Poll at intervals until timeout
    while (System.currentTimeMillis() - startTime < timeoutMillis) {
      try {
        Thread.sleep(intervalMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Wait interrupted", e);
      }

      result = condition.get();
      if (result != null) {
        return result;
      }
    }

    throw new RuntimeException(
        String.format(
            "Condition not met after %dms (polling interval: %dms)",
            timeoutMillis, intervalMillis));
  }

  /**
   * Conditionally adds a value to the map if it's not null.
   *
   * @param map The map to add to
   * @param key The key to add
   * @param value The value to add (only if not null)
   */
  public static void putIfPresent(Map<String, Object> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  /**
   * Conditionally adds a list to the map if it's not null and not empty.
   *
   * @param map The map to add to
   * @param key The key to add
   * @param value The list to add (only if not null and not empty)
   */
  public static void putIfNotEmpty(Map<String, Object> map, String key, List<String> value) {
    if (value != null && !value.isEmpty()) {
      map.put(key, value);
    }
  }

  /**
   * Conditionally adds a value to the map if it's not null and not blank.
   *
   * @param map The map to add to
   * @param key The key to add
   * @param value The value to add (only if not null and not blank)
   */
  public static void putIfNotBlank(Map<String, Object> map, String key, String value) {
    if (value != null && !value.isBlank()) {
      map.put(key, value);
    }
  }
}
