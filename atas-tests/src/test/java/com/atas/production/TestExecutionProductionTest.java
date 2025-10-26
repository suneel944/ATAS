package com.atas.production;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.atas.framework.AtasFrameworkApplication;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production-level tests using PostgreSQL to simulate real-world scenarios.
 * These tests validate the system behavior under production-like conditions.
 */
@org.springframework.boot.test.context.SpringBootTest(classes = AtasFrameworkApplication.class)
@Testcontainers
@ActiveProfiles("test")
class TestExecutionProductionTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("atas_production_test")
            .withUsername("atas")
            .withPassword("ataspass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        testExecutionRepository.deleteAll();
    }

    @Test
    void shouldHandleLargeVolumeOfTestExecutions() {
        // Given - Simulate a large test suite run
        List<TestExecution> testExecutions = List.of(
            createTestExecution("login-test-1", "Authentication Suite", TestStatus.PASSED, "production"),
            createTestExecution("registration-test-1", "Authentication Suite", TestStatus.PASSED, "production"),
            createTestExecution("password-reset-test-1", "Authentication Suite", TestStatus.FAILED, "production"),
            createTestExecution("login-test-2", "Authentication Suite", TestStatus.PASSED, "production"),
            createTestExecution("registration-test-2", "Authentication Suite", TestStatus.PASSED, "production"),
            createTestExecution("password-reset-test-2", "Authentication Suite", TestStatus.PASSED, "production"),
            createTestExecution("add-to-cart-test-1", "E-commerce Suite", TestStatus.PASSED, "production"),
            createTestExecution("checkout-test-1", "E-commerce Suite", TestStatus.PASSED, "production"),
            createTestExecution("payment-test-1", "E-commerce Suite", TestStatus.FAILED, "production"),
            createTestExecution("search-test-1", "E-commerce Suite", TestStatus.PASSED, "production")
        );

        // When
        List<TestExecution> saved = testExecutionRepository.saveAll(testExecutions);

        // Then
        assertThat(saved).hasSize(10);
        
        // Verify data integrity
        List<TestExecution> allTests = testExecutionRepository.findAll();
        assertThat(allTests).hasSize(10);
        
        // Verify we can find by execution ID
        Optional<TestExecution> foundTest = testExecutionRepository.findByExecutionId("login-test-1");
        assertThat(foundTest).isPresent();
        assertThat(foundTest.get().getSuiteName()).isEqualTo("Authentication Suite");
        assertThat(foundTest.get().getStatus()).isEqualTo(TestStatus.PASSED);
    }

    @Test
    void shouldMaintainDataConsistencyUnderConcurrentAccess() {
        // Given
        TestExecution test1 = createTestExecution("concurrent-test-1", "Concurrency Suite", TestStatus.PASSED, "production");
        TestExecution test2 = createTestExecution("concurrent-test-2", "Concurrency Suite", TestStatus.PASSED, "production");

        // When
        TestExecution saved1 = testExecutionRepository.save(test1);
        TestExecution saved2 = testExecutionRepository.save(test2);

        // Then
        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());

        // Verify data persistence
        TestExecution retrieved1 = testExecutionRepository.findById(saved1.getId()).orElse(null);
        TestExecution retrieved2 = testExecutionRepository.findById(saved2.getId()).orElse(null);

        assertThat(retrieved1).isNotNull();
        assertThat(retrieved2).isNotNull();
        assertThat(retrieved1.getExecutionId()).isEqualTo("concurrent-test-1");
        assertThat(retrieved2.getExecutionId()).isEqualTo("concurrent-test-2");
    }

    @Test
    void shouldHandleComplexQueryPerformance() {
        // Given - Create a realistic test dataset
        List<TestExecution> testExecutions = List.of(
            createTestExecution("smoke-test-1", "Smoke Suite", TestStatus.PASSED, "production"),
            createTestExecution("smoke-test-2", "Smoke Suite", TestStatus.PASSED, "production"),
            createTestExecution("smoke-test-3", "Smoke Suite", TestStatus.FAILED, "production"),
            createTestExecution("regression-test-1", "Regression Suite", TestStatus.PASSED, "production"),
            createTestExecution("regression-test-2", "Regression Suite", TestStatus.PASSED, "production"),
            createTestExecution("performance-test-1", "Performance Suite", TestStatus.PASSED, "production"),
            createTestExecution("performance-test-2", "Performance Suite", TestStatus.FAILED, "production"),
            createTestExecution("api-test-1", "API Suite", TestStatus.PASSED, "production"),
            createTestExecution("api-test-2", "API Suite", TestStatus.PASSED, "production"),
            createTestExecution("ui-test-1", "UI Suite", TestStatus.PASSED, "production")
        );

        testExecutionRepository.saveAll(testExecutions);

        // When - Execute complex queries
        long startTime = System.currentTimeMillis();
        
        List<TestExecution> allTests = testExecutionRepository.findAll();
        Optional<TestExecution> smokeTest = testExecutionRepository.findByExecutionId("smoke-test-1");
        Optional<TestExecution> regressionTest = testExecutionRepository.findByExecutionId("regression-test-1");
        
        long endTime = System.currentTimeMillis();
        long queryTime = endTime - startTime;

        // Then
        assertThat(allTests).hasSize(10);
        assertThat(smokeTest).isPresent();
        assertThat(smokeTest.get().getSuiteName()).isEqualTo("Smoke Suite");
        assertThat(regressionTest).isPresent();
        assertThat(regressionTest.get().getSuiteName()).isEqualTo("Regression Suite");
        
        // Performance assertion (should complete within reasonable time)
        assertThat(queryTime).isLessThan(1000); // Less than 1 second
    }

    @Test
    void shouldValidateProductionDataConstraints() {
        // Given
        TestExecution testWithLongName = TestExecution.builder()
                .executionId("very-long-execution-id-that-might-exceed-database-constraints-and-should-be-handled-properly-by-the-system")
                .suiteName("This is a very long suite name that might exceed database constraints and should be handled properly by the system")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .environment("production")
                .videoUrl("http://example.com/very-long-video-url-that-might-exceed-constraints.mp4")
                .build();

        // When
        TestExecution saved = testExecutionRepository.save(testWithLongName);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        
        TestExecution retrieved = testExecutionRepository.findById(saved.getId()).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getExecutionId()).isEqualTo(testWithLongName.getExecutionId());
        assertThat(retrieved.getSuiteName()).isEqualTo(testWithLongName.getSuiteName());
    }

    private TestExecution createTestExecution(String executionId, String suiteName, TestStatus status, String environment) {
        return TestExecution.builder()
                .executionId(executionId)
                .suiteName(suiteName)
                .status(status)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .environment(environment)
                .videoUrl("http://example.com/" + executionId + ".mp4")
                .build();
    }
}
