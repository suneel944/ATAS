package com.atas.framework.execution.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comprehensive DTO representing detailed test result information in JSON format. Matches the
 * structure expected for API and UI test results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailedTestResultDto {

  /** Database ID of the test result */
  private Long id;

  /** Unique test identifier (e.g. class.method) */
  private String testId;

  /** Human-friendly name of the test */
  private String testName;

  /** Detailed description of what the test verifies */
  private String description;

  /** Status of the test (PASSED, FAILED, SKIPPED, ERROR, TIMEOUT) */
  private String status;

  /** Array of tags associated with the test */
  private List<String> tags;

  /** Test priority level (e.g., P0_CRITICAL, P1_HIGH) */
  private String priority;

  /** Testing framework used (e.g., JUnit, Playwright) */
  private String framework;

  /** Environment information */
  private EnvironmentDto environment;

  /** Timing information */
  private TimingDto timing;

  /** Steps executed during the test */
  private List<StepDto> steps;

  /** Assertions made during the test */
  private List<AssertionDto> assertions;

  /** Performance metrics (for UI tests) */
  private PerformanceDto performance;

  /** Attachments (screenshots, videos, logs) */
  private List<AttachmentDto> attachments;

  /** Team or individual responsible for the test */
  private String owner;

  /** Environment information DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class EnvironmentDto {
    private String env;
    private String version;
    private String region;
    private String browser;
    private String viewport;
    private String os;
    private Boolean headless;
  }

  /** Timing information DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TimingDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
  }

  /** Step information DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StepDto {
    private Integer step;
    private String action;
    private String description;
    private String status;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    private Long durationMs;
    private String target;
    private String selector;
    private String value;
  }

  /** Assertion information DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AssertionDto {
    private String type;
    private String expect;
    private Object actual;
    private String status;
    private String selector;
  }

  /** Performance metrics DTO (for UI tests) */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PerformanceDto {
    private String firstContentfulPaint;
    private String largestContentfulPaint;
  }

  /** Attachment information DTO */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AttachmentDto {
    private String type;
    private String name;
    private String url;
    private String description;
  }
}
