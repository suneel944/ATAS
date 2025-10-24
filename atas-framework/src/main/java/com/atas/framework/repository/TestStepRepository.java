package com.atas.framework.repository;

import com.atas.framework.model.TestStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TestStep} entities.  Step-level operations
 * are usually managed through their parent result, but this
 * interface allows direct access if needed.
 */
@Repository
public interface TestStepRepository extends JpaRepository<TestStep, Long> {
}