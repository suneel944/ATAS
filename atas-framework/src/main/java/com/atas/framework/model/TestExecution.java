package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entity representing a single execution of a test suite. Each execution aggregates a number of
 * {@link TestResult} records and stores metadata such as environment and video recording. The
 * execution identifier is used to correlate results across services and may be exposed externally
 * via the API.
 */
@Entity
@Table(name = "test_executions")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  /** Unique identifier for the execution (e.g. UUID) */
  @Column(name = "execution_id", unique = true, nullable = false, updatable = false)
  String executionId;

  /** Human friendly name of the suite (e.g. regression, smoke) */
  @Column(name = "suite_name")
  String suiteName;

  /** Current status of the execution */
  @Enumerated(EnumType.STRING)
  TestStatus status;

  /** Timestamp when the execution started */
  @Column(name = "start_time")
  LocalDateTime startTime;

  /** Timestamp when the execution ended */
  @Column(name = "end_time")
  LocalDateTime endTime;

  /** Name of the environment (dev, staging, prod) */
  @Column(name = "environment")
  String environment;

  /** Optional URL to the aggregated video recording of the suite */
  @Column(name = "video_url")
  String videoUrl;

  /** Standard output from test execution */
  @Column(name = "stdout_output", columnDefinition = "TEXT")
  String stdoutOutput;

  /** Standard error output from test execution */
  @Column(name = "stderr_output", columnDefinition = "TEXT")
  String stderrOutput;

  /** Whether output capture is complete */
  @Column(name = "output_complete")
  @Builder.Default
  Boolean outputComplete = false;

  /**
   * Test results belonging to this execution. Cascade on persist to ensure results are stored
   * automatically when execution is saved.
   */
  @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  List<TestResult> results = new ArrayList<>();

  /** Fluent helper to update status immutably. */
  public TestExecution withStatus(TestStatus newStatus) {
    return this.toBuilder().status(newStatus).build();
  }
}
