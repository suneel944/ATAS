package com.atas.framework.repository;

import com.atas.framework.model.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link TestExecution} entities.  In
 * addition to standard CRUD operations provided by
 * {@link JpaRepository}, this interface can declare derived
 * query methods for custom lookups.
 */
@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {

    /**
     * Find a test execution by its external executionId.  Returns an
     * optional that is empty if no execution with the given id exists.
     *
     * @param executionId the external identifier
     * @return an optional containing the matching execution
     */
    Optional<TestExecution> findByExecutionId(String executionId);
}