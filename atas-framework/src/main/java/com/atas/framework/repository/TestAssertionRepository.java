package com.atas.framework.repository;

import com.atas.framework.model.TestAssertion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TestAssertion} entities. Assertions validate expected vs actual values
 * during test execution. Assertions are usually accessed through their parent result, but this
 * interface allows direct access if needed.
 */
@Repository
public interface TestAssertionRepository extends JpaRepository<TestAssertion, Long> {
  /**
   * Find all assertions for a given test result ID.
   *
   * @param resultId the ID of the test result
   * @return list of assertions for the result
   */
  @Query("SELECT a FROM TestAssertion a WHERE a.result.id = :resultId")
  List<TestAssertion> findByResultId(@Param("resultId") Long resultId);
}
