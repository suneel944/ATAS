package com.atas.framework.integration;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("integration-test")
class DatabaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("atas_integration_test")
            .withUsername("atas")
            .withPassword("ataspass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    private TestExecution testExecution;

    @BeforeEach
    void setUp() {
        testExecution = TestExecution.builder()
                .executionId("integration-test-123")
                .suiteName("Integration Suite")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .environment("integration")
                .videoUrl("http://example.com/integration-video.mp4")
                .build();
    }

    @Test
    void shouldPersistAndRetrieveTestExecutionWithPostgreSQL() {
        // When
        TestExecution saved = testExecutionRepository.save(testExecution);

        // Then
        TestExecution found = testExecutionRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getExecutionId()).isEqualTo("integration-test-123");
        assertThat(found.getSuiteName()).isEqualTo("Integration Suite");
        assertThat(found.getStatus()).isEqualTo(TestStatus.PASSED);
        assertThat(found.getEnvironment()).isEqualTo("integration");
    }

    @Test
    void shouldPerformComplexQueryWithPostgreSQL() {
        // Given
        TestExecution test1 = TestExecution.builder()
                .executionId("test-1")
                .suiteName("Suite A")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(LocalDateTime.now().minusMinutes(5))
                .environment("integration")
                .build();

        TestExecution test2 = TestExecution.builder()
                .executionId("test-2")
                .suiteName("Suite A")
                .status(TestStatus.FAILED)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .environment("integration")
                .build();

        TestExecution test3 = TestExecution.builder()
                .executionId("test-3")
                .suiteName("Suite B")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(3))
                .endTime(LocalDateTime.now())
                .environment("integration")
                .build();

        testExecutionRepository.saveAll(List.of(test1, test2, test3));

        // When
        List<TestExecution> allTests = testExecutionRepository.findAll();
        Optional<TestExecution> foundByExecutionId = testExecutionRepository.findByExecutionId("test-1");

        // Then
        assertThat(allTests).hasSize(3);
        assertThat(foundByExecutionId).isPresent();
        assertThat(foundByExecutionId.get().getSuiteName()).isEqualTo("Suite A");
        assertThat(foundByExecutionId.get().getStatus()).isEqualTo(TestStatus.PASSED);
    }

    @Test
    @Transactional
    void shouldHandleTransactionRollback() {
        // Given - Clear existing data first
        testExecutionRepository.deleteAll();
        
        TestExecution validTest = TestExecution.builder()
                .executionId("valid-test-123")
                .suiteName("Test Suite")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(2))
                .endTime(LocalDateTime.now())
                .environment("integration")
                .build();

        // When
        testExecutionRepository.save(validTest);

        // Then
        List<TestExecution> allTests = testExecutionRepository.findAll();
        assertThat(allTests).hasSize(1);
        assertThat(allTests.get(0).getExecutionId()).isEqualTo("valid-test-123");
    }
}
