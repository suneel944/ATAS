package com.atas.framework.repository;

import com.atas.framework.model.TestExecution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing {@link TestExecution} entities. In addition to standard CRUD operations
 * provided by {@link JpaRepository}, this interface can declare derived query methods for custom
 * lookups.
 */
@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, Long> {

  /**
   * Find a test execution by its external executionId. Returns an optional that is empty if no
   * execution with the given id exists.
   *
   * @param executionId the external identifier
   * @return an optional containing the matching execution
   */
  Optional<TestExecution> findByExecutionId(String executionId);

  /**
   * Find a test execution by its external executionId with results eagerly fetched.
   *
   * @param executionId the external identifier
   * @return an optional containing the matching execution with results loaded
   */
  @EntityGraph(attributePaths = {"results"})
  @Query("SELECT e FROM TestExecution e WHERE e.executionId = :executionId")
  Optional<TestExecution> findByExecutionIdWithResults(@Param("executionId") String executionId);

  /**
   * Find all executions with results eagerly fetched.
   *
   * @return list of executions with results loaded
   */
  @EntityGraph(attributePaths = {"results"})
  @Query("SELECT e FROM TestExecution e")
  List<TestExecution> findAllWithResults();

  /**
   * Find execution by ID with results eagerly fetched.
   *
   * @param id the primary key
   * @return an optional containing the execution with results loaded
   */
  @EntityGraph(attributePaths = {"results"})
  @Query("SELECT e FROM TestExecution e WHERE e.id = :id")
  Optional<TestExecution> findByIdWithResults(@Param("id") Long id);

  /**
   * Find all executions with results eagerly fetched, with pagination.
   *
   * @param pageable pagination information
   * @return page of executions with results loaded
   */
  @EntityGraph(attributePaths = {"results"})
  @Query("SELECT e FROM TestExecution e")
  org.springframework.data.domain.Page<TestExecution> findAllWithResults(
      org.springframework.data.domain.Pageable pageable);
}
