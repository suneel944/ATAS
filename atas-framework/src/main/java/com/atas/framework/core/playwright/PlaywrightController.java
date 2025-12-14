package com.atas.framework.core.playwright;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Page;
import com.atas.framework.core.driver.BrowserType;
import com.atas.framework.core.driver.DriverConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing internal APIs for Playwright browser automation.
 *
 * <p>Provides generic browser automation capabilities that can be used by test layers without
 * managing their own Playwright instances. This allows tests to leverage the framework's
 * centralized browser management.
 */
@RestController
@RequestMapping("/api/v1/internal/playwright")
@RequiredArgsConstructor
@Slf4j
public class PlaywrightController {

  private final PlaywrightService playwrightService;
  private final Map<String, PageSession> activeSessions = new HashMap<>();
  private final Map<String, ApiRequestSession> activeApiSessions = new HashMap<>();

  /**
   * Create a new browser page session.
   *
   * @param request Page creation request with browser type and configuration
   * @return Session ID and page information
   */
  @PostMapping("/sessions")
  public ResponseEntity<SessionResponse> createSession(@RequestBody CreateSessionRequest request) {

    try {
      BrowserType browserType = parseBrowserType(request.getBrowserType());
      DriverConfig config =
          DriverConfig.builder()
              .headless(request.isHeadless())
              .recordVideo(request.isRecordVideo())
              .viewportWidth(request.getViewportWidth() != null ? request.getViewportWidth() : 1280)
              .viewportHeight(
                  request.getViewportHeight() != null ? request.getViewportHeight() : 720)
              .build();

      Page page = playwrightService.createPage(browserType, config);
      String sessionId = UUID.randomUUID().toString();

      PageSession session = new PageSession();
      session.setPage(page);
      session.setSessionId(sessionId);
      session.setCreatedAt(System.currentTimeMillis());
      activeSessions.put(sessionId, session);

      SessionResponse response = new SessionResponse();
      response.setSessionId(sessionId);
      response.setSuccess(true);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to create browser session", e);
      SessionResponse response = new SessionResponse();
      response.setSuccess(false);
      response.setError(e.getMessage());
      return ResponseEntity.status(500).body(response);
    }
  }

  /**
   * Navigate to a URL in the specified session.
   *
   * @param sessionId Session ID
   * @param request Navigation request with URL
   * @return Navigation result
   */
  @PostMapping("/sessions/{sessionId}/navigate")
  public ResponseEntity<OperationResponse> navigate(
      @PathVariable String sessionId, @RequestBody NavigateRequest request) {

    try {
      Page page = getPage(sessionId);
      page.navigate(request.getUrl());

      OperationResponse response = new OperationResponse();
      response.setSuccess(true);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to navigate session {}", sessionId, e);
      return errorResponse(e);
    }
  }

  /**
   * Fill an input field in the specified session.
   *
   * @param sessionId Session ID
   * @param request Fill request with selector and value
   * @return Operation result
   */
  @PostMapping("/sessions/{sessionId}/fill")
  public ResponseEntity<OperationResponse> fill(
      @PathVariable String sessionId, @RequestBody FillRequest request) {

    try {
      Page page = getPage(sessionId);
      page.fill(request.getSelector(), request.getValue());

      OperationResponse response = new OperationResponse();
      response.setSuccess(true);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to fill field in session {}", sessionId, e);
      return errorResponse(e);
    }
  }

  /**
   * Click an element in the specified session.
   *
   * @param sessionId Session ID
   * @param request Click request with selector
   * @return Operation result
   */
  @PostMapping("/sessions/{sessionId}/click")
  public ResponseEntity<OperationResponse> click(
      @PathVariable String sessionId, @RequestBody ClickRequest request) {

    try {
      Page page = getPage(sessionId);
      page.click(request.getSelector());

      OperationResponse response = new OperationResponse();
      response.setSuccess(true);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to click element in session {}", sessionId, e);
      return errorResponse(e);
    }
  }

