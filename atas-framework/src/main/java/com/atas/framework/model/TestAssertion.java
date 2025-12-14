package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Represents an assertion made during test execution. Assertions validate expected vs actual values
 * and track their pass/fail status. Each assertion is linked to its parent {@link TestResult}.
 */
@Entity
@Table(name = "test_assertions")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestAssertion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "result_id")
  TestResult result;

  /** Type of assertion (e.g., URL Check, Visual Visibility, Response Status Code) */
  @Column(name = "type")
  String type;

  /** Expected value for the assertion */
  @Column(name = "expect_value", columnDefinition = "TEXT")
  String expectValue;

  /** Actual value observed during test execution */
  @Column(name = "actual_value", columnDefinition = "TEXT")
  String actualValue;

  /** Status of the assertion (PASSED, FAILED) */
  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  TestStatus status;

  /** When the assertion was created */
  @Column(name = "created_at")
  @Builder.Default
  LocalDateTime createdAt = LocalDateTime.now();
}
