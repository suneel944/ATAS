package com.atas.framework.execution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for test execution requests with various filtering options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecutionRequest {

    /**
     * Type of test execution filter
     */
    @NotNull
    private ExecutionType type;

    /**
     * Specific test class name (for individual test execution)
     */
    private String testClass;

    /**
     * Specific test method name (for individual test execution)
     */
    private String testMethod;

    /**
     * Test tags to filter by (e.g., @Tag("smoke"), @Tag("regression"))
     */
    private List<String> tags;

    /**
     * Grep pattern to match test names or classes
     */
    private String grepPattern;

    /**
     * Test suite name (e.g., "authentication-ui", "monitoring-api")
     */
    private String suiteName;

    /**
     * Environment where tests should run (dev, staging, prod)
     */
    @Builder.Default
    private String environment = "dev";

    /**
     * Additional execution parameters
     */
    private Map<String, String> parameters;

    /**
     * Whether to enable video recording
     */
    @Builder.Default
    private boolean recordVideo = true;

    /**
     * Whether to enable screenshots on failure
     */
    @Builder.Default
    private boolean captureScreenshots = true;

    /**
     * Browser type for UI tests (CHROMIUM, FIREFOX, WEBKIT)
     */
    private String browserType;

    /**
     * Maximum execution timeout in minutes
     */
    @Builder.Default
    private int timeoutMinutes = 30;

    /**
     * Execution types supported by the API
     */
    public enum ExecutionType {
        INDIVIDUAL_TEST,    // Run a specific test class/method
        TAGS,              // Run tests matching specific tags
        GREP,              // Run tests matching grep pattern
        SUITE              // Run a specific test suite
    }
}
