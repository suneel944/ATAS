package com.atas.framework.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for test discovery responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDiscoveryResponse {

    /**
     * List of available test classes
     */
    private List<TestClassInfo> testClasses;

    /**
     * List of available test suites
     */
    private List<TestSuiteInfo> testSuites;

    /**
     * List of available tags
     */
    private List<String> availableTags;

    /**
     * Total number of tests discovered
     */
    private int totalTests;

    /**
     * Information about a test class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestClassInfo {
        private String className;
        private String packageName;
        private String fullName;
        private List<String> testMethods;
        private List<String> tags;
        private String type; // UI, API, etc.
    }

    /**
     * Information about a test suite
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestSuiteInfo {
        private String suiteName;
        private String className;
        private String description;
        private List<String> includedTestClasses;
        private String type; // UI, API, etc.
    }
}
