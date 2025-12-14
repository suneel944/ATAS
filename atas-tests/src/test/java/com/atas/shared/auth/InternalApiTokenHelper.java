package com.atas.shared.auth;

import com.microsoft.playwright.APIRequestContext;
import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.utility.TestDataUtility;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InternalApiTokenHelper {

  private InternalApiTokenHelper() {}

  private static final Properties testConfigProperties =
      TestDataUtility.loadProperties("test-config.properties");

  private static String getClientId() {
    return Optional.of("ATAS_EXECUTION_ID")
        .map(
            key -> {
              try {
                return TestDataUtility.getProperty(key);
              } catch (IllegalStateException ignored) {
                return null;
              }
            })
        .orElseGet(
            () ->
                "test-client-%d-%s"
                    .formatted(
                        System.currentTimeMillis(),
                        java.util.UUID.randomUUID().toString().substring(0, 8)));
  }

  public static String getInternalApiToken(APIRequestContext request, String frameworkBaseUrl) {
    var baseUrl =
        Optional.ofNullable(frameworkBaseUrl)
            .filter(s -> !s.isBlank())
            .orElseGet(
                () -> TestDataUtility.getProperty("ATAS_FRAMEWORK_URL", testConfigProperties));
    var apiKey = TestDataUtility.getProperty("ATAS_INTERNAL_API_KEY");
    var clientId = getClientId();
    var endpoint = "%s/api/v1/internal/auth/token".formatted(baseUrl);

    try {
      var api = new FluentApiRequest(request, baseUrl);
      var response =
          api.endpoint("/api/v1/internal/auth/token")
              .withHeader("X-API-Key", apiKey)
              .withBody(Map.of("clientId", clientId))
              .post();

      if (response.getStatus() != 200) {
        var errorMessage =
            "Failed to get internal API token. Status: %d, Response: %s, Endpoint: %s"
                .formatted(response.getStatus(), response.asString(), endpoint);
        log.error(errorMessage);
        throw new IllegalStateException(errorMessage);
      }

      var tokenResponse = response.asMap();
      var token =
          Optional.ofNullable(tokenResponse.get("token"))
              .map(String.class::cast)
              .filter(s -> !s.isBlank())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Internal API token not found in response. Framework must be available and configured."));

      return token;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      var errorMessage =
          "Failed to get internal API token. Endpoint: %s, Error: %s"
              .formatted(endpoint, e.getMessage());
      log.error(errorMessage, e);
      throw new IllegalStateException(errorMessage, e);
    }
  }
}
