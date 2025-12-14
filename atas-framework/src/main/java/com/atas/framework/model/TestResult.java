package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing the outcome of a single test. Each result belongs to a {@link TestExecution}
 * and contains child steps, attachments and metrics. The testId is typically the fully qualified
 * method name or a custom identifier provided by the caller.
 */
@Entity
@Table(name = "test_results")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  /** Parent execution that this result belongs to */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "execution_id")
  TestExecution execution;

  /** Unique test identifier (e.g. class.method) */
  @Column(name = "test_id")
  String testId;

  /** Human friendly name of the test */
  @Column(name = "test_name")
  String testName;

  /** Detailed description of what the test verifies */
  @Column(name = "description", columnDefinition = "TEXT")
  String description;

  /** Status of this individual test */
  @Enumerated(EnumType.STRING)
  TestStatus status;

  /** When the test started */
  @Column(name = "start_time")
  LocalDateTime startTime;

  /** When the test ended */
  @Column(name = "end_time")
  LocalDateTime endTime;

  /** Array of tags associated with the test (e.g., API, REGRESSION, PAYMENTS) */
  @Column(name = "tags", columnDefinition = "JSONB")
  @JdbcTypeCode(SqlTypes.JSON)
  List<String> tags;

  /** Test priority level (e.g., P0_CRITICAL, P1_HIGH, P2_MEDIUM, P3_LOW) */
  @Column(name = "priority")
  String priority;

  /** Testing framework used (e.g., JUnit, Playwright, TestNG) */
  @Column(name = "framework")
  String framework;

  /** JSON object containing environment-specific details */
  @Column(name = "environment_details", columnDefinition = "JSONB")
  @JdbcTypeCode(SqlTypes.JSON)
  String environmentDetails;

  /** Team or individual responsible for the test */
  @Column(name = "owner")
  String owner;

  /** Steps that comprise this test execution */
  @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  List<TestStep> steps = new ArrayList<>();

  /** Attachments (screenshots, videos) for this test */
  @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  List<TestAttachment> attachments = new ArrayList<>();

  /** Performance metrics captured during this test */
  @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  List<TestMetric> metrics = new ArrayList<>();

  /** Assertions made during test execution */
  @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  List<TestAssertion> assertions = new ArrayList<>();

  /** Fluent helper to update status immutably. */
  public TestResult withStatus(TestStatus newStatus) {
    return this.toBuilder().status(newStatus).build();
  }
}
