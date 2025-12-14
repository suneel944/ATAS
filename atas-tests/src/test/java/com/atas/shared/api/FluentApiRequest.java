package com.atas.shared.api;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * Fluent API for making HTTP requests with method chaining.
 *
 * <p>Provides an elegant, chainable interface for API testing:
 *
 * <pre>{@code
 * FluentApiRequest request = new FluentApiRequest(apiRequestContext, baseUrl);
 *
 * Map<String, Object> response = request
 *     .get("/api/v1/journeys/current")
 *     .withAuth(accessToken)
 *     .expectStatus(200)
 *     .asMap();
 * }</pre>
 */
@Slf4j
public class FluentApiRequest {

  private final APIRequestContext request;
  private final String baseUrl;
  private final ApiRequestHelper helper;
  private String endpoint;
  private String accessToken;
  private Map<String, Object> requestBody;
  private Map<String, String> headers;
  private Integer expectedStatus;
  private APIResponse response;

  public FluentApiRequest(APIRequestContext request, String baseUrl) {
    this.request = request;
    this.baseUrl = baseUrl;
    this.helper = new ApiRequestHelper(request, baseUrl);
    this.headers = new LinkedHashMap<>();
  }

  /**
   * Sets the API endpoint for the request.
   *
   * @param endpoint API endpoint (relative to base URL)
   * @return this instance for method chaining
   */
  public FluentApiRequest endpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  /**
   * Sets the access token for authentication.
   *
   * @param token JWT access token
   * @return this instance for method chaining
   */
  public FluentApiRequest withAuth(String token) {
    this.accessToken = token;
    return this;
  }

  /**
   * Sets a custom header.
   *
   * @param name Header name
   * @param value Header value
   * @return this instance for method chaining
   */
  public FluentApiRequest withHeader(String name, String value) {
    this.headers.put(name, value);
    return this;
  }

  /**
   * Sets the request body for POST/PUT requests.
   *
   * @param body Request body as Map
   * @return this instance for method chaining
   */
  public FluentApiRequest withBody(Map<String, Object> body) {
    this.requestBody = body;
    return this;
  }

  /**
   * Sets the expected HTTP status code and validates it.
   *
   * @param status Expected status code
   * @return this instance for method chaining
   */
  public FluentApiRequest expectStatus(int status) {
    this.expectedStatus = status;
    return this;
  }

  /**
   * Executes a GET request.
   *
   * @return this instance for method chaining
   */
  public FluentApiRequest get() {
    return executeRequest("GET");
  }

  /**
   * Executes a POST request.
   *
   * @return this instance for method chaining
   */
  public FluentApiRequest post() {
    return executeRequest("POST");
  }

  /**
   * Executes a PUT request.
   *
   * @return this instance for method chaining
   */
  public FluentApiRequest put() {
    return executeRequest("PUT");
  }

  /**
   * Executes a DELETE request.
   *
   * @return this instance for method chaining
   */
  public FluentApiRequest delete() {
    return executeRequest("DELETE");
  }

  /**
   * Returns the response as a Map.
   *
   * @return Parsed JSON response as Map
   */
  public Map<String, Object> asMap() {
    ensureResponse();
    return helper.parseJson(response);
  }

  /**
   * Returns the response as a String.
   *
   * @return Response body as String
   */
  public String asString() {
    ensureResponse();
    return response.text();
  }

  /**
   * Extracts a string value from the response.
   *
   * @param key JSON key
   * @return String value
   */
  public String extractString(String key) {
    ensureResponse();
    return helper.extractString(response, key);
  }

  /**
   * Extracts a nested string value from the response.
   *
   * @param parentKey Parent JSON key
   * @param childKey Child JSON key
   * @return String value
   */
  public String extractNestedString(String parentKey, String childKey) {
    ensureResponse();
    return helper.extractNestedString(response, parentKey, childKey);
  }

  /**
   * Gets the raw APIResponse for advanced operations.
   *
   * @return APIResponse instance
   */
  public APIResponse getResponse() {
    ensureResponse();
    return response;
  }

  /**
   * Gets the response status code.
   *
   * @return HTTP status code
   */
  public int getStatus() {
    ensureResponse();
    return response.status();
  }

  /**
   * Allows custom validation of the response.
   *
   * @param validator Consumer function that receives the parsed response Map
   * @return this instance for method chaining
   */
  public FluentApiRequest validate(Consumer<Map<String, Object>> validator) {
    Map<String, Object> responseMap = asMap();
    validator.accept(responseMap);
    return this;
  }

  /** Executes the HTTP request based on the method. */
  private FluentApiRequest executeRequest(String method) {
    if (endpoint == null || endpoint.isBlank()) {
      throw new IllegalStateException("Endpoint must be set before executing request");
    }

    RequestOptions options = RequestOptions.create();

    Optional.ofNullable(accessToken)
        .filter(token -> !token.isBlank())
        .ifPresent(token -> options.setHeader("Authorization", "Bearer " + token));

    // Add custom headers
    headers.forEach(options::setHeader);

    if (requestBody != null && (method.equals("POST") || method.equals("PUT"))) {
      options.setHeader("Content-Type", "application/json");
      options.setData(requestBody);
    }

    // Log request
    String fullUrl = baseUrl + endpoint;
    log.info("→ {} {}", method, fullUrl);
    if (requestBody != null) {}

    long startTime = System.currentTimeMillis();
    response =
        switch (method) {
          case "GET" -> request.get(fullUrl, options);
          case "POST" -> request.post(fullUrl, options);
          case "PUT" -> request.put(fullUrl, options);
          case "DELETE" -> request.delete(fullUrl, options);
          default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    long duration = System.currentTimeMillis() - startTime;

    int status = response.status();
    String statusIcon =
        switch (status / 100) {
          case 2 -> "✓";
          case 4, 5 -> "✗";
          default -> "⚠";
        };
    log.info("{} {} {} ({}ms) - Status: {}", statusIcon, method, endpoint, duration, status);

    var responseText = response.text();
    if (status >= 400) {
      log.warn("  Response: {}", responseText);
    } else {
    }

    Optional.ofNullable(expectedStatus)
        .filter(expected -> response.status() != expected)
        .ifPresent(
            expected -> {
              throw new AssertionError(
                  "Expected status %d but got %d. Response: %s"
                      .formatted(expected, response.status(), responseText));
            });

    return this;
  }

  private void ensureResponse() {
    if (response == null) {
      throw new IllegalStateException(
          "No request has been executed. Call get(), post(), put(), or delete() first.");
    }
  }
}
