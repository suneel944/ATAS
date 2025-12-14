package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents a single step or action within a test. A step may correspond to a user action (e.g.
 * clicking a button) or an assertion. Steps are linked to their parent {@link TestResult}.
 */
@Entity
@Table(name = "test_steps")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestStep {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "result_id")
  TestResult result;

  /** Sequential step number within the test */
  @Column(name = "step_number")
  Integer stepNumber;

  /** Action performed in this step (e.g., Navigate, Fill, Click, POST /api/v1/tables/links) */
  @Column(name = "action")
  String action;

  @Column(name = "description", columnDefinition = "TEXT")
  String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  TestStatus status;

  @Column(name = "start_time")
  LocalDateTime startTime;

  @Column(name = "end_time")
  LocalDateTime endTime;

  /**
   * JSON object containing step-specific data (requestPayload, responseCode, selector, value,
   * target, etc.)
   */
  @Column(name = "data", columnDefinition = "JSONB")
  @JdbcTypeCode(SqlTypes.JSON)
  String data;
}
