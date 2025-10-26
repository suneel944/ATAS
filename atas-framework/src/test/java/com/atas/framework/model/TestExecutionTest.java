package com.atas.framework.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TestExecutionTest {

    @Test
    void shouldCreateTestExecutionWithBuilder() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
        LocalDateTime endTime = LocalDateTime.now();

        // When
        TestExecution testExecution = TestExecution.builder()
                .executionId("test-execution-123")
                .suiteName("Sample Test Suite")
                .status(TestStatus.PASSED)
                .startTime(startTime)
                .endTime(endTime)
                .environment("test")
                .videoUrl("http://example.com/video.mp4")
                .build();

        // Then
        assertThat(testExecution.getExecutionId()).isEqualTo("test-execution-123");
        assertThat(testExecution.getSuiteName()).isEqualTo("Sample Test Suite");
        assertThat(testExecution.getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(testExecution.getStartTime()).isEqualTo(startTime);
        assertThat(testExecution.getEndTime()).isEqualTo(endTime);
        assertThat(testExecution.getEnvironment()).isEqualTo("test");
        assertThat(testExecution.getVideoUrl()).isEqualTo("http://example.com/video.mp4");
    }

    @Test
    void shouldCreateTestExecutionWithAllFields() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime endTime = LocalDateTime.now();

        // When
        TestExecution testExecution = TestExecution.builder()
                .executionId("complex-execution-456")
                .suiteName("Integration Suite")
                .status(TestStatus.FAILED)
                .startTime(startTime)
                .endTime(endTime)
                .environment("staging")
                .videoUrl("http://example.com/complex-video.mp4")
                .build();

        // Then
        assertThat(testExecution.getExecutionId()).isEqualTo("complex-execution-456");
        assertThat(testExecution.getSuiteName()).isEqualTo("Integration Suite");
        assertThat(testExecution.getStatus()).isEqualTo(TestStatus.FAILED);
        assertThat(testExecution.getStartTime()).isEqualTo(startTime);
        assertThat(testExecution.getEndTime()).isEqualTo(endTime);
        assertThat(testExecution.getEnvironment()).isEqualTo("staging");
        assertThat(testExecution.getVideoUrl()).isEqualTo("http://example.com/complex-video.mp4");
    }

    @Test
    void shouldHandleNullValues() {
        // When
        TestExecution testExecution = TestExecution.builder()
                .executionId("null-test-789")
                .suiteName("Test Suite")
                .status(TestStatus.PASSED)
                .build();

        // Then
        assertThat(testExecution.getExecutionId()).isEqualTo("null-test-789");
        assertThat(testExecution.getSuiteName()).isEqualTo("Test Suite");
        assertThat(testExecution.getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(testExecution.getStartTime()).isNull();
        assertThat(testExecution.getEndTime()).isNull();
        assertThat(testExecution.getEnvironment()).isNull();
        assertThat(testExecution.getVideoUrl()).isNull();
        assertThat(testExecution.getResults()).isEmpty();
    }

    @Test
    void shouldHaveCorrectToString() {
        // Given
        TestExecution testExecution = TestExecution.builder()
                .executionId("toString-test-123")
                .suiteName("Test Suite")
                .status(TestStatus.PASSED)
                .build();

        // When
        String toString = testExecution.toString();

        // Then
        assertThat(toString).contains("TestExecution");
        assertThat(toString).contains("executionId=toString-test-123");
        assertThat(toString).contains("suiteName=Test Suite");
        assertThat(toString).contains("status=PASSED");
    }

    @Test
    void shouldHaveCorrectEqualsAndHashCode() {
        // Given
        TestExecution test1 = TestExecution.builder()
                .executionId("test-1")
                .suiteName("Suite 1")
                .status(TestStatus.PASSED)
                .build();

        TestExecution test2 = TestExecution.builder()
                .executionId("test-1")
                .suiteName("Suite 1")
                .status(TestStatus.PASSED)
                .build();

        TestExecution test3 = TestExecution.builder()
                .executionId("test-2")
                .suiteName("Suite 1")
                .status(TestStatus.PASSED)
                .build();

        // When & Then
        assertThat(test1).isEqualTo(test2);
        assertThat(test1).isNotEqualTo(test3);
        assertThat(test1.hashCode()).isEqualTo(test2.hashCode());
        assertThat(test1.hashCode()).isNotEqualTo(test3.hashCode());
    }

    @Test
    void shouldUpdateStatusWithWithStatusMethod() {
        // Given
        TestExecution testExecution = TestExecution.builder()
                .executionId("status-test-123")
                .suiteName("Test Suite")
                .status(TestStatus.RUNNING)
                .build();

        // When
        TestExecution updatedExecution = testExecution.withStatus(TestStatus.PASSED);

        // Then
        assertThat(updatedExecution.getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(updatedExecution.getExecutionId()).isEqualTo("status-test-123");
        assertThat(updatedExecution.getSuiteName()).isEqualTo("Test Suite");
        // Original should remain unchanged
        assertThat(testExecution.getStatus()).isEqualTo(TestStatus.RUNNING);
    }
}
