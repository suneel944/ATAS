package com.atas.shared.testing;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class that manages Playwright API setup/teardown for tests. Extend this in API test classes
 * to get a ready {@link #request}.
 */
public abstract class ApiTestHooks {

  protected Playwright playwright;
  protected APIRequestContext request;

  @BeforeEach
  void apiSetUp() {
    playwright = Playwright.create();
    request =
        playwright
            .request()
            .newContext(new APIRequest.NewContextOptions().setBaseURL(resolveApiBaseUrl()));
  }

  @AfterEach
  void apiTearDown() {
    if (request != null) {
      request.dispose();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  protected String resolveApiBaseUrl() {
    String fromEnv = System.getenv("API_BASE_URL");
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }
    String fromProp = System.getProperty("API_BASE_URL");
    if (fromProp != null && !fromProp.isBlank()) {
      return fromProp;
    }
    return "https://automationexercise.com";
  }
}
