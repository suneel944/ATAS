package com.atas.shared.assertions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;

/**
 * Common assertions for API response validation.
 *
 * <p>Provides reusable validation patterns for common response structures like lists, nested
 * objects, and field extractions.
 */
public class CommonAssertions {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS_TYPE =
      new TypeReference<>() {};
  private static final TypeReference<List<String>> LIST_OF_STRINGS_TYPE = new TypeReference<>() {};

  private CommonAssertions() {
    // Utility class - prevent instantiation
  }

  /**
   * Converts a Map to JsonNode for flexible navigation.
   *
   * @param map Map to convert
   * @return JsonNode representation
   */
  private static JsonNode toJsonNode(Map<String, Object> map) {
    return objectMapper.valueToTree(map);
  }

  /**
   * Extracts a list from a response Map by key using type-safe deserialization.
   *
   * @param response Response Map
   * @param key Key containing the list
   * @return List of Maps
   * @throws IllegalArgumentException if the key doesn't exist or value is not an array
   */
  public static List<Map<String, Object>> getList(Map<String, Object> response, String key) {
    JsonNode node = toJsonNode(response);
    JsonNode listNode =
        Optional.ofNullable(node.get(key))
            .filter(n -> !n.isNull())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Response does not contain a list at key '%s'".formatted(key)));

    if (!listNode.isArray()) {
      throw new IllegalArgumentException("Value at key '%s' is not a list/array".formatted(key));
    }

    return objectMapper.convertValue(listNode, LIST_OF_MAPS_TYPE);
  }

  /**
   * Asserts that a field exists and is not null.
   *
   * @param response Response Map
   * @param fieldName Field name to check
   */
  public static void assertFieldExists(Map<String, Object> response, String fieldName) {
    JsonNode node = toJsonNode(response);
    JsonNode fieldNode =
        Optional.ofNullable(node.get(fieldName))
            .filter(field -> !field.isNull())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Field '%s' does not exist or is null".formatted(fieldName)));

    Assertions.assertThat(fieldNode)
        .as("Field '%s' should exist and not be null", fieldName)
        .isNotNull();
  }

  /**
   * Asserts that a field has the expected value.
   *
   * @param response Response Map
   * @param fieldName Field name to check
   * @param expectedValue Expected value
   */
  public static void assertFieldEquals(
      Map<String, Object> response, String fieldName, Object expectedValue) {
    JsonNode node = toJsonNode(response);
    JsonNode fieldNode =
        Optional.ofNullable(node.get(fieldName))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Field '%s' does not exist in response".formatted(fieldName)));

    String actualValue = fieldNode.asText();
    Assertions.assertThat(actualValue)
        .as("Field '%s' should equal '%s'", fieldName, expectedValue)
        .isEqualTo(expectedValue.toString());
  }

  /**
   * Asserts that a list has the expected size.
   *
   * @param response Response Map
   * @param listKey Key containing the list
   * @param expectedSize Expected list size
   */
  public static void assertListSize(
      Map<String, Object> response, String listKey, int expectedSize) {
    List<Map<String, Object>> list = getList(response, listKey);
    Assertions.assertThat(list)
        .as("List at key '%s' should have size %d", listKey, expectedSize)
        .hasSize(expectedSize);
  }

  /**
   * Extracts a nested value from a response Map using a dot-separated path.
   *
   * <p>Example: getNestedValue(response, "config.pos.type") extracts
   * response["config"]["pos"]["type"]
   *
   * @param response Response Map
   * @param path Dot-separated path to the nested value (e.g., "config.pos.type")
   * @return The extracted value, or null if any part of the path doesn't exist
   */
  public static Object getNestedValue(Map<String, Object> response, String path) {
    JsonNode node = toJsonNode(response);
    String[] keys = path.split("\\.");
    JsonNode current = node;

    for (String key : keys) {
      if (current == null || !current.has(key)) {
        return null;
      }
      current = current.get(key);
    }

    return current.isNull() ? null : objectMapper.convertValue(current, Object.class);
  }

  /**
   * Asserts that a nested field has the expected value.
   *
   * <p>Example: assertNestedFieldEquals(response, "config.pos.type", "OREXSYS")
   *
   * @param response Response Map
   * @param path Dot-separated path to the nested field
   * @param expectedValue Expected value
   */
  public static void assertNestedFieldEquals(
      Map<String, Object> response, String path, Object expectedValue) {
    Object actualValue = getNestedValue(response, path);
    Assertions.assertThat(actualValue)
        .as("Nested field at path '%s' should equal '%s'", path, expectedValue)
        .isEqualTo(expectedValue);
  }

  /**
   * Extracts a string value from a response Map by key using type-safe extraction.
   *
   * @param response Response Map
   * @param key Key containing the string value
   * @return String value, or null if the key doesn't exist or value is null
   */
  public static String getString(Map<String, Object> response, String key) {
    JsonNode node = toJsonNode(response);
    JsonNode valueNode = node.get(key);
    if (valueNode == null || valueNode.isNull()) {
      return null;
    }
    return valueNode.asText();
  }

  /**
   * Extracts a string value from a nested path in a response Map.
   *
   * <p>Example: getNestedString(response, "config.pos.type") extracts
   * response["config"]["pos"]["type"] as a String
   *
   * @param response Response Map
   * @param path Dot-separated path to the nested value (e.g., "config.pos.type")
   * @return String value, or null if any part of the path doesn't exist
   */
  public static String getNestedString(Map<String, Object> response, String path) {
    Object value = getNestedValue(response, path);
    return value != null ? value.toString() : null;
  }

  /**
   * Extracts a double value from a response Map by key using type-safe extraction.
   *
   * @param response Response Map
   * @param key Key containing the numeric value
   * @return Double value, or 0.0 if the key doesn't exist or value is null
   */
  public static double getDouble(Map<String, Object> response, String key) {
    JsonNode node = toJsonNode(response);
    JsonNode valueNode = node.get(key);
    if (valueNode == null || valueNode.isNull()) {
      return 0.0;
    }
    return valueNode.asDouble();
  }

  /**
   * Extracts a list of strings from a response Map by key using type-safe deserialization.
   *
   * @param response Response Map
   * @param key Key containing the list
   * @return List of Strings
   * @throws IllegalArgumentException if the key doesn't exist or value is not an array
   */
  public static List<String> getStringList(Map<String, Object> response, String key) {
    JsonNode node = toJsonNode(response);
    JsonNode listNode =
        Optional.ofNullable(node.get(key))
            .filter(n -> !n.isNull())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Response does not contain a list at key '%s'".formatted(key)));

    if (!listNode.isArray()) {
      throw new IllegalArgumentException("Value at key '%s' is not a list/array".formatted(key));
    }

    return objectMapper.convertValue(listNode, LIST_OF_STRINGS_TYPE);
  }
}
