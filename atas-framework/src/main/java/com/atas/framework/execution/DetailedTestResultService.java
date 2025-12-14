package com.atas.framework.execution;

import com.atas.framework.execution.dto.DetailedTestResultDto;
import com.atas.framework.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for converting TestResult entities to detailed DTOs matching the comprehensive JSON
 * format. Handles parsing of JSONB columns and mapping to nested DTO structures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DetailedTestResultService {

  private final ObjectMapper objectMapper;

  /**
   * Converts a TestResult entity to a DetailedTestResultDto with all nested information.
   *
   * @param result the TestResult entity to convert
   * @return DetailedTestResultDto with all fields populated
   */
  public DetailedTestResultDto toDetailedDto(TestResult result) {
    if (result == null) {
      return null;
    }

    return DetailedTestResultDto.builder()
        .id(result.getId())
        .testId(result.getTestId())
        .testName(result.getTestName())
        .description(result.getDescription())
        .status(result.getStatus() != null ? result.getStatus().name() : null)
        .tags(result.getTags())
        .priority(result.getPriority())
        .framework(result.getFramework())
        .environment(parseEnvironment(result.getEnvironmentDetails()))
        .timing(calculateTiming(result.getStartTime(), result.getEndTime()))
        .steps(mapSteps(result.getSteps()))
        .assertions(mapAssertions(result.getAssertions()))
        .performance(extractPerformance(result.getMetrics()))
        .attachments(mapAttachments(result.getAttachments()))
        .owner(result.getOwner())
        .build();
  }

  /**
   * Parses environment details JSON string into EnvironmentDto.
   *
   * @param environmentDetailsJson JSON string containing environment details
   * @return EnvironmentDto or null if parsing fails or input is null
   */
  private DetailedTestResultDto.EnvironmentDto parseEnvironment(String environmentDetailsJson) {
    if (environmentDetailsJson == null || environmentDetailsJson.trim().isEmpty()) {
      return null;
    }

    try {
      Map<String, Object> envMap =
          objectMapper.readValue(environmentDetailsJson, new TypeReference<>() {});
      return DetailedTestResultDto.EnvironmentDto.builder()
          .env(getStringValue(envMap, "env"))
          .version(getStringValue(envMap, "version"))
          .region(getStringValue(envMap, "region"))
          .browser(getStringValue(envMap, "browser"))
          .viewport(getStringValue(envMap, "viewport"))
          .os(getStringValue(envMap, "os"))
          .headless(getBooleanValue(envMap, "headless"))
          .build();
    } catch (Exception e) {
      log.warn("Failed to parse environment details JSON: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Calculates timing information from start and end times.
   *
   * @param startTime test start time
   * @param endTime test end time
   * @return TimingDto with calculated duration
   */
  private DetailedTestResultDto.TimingDto calculateTiming(
      LocalDateTime startTime, LocalDateTime endTime) {
    if (startTime == null) {
      return null;
    }

    Long durationMs = null;
    if (endTime != null) {
      durationMs = Duration.between(startTime, endTime).toMillis();
    }

    return DetailedTestResultDto.TimingDto.builder()
        .startTime(startTime)
        .endTime(endTime)
        .durationMs(durationMs)
        .build();
  }

  /**
   * Maps TestStep entities to StepDto list.
   *
   * @param steps list of TestStep entities
   * @return list of StepDto objects
   */
  private List<DetailedTestResultDto.StepDto> mapSteps(List<TestStep> steps) {
    if (steps == null || steps.isEmpty()) {
      return Collections.emptyList();
    }

    return steps.stream()
        .sorted(
            Comparator.comparing(
                step -> step.getStepNumber() != null ? step.getStepNumber() : Integer.MAX_VALUE))
        .map(this::mapStep)
        .collect(Collectors.toList());
  }

  /**
   * Maps a single TestStep entity to StepDto.
   *
   * @param step TestStep entity
   * @return StepDto
   */
  private DetailedTestResultDto.StepDto mapStep(TestStep step) {
    Long durationMs = null;
    if (step.getStartTime() != null && step.getEndTime() != null) {
      durationMs = Duration.between(step.getStartTime(), step.getEndTime()).toMillis();
    }

    Map<String, Object> dataMap = parseStepData(step.getData());

    DetailedTestResultDto.StepDto.StepDtoBuilder builder =
        DetailedTestResultDto.StepDto.builder()
            .step(step.getStepNumber())
            .action(step.getAction())
            .description(step.getDescription())
            .status(step.getStatus() != null ? step.getStatus().name() : null)
            .timestamp(step.getStartTime())
            .durationMs(durationMs);

    // Extract common fields from data map if present
    if (dataMap != null) {
      builder.data(dataMap);
      if (dataMap.containsKey("target")) {
        builder.target(getStringValue(dataMap, "target"));
      }
      if (dataMap.containsKey("selector")) {
        builder.selector(getStringValue(dataMap, "selector"));
      }
      if (dataMap.containsKey("value")) {
        Object value = dataMap.get("value");
        builder.value(value != null ? value.toString() : null);
      }
    }

    return builder.build();
  }

  /**
   * Parses step data JSON string into a Map.
   *
   * @param dataJson JSON string containing step data
   * @return Map of step data or null if parsing fails
   */
  private Map<String, Object> parseStepData(String dataJson) {
    if (dataJson == null || dataJson.trim().isEmpty()) {
      return null;
    }

    try {
      return objectMapper.readValue(dataJson, new TypeReference<>() {});
    } catch (Exception e) {
      log.warn("Failed to parse step data JSON: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Maps TestAssertion entities to AssertionDto list.
   *
   * @param assertions list of TestAssertion entities
   * @return list of AssertionDto objects
   */
  private List<DetailedTestResultDto.AssertionDto> mapAssertions(List<TestAssertion> assertions) {
    if (assertions == null || assertions.isEmpty()) {
      return Collections.emptyList();
    }

    return assertions.stream()
        .map(
            assertion ->
                DetailedTestResultDto.AssertionDto.builder()
                    .type(assertion.getType())
                    .expect(assertion.getExpectValue())
                    .actual(assertion.getActualValue())
                    .status(assertion.getStatus() != null ? assertion.getStatus().name() : null)
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * Extracts performance metrics from TestMetric list.
   *
   * @param metrics list of TestMetric entities
   * @return PerformanceDto with extracted metrics
   */
  private DetailedTestResultDto.PerformanceDto extractPerformance(List<TestMetric> metrics) {
    if (metrics == null || metrics.isEmpty()) {
      return null;
    }

    Map<String, String> metricMap =
        metrics.stream()
            .collect(
                Collectors.toMap(
                    TestMetric::getKey, TestMetric::getValue, (existing, replacement) -> existing));

    DetailedTestResultDto.PerformanceDto.PerformanceDtoBuilder builder =
        DetailedTestResultDto.PerformanceDto.builder();

    if (metricMap.containsKey("firstContentfulPaint")) {
      builder.firstContentfulPaint(metricMap.get("firstContentfulPaint"));
    }
    if (metricMap.containsKey("largestContentfulPaint")) {
      builder.largestContentfulPaint(metricMap.get("largestContentfulPaint"));
    }

    DetailedTestResultDto.PerformanceDto performance = builder.build();

    // Return null if no performance metrics were found
    return performance.getFirstContentfulPaint() == null
            && performance.getLargestContentfulPaint() == null
        ? null
        : performance;
  }

  /**
   * Maps TestAttachment entities to AttachmentDto list.
   *
   * @param attachments list of TestAttachment entities
   * @return list of AttachmentDto objects
   */
  private List<DetailedTestResultDto.AttachmentDto> mapAttachments(
      List<TestAttachment> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return Collections.emptyList();
    }

    return attachments.stream()
        .map(
            attachment ->
                DetailedTestResultDto.AttachmentDto.builder()
                    .type(attachment.getType() != null ? attachment.getType().name() : null)
                    .name(attachment.getFileName())
                    .url(attachment.getUrl())
                    .description(attachment.getDescription())
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * Helper method to safely extract string value from map.
   *
   * @param map source map
   * @param key key to extract
   * @return string value or null
   */
  private String getStringValue(Map<String, Object> map, String key) {
    if (map == null) {
      return null;
    }
    Object value = map.get(key);
    return value != null ? value.toString() : null;
  }

  /**
   * Helper method to safely extract boolean value from map.
   *
   * @param map source map
   * @param key key to extract
   * @return boolean value or null
   */
  private Boolean getBooleanValue(Map<String, Object> map, String key) {
    if (map == null) {
      return null;
    }
    Object value = map.get(key);
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    }
    return null;
  }
}
