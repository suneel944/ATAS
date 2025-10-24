package com.atas.framework.repository;

import com.atas.framework.model.TestMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TestMetric} entities.  Metrics capture
 * performance data that can be analysed later for trends and
 * reporting.
 */
@Repository
public interface TestMetricRepository extends JpaRepository<TestMetric, Long> {
}