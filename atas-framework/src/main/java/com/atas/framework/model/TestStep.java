package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

  @Column(name = "description")
  String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  TestStatus status;

  @Column(name = "start_time")
  LocalDateTime startTime;

  @Column(name = "end_time")
  LocalDateTime endTime;
}
