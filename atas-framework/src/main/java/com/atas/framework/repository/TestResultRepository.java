package com.atas.framework.repository;

import com.atas.framework.model.TestResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link TestResult} entities. This repository includes methods for
 * retrieving results by test ID or execution.
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

  /**
   * Find all test results with execution relationship eagerly loaded for efficient browsing. Uses
   * EntityGraph to eagerly fetch the execution relationship to avoid N+1 queries.
   *
   * @param pageable pagination parameters
   * @return page of results with execution loaded
   */
  @EntityGraph(attributePaths = {"execution"})
  @Query("SELECT r FROM TestResult r")
  Page<TestResult> findAllWithExecution(Pageable pageable);
}
