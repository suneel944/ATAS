package com.atas.framework.repository;

import com.atas.framework.model.TestExecution;
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
class TestExecutionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    private TestExecution testExecution;

    @BeforeEach
    void setUp() {
        testExecution = TestExecution.builder()
                .executionId("test-execution-123")
                .suiteName("Sample Test Suite")
                .status(TestStatus.PASSED)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .environment("test")
                .videoUrl("http://example.com/video.mp4")
                .build();
    }

    @Test
    void shouldSaveAndRetrieveTestExecution() {
        // When
        TestExecution saved = testExecutionRepository.save(testExecution);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<TestExecution> found = testExecutionRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getExecutionId()).isEqualTo("test-execution-123");
        assertThat(found.get().getSuiteName()).isEqualTo("Sample Test Suite");
        assertThat(found.get().getStatus()).isEqualTo(TestStatus.PASSED);
    }

    @Test
    void shouldFindByExecutionId() {
        // Given
        testExecutionRepository.save(testExecution);
        entityManager.flush();

        // When
        Optional<TestExecution> found = testExecutionRepository.findByExecutionId("test-execution-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSuiteName()).isEqualTo("Sample Test Suite");
        assertThat(found.get().getStatus()).isEqualTo(TestStatus.PASSED);
    }

    @Test
    void shouldReturnEmptyWhenExecutionIdNotFound() {
        // When
        Optional<TestExecution> found = testExecutionRepository.findByExecutionId("non-existent-id");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindAllTestExecutions() {
        // Given
        TestExecution anotherTest = TestExecution.builder()
                .executionId("test-execution-456")
                .suiteName("Another Test Suite")
                .status(TestStatus.FAILED)
                .startTime(LocalDateTime.now().minusMinutes(3))
                .endTime(LocalDateTime.now())
                .environment("test")
                .build();

        testExecutionRepository.save(testExecution);
        testExecutionRepository.save(anotherTest);
        entityManager.flush();

        // When
        List<TestExecution> allExecutions = testExecutionRepository.findAll();

        // Then
        assertThat(allExecutions).hasSize(2);
        assertThat(allExecutions).extracting(TestExecution::getExecutionId)
                .containsExactlyInAnyOrder("test-execution-123", "test-execution-456");
    }

    @Test
    void shouldUpdateTestExecution() {
        // Given
        TestExecution saved = testExecutionRepository.save(testExecution);
        entityManager.flush();

        // When
        saved.setStatus(TestStatus.FAILED);
        saved.setVideoUrl("http://example.com/updated-video.mp4");
        TestExecution updated = testExecutionRepository.save(saved);
        entityManager.flush();

        // Then
        Optional<TestExecution> found = testExecutionRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TestStatus.FAILED);
        assertThat(found.get().getVideoUrl()).isEqualTo("http://example.com/updated-video.mp4");
    }

    @Test
    void shouldDeleteTestExecution() {
        // Given
        TestExecution saved = testExecutionRepository.save(testExecution);
        entityManager.flush();

        // When
        testExecutionRepository.deleteById(saved.getId());
        entityManager.flush();

        // Then
        Optional<TestExecution> found = testExecutionRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}
