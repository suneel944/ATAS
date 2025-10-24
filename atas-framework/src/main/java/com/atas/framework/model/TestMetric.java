package com.atas.framework.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Represents a key-value performance metric captured during a test
 * run.  Common examples include page load times or API response
 * durations.  Metrics can be analysed later for trend reporting.
 */
@Entity
@Table(name = "test_metrics")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id")
    TestResult result;

    @Column(name = "metric_key")
    String key;

    @Column(name = "metric_value")
    String value;
}