  /**
   * Wait for a selector to be visible in the specified session.
   *
   * @param sessionId Session ID
   * @param request Wait request with selector and optional timeout
   * @return Operation result
   */
  @PostMapping("/sessions/{sessionId}/wait-for-selector")
  public ResponseEntity<OperationResponse> waitForSelector(
      @PathVariable String sessionId, @RequestBody WaitForSelectorRequest request) {

    try {
      Page page = getPage(sessionId);
      int timeout = request.getTimeout() != null ? request.getTimeout() : 30000;
      page.waitForSelector(
          request.getSelector(), new Page.WaitForSelectorOptions().setTimeout(timeout));

      OperationResponse response = new OperationResponse();
      response.setSuccess(true);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to wait for selector in session {}", sessionId, e);
      return errorResponse(e);
    }
  }

  /**
   * Wait for URL to match a pattern in the specified session.
   *
   * @param sessionId Session ID
   * @param request Wait request with URL pattern and optional timeout
   * @return Operation result with current URL
   */
  @PostMapping("/sessions/{sessionId}/wait-for-url")
  public ResponseEntity<UrlResponse> waitForUrl(
      @PathVariable String sessionId, @RequestBody WaitForUrlRequest request) {

    try {
      Page page = getPage(sessionId);
      int timeout = request.getTimeout() != null ? request.getTimeout() : 30000;
      page.waitForURL(
          url -> url.contains(request.getUrlPattern()) || url.startsWith(request.getUrlPattern()),
          new Page.WaitForURLOptions().setTimeout(timeout));

      UrlResponse response = new UrlResponse();
      response.setSuccess(true);
      response.setCurrentUrl(page.url());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to wait for URL in session {}", sessionId, e);
      UrlResponse response = new UrlResponse();
      response.setSuccess(false);
      response.setError(e.getMessage());
      return ResponseEntity.status(500).body(response);
    }
  }

  /**
   * Get the current URL of the page in the specified session.
   *
   * @param sessionId Session ID
   * @return Current URL
   */
  @GetMapping("/sessions/{sessionId}/url")
  public ResponseEntity<UrlResponse> getUrl(@PathVariable String sessionId) {
    try {
      Page page = getPage(sessionId);
      UrlResponse response = new UrlResponse();
      response.setSuccess(true);
      response.setCurrentUrl(page.url());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to get URL for session {}", sessionId, e);
      UrlResponse response = new UrlResponse();
      response.setSuccess(false);
      response.setError(e.getMessage());
      return ResponseEntity.status(500).body(response);
    }
  }

  /**
   * Execute JavaScript in the specified session.
   *
   * @param sessionId Session ID
   * @param request JavaScript execution request
   * @return Execution result
   */
  @PostMapping("/sessions/{sessionId}/evaluate")
  public ResponseEntity<EvaluateResponse> evaluate(
      @PathVariable String sessionId, @RequestBody EvaluateRequest request) {

    try {
      Page page = getPage(sessionId);
      Object result = page.evaluate(request.getScript());

      EvaluateResponse response = new EvaluateResponse();
      response.setSuccess(true);
      response.setResult(result != null ? result.toString() : null);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to evaluate JavaScript in session {}", sessionId, e);
      EvaluateResponse response = new EvaluateResponse();
      response.setSuccess(false);
      response.setError(e.getMessage());
      return ResponseEntity.status(500).body(response);
    }
  }

  /**
   * Close a browser session.
   *
   * @param sessionId Session ID
   * @return Operation result
   */
  @DeleteMapping("/sessions/{sessionId}")
  public ResponseEntity<OperationResponse> closeSession(@PathVariable String sessionId) {

    try {
      PageSession session = activeSessions.remove(sessionId);
      if (session != null && session.getPage() != null) {
        session.getPage().close();
      }

      OperationResponse response = new OperationResponse();
      response.setSuccess(true);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to close session {}", sessionId, e);
      return errorResponse(e);
    }
  }

