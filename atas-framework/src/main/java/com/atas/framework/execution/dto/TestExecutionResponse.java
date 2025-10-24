package com.atas.framework.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for test execution response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecutionResponse {

    /**
     * Unique execution ID for tracking
     */
    private String executionId;

    /**
     * Status of the execution
     */
    private String status;

    /**
     * Type of execution that was triggered
     */
    private String executionType;

    /**
     * Description of what was executed
     */
    private String description;

    /**
     * When the execution started
     */
    private LocalDateTime startTime;

    /**
     * Estimated duration or timeout
     */
    private int timeoutMinutes;

    /**
     * List of tests that will be executed
     */
    private List<String> testsToExecute;

    /**
     * Environment where tests are running
     */
    private String environment;

    /**
     * Browser type for UI tests
     */
    private String browserType;

    /**
     * Whether video recording is enabled
     */
    private boolean recordVideo;

    /**
     * Whether screenshots are enabled
     */
    private boolean captureScreenshots;

    /**
     * URL for monitoring the execution
     */
    private String monitoringUrl;

    /**
     * URL for live updates via SSE
     */
    private String liveUpdatesUrl;

    /**
     * URL for results
     */
    private String resultsUrl;
}
