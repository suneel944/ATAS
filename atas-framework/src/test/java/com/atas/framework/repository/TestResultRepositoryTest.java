package com.atas.framework.repository;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("unit-test")
class TestResultRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestResultRepository testResultRepository;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    private TestExecution testExecution;
    private TestResult testResult;

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

        testExecution = testExecutionRepository.save(testExecution);

        testResult = TestResult.builder()
                .execution(testExecution)
                .testId("com.example.TestClass.testMethod")
                .testName("Sample Test Method")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(4))
                .endTime(LocalDateTime.now().minusMinutes(3))
                .build();
    }

    @Test
    void shouldSaveAndRetrieveTestResult() {
        // When
        TestResult saved = testResultRepository.save(testResult);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<TestResult> found = testResultRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(found.get().getTestName()).isEqualTo("Sample Test Method");
        assertThat(found.get().getTestId()).isEqualTo("com.example.TestClass.testMethod");
        assertThat(found.get().getExecution().getId()).isEqualTo(testExecution.getId());
    }

    @Test
    void shouldFindByExecutionId() {
        // Given
        testResultRepository.save(testResult);
        entityManager.flush();

        // When
        List<TestResult> results = testResultRepository.findByExecutionId(testExecution.getId());

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(results.get(0).getTestName()).isEqualTo("Sample Test Method");
    }

    @Test
    void shouldFindAllTestResults() {
        // Given
        TestResult anotherResult = TestResult.builder()
                .execution(testExecution)
                .testId("com.example.TestClass.anotherTestMethod")
                .testName("Another Test Method")
                .status(TestStatus.FAILED)
                .startTime(LocalDateTime.now().minusMinutes(2))
                .endTime(LocalDateTime.now().minusMinutes(1))
                .build();

        testResultRepository.save(testResult);
        testResultRepository.save(anotherResult);
        entityManager.flush();

        // When
        List<TestResult> allResults = testResultRepository.findAll();

        // Then
        assertThat(allResults).hasSize(2);
        assertThat(allResults).extracting(TestResult::getTestName)
                .containsExactlyInAnyOrder("Sample Test Method", "Another Test Method");
    }

    @Test
    void shouldUpdateTestResult() {
        // Given
        TestResult saved = testResultRepository.save(testResult);
        entityManager.flush();

        // When
        saved.setStatus(TestStatus.FAILED);
        saved.setTestName("Updated Test Method");
        TestResult updated = testResultRepository.save(saved);
        entityManager.flush();

        // Then
        Optional<TestResult> found = testResultRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TestStatus.FAILED);
        assertThat(found.get().getTestName()).isEqualTo("Updated Test Method");
    }

    @Test
    void shouldDeleteTestResult() {
        // Given
        TestResult saved = testResultRepository.save(testResult);
        entityManager.flush();

        // When
        testResultRepository.deleteById(saved.getId());
        entityManager.flush();

        // Then
        Optional<TestResult> found = testResultRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void shouldHandleCascadeOperations() {
        // Given
        TestResult savedResult = testResultRepository.save(testResult);
        entityManager.flush();
        entityManager.clear();

        // When - Delete the parent execution
        testExecutionRepository.deleteById(testExecution.getId());
        entityManager.flush();

        // Then - The result should also be deleted due to cascade
        Optional<TestResult> found = testResultRepository.findById(savedResult.getId());
        assertThat(found).isEmpty();
    }
}
