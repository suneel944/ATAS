package com.atas.framework.repository;

import com.atas.framework.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link TestResult} entities.  This
 * repository includes methods for retrieving results by test ID or
 * execution.
 */
@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    /**
     * Find all test results belonging to a particular execution.
     *
     * @param executionId the primary key of the execution
     * @return list of results
     */
    List<TestResult> findByExecutionId(Long executionId);
}