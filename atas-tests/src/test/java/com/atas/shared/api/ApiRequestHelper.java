package com.atas.shared.api;

import static com.atas.shared.assertions.CommonAssertions.getNestedString;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for making authenticated API requests.
 *
 * <p>Provides convenience methods for common API operations with authentication.
 */
public class ApiRequestHelper {

  private final APIRequestContext request;
  private final String baseUrl;
  private final ObjectMapper objectMapper;

  public ApiRequestHelper(APIRequestContext request, String baseUrl) {
    this.request = request;
    this.baseUrl = baseUrl;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Makes an authenticated GET request.
   *
   * @param endpoint API endpoint (relative to base URL)
   * @param accessToken JWT access token
   * @return API response
   */
  public APIResponse get(String endpoint, String accessToken) {
    return request.get(
        baseUrl + endpoint,
        RequestOptions.create().setHeader("Authorization", "Bearer " + accessToken));
  }

  /**
   * Makes an authenticated POST request.
   *
   * @param endpoint API endpoint (relative to base URL)
   * @param accessToken JWT access token
   * @param requestBody Request body as Map
   * @return API response
   */
  public APIResponse post(String endpoint, String accessToken, Map<String, Object> requestBody) {
    return request.post(
        baseUrl + endpoint,
        RequestOptions.create()
            .setHeader("Authorization", "Bearer " + accessToken)
            .setHeader("Content-Type", "application/json")
            .setData(requestBody));
  }

  /**
   * Makes an authenticated PUT request.
   *
   * @param endpoint API endpoint (relative to base URL)
   * @param accessToken JWT access token
   * @param requestBody Request body as Map
   * @return API response
   */
  public APIResponse put(String endpoint, String accessToken, Map<String, Object> requestBody) {
    return request.put(
        baseUrl + endpoint,
        RequestOptions.create()
            .setHeader("Authorization", "Bearer " + accessToken)
            .setHeader("Content-Type", "application/json")
            .setData(requestBody));
  }

  /**
   * Makes an authenticated DELETE request.
   *
   * @param endpoint API endpoint (relative to base URL)
   * @param accessToken JWT access token
   * @return API response
   */
  public APIResponse delete(String endpoint, String accessToken) {
    return request.delete(
        baseUrl + endpoint,
        RequestOptions.create().setHeader("Authorization", "Bearer " + accessToken));
  }

  /**
   * Parses JSON response to Map.
   *
   * <p>Handles both JSON objects and arrays. If the response is an array, it wraps it in a Map with
   * a "content" key to maintain compatibility with paginated response structures.
   *
   * @param response API response
   * @return Parsed JSON as Map
   */
  public Map<String, Object> parseJson(APIResponse response) {
    try {
      String text = response.text();
      JsonNode jsonNode = objectMapper.readTree(text);

      // If the response is an array, wrap it in a map with "content" key
      if (jsonNode.isArray()) {
        List<Map<String, Object>> arrayContent =
            objectMapper.convertValue(jsonNode, new TypeReference<List<Map<String, Object>>>() {});
        Map<String, Object> wrappedResponse = new LinkedHashMap<>();
        wrappedResponse.put("content", arrayContent);
        return wrappedResponse;
      }

      // Otherwise, parse as a regular object
      return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON response", e);
    }
  }

  /**
   * Extracts a string value from JSON response.
   *
   * @param response API response
   * @param key JSON key
   * @return String value
   */
  public String extractString(APIResponse response, String key) {
    Map<String, Object> json = parseJson(response);
    Object value = json.get(key);
    return value != null ? value.toString() : null;
  }

  /**
   * Extracts a nested string value from JSON response.
   *
   * @param response API response
   * @param parentKey Parent JSON key
   * @param childKey Child JSON key
   * @return String value
   */
  public String extractNestedString(APIResponse response, String parentKey, String childKey) {
    Map<String, Object> json = parseJson(response);
    String path = parentKey + "." + childKey;
    return getNestedString(json, path);
  }
}
