package com.atas.shared.testing;

/**
 * Standardized test tags for categorizing and filtering tests.
 *
 * <p>Usage examples:
 *
 * <ul>
 *   <li>{@code @Tag(TestTags.UI)} - UI tests
 *   <li>{@code @Tag(TestTags.API)} - API tests
 *   <li>{@code @Tag(TestTags.SMOKE)} - Smoke tests
 *   <li>{@code @Tag(TestTags.REGRESSION)} - Regression tests
 * </ul>
 *
 * <p>To run tests by tag:
 *
 * <ul>
 *   <li>Maven: {@code mvn test -Dgroups=ui}
 *   <li>Maven: {@code mvn test -Dgroups=smoke -DexcludedGroups=slow}
 *   <li>IDE: Configure JUnit run configuration with tags
 * </ul>
 */
public final class TestTags {

  private TestTags() {
    // Utility class - prevent instantiation
  }

  /** Test type tags */
  public static final String UI = "ui";

  public static final String API = "api";
  public static final String DB = "db";
  public static final String INTEGRATION = "integration";

  /** Test suite tags */
  public static final String SMOKE = "smoke";

  public static final String REGRESSION = "regression";
  public static final String SANITY = "sanity";

  /** Test execution tags */
  public static final String FAST = "fast";

  public static final String SLOW = "slow";
  public static final String CRITICAL = "critical";
  public static final String HIGH = "high";
  public static final String MEDIUM = "medium";
  public static final String LOW = "low";

  /** Feature area tags */
  public static final String AUTH = "auth";

  public static final String PRODUCTS = "products";
  public static final String CART = "cart";
  public static final String CHECKOUT = "checkout";
  public static final String PAYMENT = "payment";
  public static final String NAVIGATION = "navigation";
  public static final String CONTACT = "contact";

  /** Environment tags */
  public static final String DEV = "dev";

  public static final String STAGE = "stage";
  public static final String PROD = "prod";

  /** Priority tags */
  public static final String P0 = "p0";

  public static final String P1 = "p1";
  public static final String P2 = "p2";
  public static final String P3 = "p3";
}
