package com.atas.shared.utility;

import java.util.List;
import java.util.Properties;

public class BaseUrlResolver {

  private BaseUrlResolver() {}

  private static final Properties testConfigProperties =
      TestDataUtility.loadProperties("test-config.properties");

  public static String resolveService(String serviceName) {
    var normalizedName = normalizeServiceName(serviceName);
    var propertyKeys =
        List.of(
            normalizedName + "_BASE_URL",
            "API_" + normalizedName + "_BASE_URL",
            "SERVICE_" + normalizedName + "_BASE_URL");
    return resolveByPropertyKeys(propertyKeys, testConfigProperties);
  }

  public static String resolveGateway(String gatewayName) {
    var normalizedName = normalizeServiceName(gatewayName);
    var propertyKeys =
        List.of(
            normalizedName + "_BASE_URL",
            "GATEWAY_" + normalizedName + "_BASE_URL",
            normalizedName + "_GATEWAY_BASE_URL");
    return resolveByPropertyKeys(propertyKeys, testConfigProperties);
  }

  public static String resolveFrameworkBaseUrl() {
    return TestDataUtility.getProperty("ATAS_FRAMEWORK_URL", testConfigProperties);
  }

  private static String normalizeServiceName(String serviceName) {
    return serviceName.toUpperCase().replaceAll("[^A-Z0-9]", "_");
  }

  private static String resolveByPropertyKeys(List<String> propertyKeys, Properties properties) {
    return propertyKeys.stream()
        .map(
            key -> {
              try {
                return TestDataUtility.getProperty(key, properties);
              } catch (IllegalStateException e) {
                return null;
              }
            })
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Required service URL not found. Tried property keys: "
                        + String.join(", ", propertyKeys)));
  }
}
