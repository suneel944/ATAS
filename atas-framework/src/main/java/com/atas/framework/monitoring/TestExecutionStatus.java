package com.atas.framework.monitoring;


import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * DTO returned by the monitoring API summarising the current
 * execution status.  It aggregates counts of tests by status and
 * calculates overall progress based on completed tests.  The
 * {@code progress} field is expressed as a percentage (0–100).
 */
@Data
@Builder
public class TestExecutionStatus {
    private String executionId;
    private String suiteName;
    private String environment;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int total;
    private int passed;
    private int failed;
    private int skipped;
    private int running;
    private double progress;
    private Duration duration;

    /** Compute the progress percentage based on counts. */
    public static double computeProgress(int total, int passed, int failed, int skipped) {
        if (total == 0) {
            return 0.0;
        }
        int completed = passed + failed + skipped;
        return ((double) completed) / total * 100.0;
    }
}