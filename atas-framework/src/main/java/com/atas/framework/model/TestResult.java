package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

  /** Status of this individual test */
  @Enumerated(EnumType.STRING)
  TestStatus status;

  /** When the test started */
  @Column(name = "start_time")
  LocalDateTime startTime;

  /** When the test ended */
  @Column(name = "end_time")
  LocalDateTime endTime;

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

  /** Fluent helper to update status immutably. */
  public TestResult withStatus(TestStatus newStatus) {
    return this.toBuilder().status(newStatus).build();
  }
}
