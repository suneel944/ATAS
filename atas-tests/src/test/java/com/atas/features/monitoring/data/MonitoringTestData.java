package com.atas.features.monitoring.data;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test data class for monitoring feature tests.
 * Contains test execution and result data for API testing.
 */
@Data
@Builder
public class MonitoringTestData {
    
    private String executionId;
    private String suiteName;
    private TestStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String environment;
    
    // Predefined test execution scenarios
    public static TestExecution createTestExecutionWithMixedResults() {
        String executionId = UUID.randomUUID().toString();
        TestExecution execution = TestExecution.builder()
                .executionId(executionId)
                .suiteName("monitoring-test-suite")
                .status(TestStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .environment("test")
                .build();
        
        // Add mixed test results
        TestResult passedTest1 = TestResult.builder()
                .execution(execution)
                .testId("monitoring-test-1")
                .testName("Status endpoint returns correct format")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
                
        TestResult passedTest2 = TestResult.builder()
                .execution(execution)
                .testId("monitoring-test-2")
                .testName("Execution count aggregation works")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
                
        TestResult failedTest = TestResult.builder()
                .execution(execution)
                .testId("monitoring-test-3")
                .testName("Error handling for invalid execution ID")
                .status(TestStatus.FAILED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();
        
        execution.getResults().add(passedTest1);
        execution.getResults().add(passedTest2);
        execution.getResults().add(failedTest);
        
        return execution;
    }
    
    public static TestExecution createEmptyTestExecution() {
        return TestExecution.builder()
                .executionId(UUID.randomUUID().toString())
                .suiteName("empty-test-suite")
                .status(TestStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .environment("test")
                .build();
    }
    
    public static TestExecution createAllPassedTestExecution() {
        String executionId = UUID.randomUUID().toString();
        TestExecution execution = TestExecution.builder()
                .executionId(executionId)
                .suiteName("all-passed-suite")
                .status(TestStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .environment("test")
                .build();
        
        for (int i = 1; i <= 5; i++) {
            TestResult result = TestResult.builder()
                    .execution(execution)
                    .testId("passed-test-" + i)
                    .testName("Test " + i + " - All Passed Scenario")
                    .status(TestStatus.PASSED)
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now())
                    .build();
            execution.getResults().add(result);
        }
        
        return execution;
    }
}
