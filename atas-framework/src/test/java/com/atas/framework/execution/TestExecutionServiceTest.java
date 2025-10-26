package com.atas.framework.execution;

import com.atas.framework.execution.dto.TestExecutionRequest;
import com.atas.framework.execution.dto.TestExecutionResponse;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestExecutionServiceTest {

    @Mock
    private TestExecutionRepository testExecutionRepository;

    @Mock
    private TestDiscoveryService testDiscoveryService;

    @InjectMocks
    private TestExecutionService testExecutionService;

    private TestExecution testExecution;
    private TestExecutionRequest testExecutionRequest;

    @BeforeEach
    void setUp() {
        testExecution = TestExecution.builder()
                .executionId("test-execution-123")
                .suiteName("Sample Test Suite")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .environment("test")
                .build();

        testExecutionRequest = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.INDIVIDUAL_TEST)
                .testClass("com.example.TestClass")
                .environment("test")
                .browserType("Chrome")
                .recordVideo(true)
                .captureScreenshots(true)
                .timeoutMinutes(30)
                .build();
    }

    @Test
    void shouldExecuteTestsSuccessfully() {
        // Given
        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(testExecution);
        when(testDiscoveryService.discoverTestsToExecute(any(TestExecutionRequest.class)))
                .thenReturn(Arrays.asList("com.example.TestClass.testMethod"));

        // When
        TestExecutionResponse response = testExecutionService.executeTests(testExecutionRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getExecutionId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RUNNING");
        assertThat(response.getExecutionType()).isEqualTo("INDIVIDUAL_TEST");
        assertThat(response.getEnvironment()).isEqualTo("test");
        assertThat(response.getBrowserType()).isEqualTo("Chrome");
        assertThat(response.isRecordVideo()).isTrue();
        assertThat(response.isCaptureScreenshots()).isTrue();
        assertThat(response.getTimeoutMinutes()).isEqualTo(30);
        assertThat(response.getMonitoringUrl()).contains("executionId=");
        assertThat(response.getLiveUpdatesUrl()).contains("executionId=");
        assertThat(response.getResultsUrl()).contains("/api/v1/test-execution/results/");
        
        verify(testExecutionRepository).save(any(TestExecution.class));
        verify(testDiscoveryService).discoverTestsToExecute(testExecutionRequest);
    }

    @Test
    void shouldGenerateCorrectSuiteNameForIndividualTest() {
        // Given
        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(testExecution);
        when(testDiscoveryService.discoverTestsToExecute(any(TestExecutionRequest.class)))
                .thenReturn(Arrays.asList("com.example.TestClass.testMethod"));

        // When
        TestExecutionResponse response = testExecutionService.executeTests(testExecutionRequest);

        // Then
        assertThat(response.getDescription()).contains("Individual test: com.example.TestClass");
        verify(testExecutionRepository).save(argThat(execution -> 
            execution.getSuiteName().equals("com.example.TestClass")));
    }

    @Test
    void shouldGenerateCorrectSuiteNameForTaggedTests() {
        // Given
        TestExecutionRequest taggedRequest = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.TAGS)
                .tags(Arrays.asList("smoke", "regression"))
                .environment("test")
                .build();

        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(testExecution);
        when(testDiscoveryService.discoverTestsToExecute(any(TestExecutionRequest.class)))
                .thenReturn(Arrays.asList("com.example.SmokeTest", "com.example.RegressionTest"));

        // When
        TestExecutionResponse response = testExecutionService.executeTests(taggedRequest);

        // Then
        assertThat(response.getDescription()).contains("Tests with tags: smoke, regression");
        verify(testExecutionRepository).save(argThat(execution -> 
            execution.getSuiteName().equals("tagged-tests-smoke-regression")));
    }

    @Test
    void shouldGenerateCorrectSuiteNameForGrepPattern() {
        // Given
        TestExecutionRequest grepRequest = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.GREP)
                .grepPattern("LoginTest")
                .environment("test")
                .build();

        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(testExecution);
        when(testDiscoveryService.discoverTestsToExecute(any(TestExecutionRequest.class)))
                .thenReturn(Arrays.asList("com.example.LoginTest"));

        // When
        TestExecutionResponse response = testExecutionService.executeTests(grepRequest);

        // Then
        assertThat(response.getDescription()).contains("Tests matching pattern: LoginTest");
        verify(testExecutionRepository).save(argThat(execution -> 
            execution.getSuiteName().equals("grep-LoginTest")));
    }

    @Test
    void shouldGenerateCorrectSuiteNameForTestSuite() {
        // Given
        TestExecutionRequest suiteRequest = TestExecutionRequest.builder()
                .type(TestExecutionRequest.ExecutionType.SUITE)
                .suiteName("AuthenticationSuite")
                .environment("test")
                .build();

        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(testExecution);
        when(testDiscoveryService.discoverTestsToExecute(any(TestExecutionRequest.class)))
                .thenReturn(Arrays.asList("com.example.AuthenticationSuiteTest"));

        // When
        TestExecutionResponse response = testExecutionService.executeTests(suiteRequest);

        // Then
        assertThat(response.getDescription()).contains("Test suite: AuthenticationSuite");
        verify(testExecutionRepository).save(argThat(execution -> 
            execution.getSuiteName().equals("AuthenticationSuite")));
    }

    @Test
    void shouldHandleTestDiscoveryFailure() {
        // Given
        when(testExecutionRepository.save(any(TestExecution.class))).thenReturn(testExecution);
        when(testDiscoveryService.discoverTestsToExecute(any(TestExecutionRequest.class)))
                .thenThrow(new RuntimeException("Discovery failed"));

        // When
        TestExecutionResponse response = testExecutionService.executeTests(testExecutionRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTestsToExecute()).containsExactly("Tests will be discovered during execution");
        verify(testExecutionRepository).save(any(TestExecution.class));
    }
}