  /**
   * Create a new API request context session.
   *
   * @param request API session creation request with base URL
   * @return Session ID and success status
   */
  @PostMapping("/api-sessions")
  public ResponseEntity<SessionResponse> createApiSession(
      @RequestBody CreateApiSessionRequest request) {

    try {
      APIRequestContext apiRequestContext =
          playwrightService.createApiRequestContext(request.getBaseUrl());
      String sessionId = UUID.randomUUID().toString();

      ApiRequestSession session = new ApiRequestSession();
      session.setApiRequestContext(apiRequestContext);
      session.setSessionId(sessionId);
      session.setBaseUrl(request.getBaseUrl());
      session.setCreatedAt(System.currentTimeMillis());
      activeApiSessions.put(sessionId, session);

      SessionResponse response = new SessionResponse();
      response.setSessionId(sessionId);
      response.setSuccess(true);

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to create API request context session", e);
      SessionResponse response = new SessionResponse();
      response.setSuccess(false);
      response.setError(e.getMessage());
      return ResponseEntity.status(500).body(response);
    }
  }

  /**
   * Close an API request context session.
   *
   * @param sessionId Session ID
   * @return Operation result
   */
  @DeleteMapping("/api-sessions/{sessionId}")
  public ResponseEntity<OperationResponse> closeApiSession(@PathVariable String sessionId) {

    try {
      ApiRequestSession session = activeApiSessions.remove(sessionId);
      if (session == null) {
        throw new RuntimeException("API session not found: " + sessionId);
      }
      session.getApiRequestContext().dispose();

      OperationResponse response = new OperationResponse();
      response.setSuccess(true);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to close API session {}", sessionId, e);
      return errorResponse(e);
    }
  }

  // Helper methods

  private Page getPage(String sessionId) {
    PageSession session = activeSessions.get(sessionId);
    if (session == null || session.getPage() == null) {
      throw new RuntimeException("Session not found: " + sessionId);
    }
    return session.getPage();
  }

  private BrowserType parseBrowserType(String browserType) {
    if (browserType == null || browserType.isBlank()) {
      return BrowserType.CHROMIUM;
    }
    try {
      return BrowserType.valueOf(browserType.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Invalid browser type: {}, defaulting to CHROMIUM", browserType);
      return BrowserType.CHROMIUM;
    }
  }

  private ResponseEntity<OperationResponse> errorResponse(Exception e) {
    OperationResponse response = new OperationResponse();
    response.setSuccess(false);
    response.setError(e.getMessage());
    return ResponseEntity.status(500).body(response);
  }

  // Request/Response DTOs

  @Data
  public static class CreateSessionRequest {
    private String browserType = "CHROMIUM";
    private boolean headless = true;
    private boolean recordVideo = false;
    private Integer viewportWidth;
    private Integer viewportHeight;
  }

  @Data
  public static class NavigateRequest {
    private String url;
  }

  @Data
  public static class FillRequest {
    private String selector;
    private String value;
  }

  @Data
  public static class ClickRequest {
    private String selector;
  }

  @Data
  public static class WaitForSelectorRequest {
    private String selector;
    private Integer timeout;
  }

  @Data
  public static class WaitForUrlRequest {
    private String urlPattern;
    private Integer timeout;
  }

  @Data
  public static class EvaluateRequest {
    private String script;
  }

  @Data
  public static class SessionResponse {
    private boolean success;
    private String sessionId;
    private String error;
  }

  @Data
  public static class OperationResponse {
    private boolean success;
    private String error;
  }

  @Data
  public static class UrlResponse {
    private boolean success;
    private String currentUrl;
    private String error;
  }

  @Data
  public static class EvaluateResponse {
    private boolean success;
    private String result;
    private String error;
  }

  @Data
  public static class CreateApiSessionRequest {
    private String baseUrl;
  }

  @Data
  private static class PageSession {
    private String sessionId;
    private Page page;
    private long createdAt;
  }

  @Data
  private static class ApiRequestSession {
    private String sessionId;
    private APIRequestContext apiRequestContext;
    private String baseUrl;
    private long createdAt;
  }
}
