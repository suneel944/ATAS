package com.atas.shared.api;

import com.atas.shared.auth.InternalApiTokenHelper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FrameworkApiRequestContextHelper {

  private FrameworkApiRequestContextHelper() {}

  public static ApiRequestContextResult createApiRequestContext(
      String frameworkBaseUrl, String baseUrl) {
    Playwright tempPlaywright = Playwright.create();
    APIRequestContext tempRequest = tempPlaywright.request().newContext();

    try {
      var internalApiToken =
          InternalApiTokenHelper.getInternalApiToken(tempRequest, frameworkBaseUrl);
      var endpoint = "/api/v1/internal/playwright/api-sessions";

      var api = new FluentApiRequest(tempRequest, frameworkBaseUrl);
      var response =
          api.endpoint(endpoint)
              .withHeader("Authorization", "Bearer " + internalApiToken)
              .withBody(Map.of("baseUrl", baseUrl))
              .post();

      if (response.getStatus() != 200) {
        var errorMessage =
            "Failed to create API session. Status: %d, Response: %s, Endpoint: %s"
                .formatted(response.getStatus(), response.asString(), frameworkBaseUrl + endpoint);
        log.error(errorMessage);
        throw new IllegalStateException(errorMessage);
      }

      var responseData = response.asMap();
      var sessionId =
          Optional.ofNullable(responseData.get("sessionId"))
              .map(String.class::cast)
              .filter(s -> !s.isBlank())
              .orElseThrow(
                  () -> {
                    var errorMessage =
                        "Session ID not found in response. Response: %s"
                            .formatted(response.asString());
                    log.error(errorMessage);
                    return new IllegalStateException(errorMessage);
                  });

      tempRequest.dispose();
      tempPlaywright.close();

      var playwright = Playwright.create();
      var apiRequestContext =
          playwright
              .request()
              .newContext(
                  new com.microsoft.playwright.APIRequest.NewContextOptions().setBaseURL(baseUrl));

      return new ApiRequestContextResult(apiRequestContext, sessionId, playwright);
    } catch (IllegalStateException e) {
      tempRequest.dispose();
      tempPlaywright.close();
      throw e;
    } catch (Exception e) {
      var errorMessage =
          "Failed to create API session via framework. Endpoint: %s, Error: %s"
              .formatted(
                  frameworkBaseUrl + "/api/v1/internal/playwright/api-sessions", e.getMessage());
      log.error(errorMessage, e);
      tempRequest.dispose();
      tempPlaywright.close();
      throw new IllegalStateException(errorMessage, e);
    }
  }

  public static void closeApiSession(String frameworkBaseUrl, String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      return;
    }

    var tempPlaywright = Playwright.create();
    var tempRequest = tempPlaywright.request().newContext();

    try {
      var internalApiToken =
          InternalApiTokenHelper.getInternalApiToken(tempRequest, frameworkBaseUrl);
      var api = new FluentApiRequest(tempRequest, frameworkBaseUrl);
      api.endpoint("/api/v1/internal/playwright/api-sessions/" + sessionId)
          .withHeader("Authorization", "Bearer " + internalApiToken)
          .delete();
    } catch (Exception e) {
      log.warn("Failed to close API session %s: %s", sessionId, e.getMessage());
    } finally {
      tempRequest.dispose();
      tempPlaywright.close();
    }
  }

  public record ApiRequestContextResult(
      APIRequestContext apiRequestContext, String sessionId, Playwright playwright) {}
}
