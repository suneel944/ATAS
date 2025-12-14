package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entity for audit logging. Records all authentication events and sensitive operations for security
 * monitoring and compliance.
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  User user;

  @Column String username;

  @Column(nullable = false, length = 100)
  String action; // e.g., LOGIN, LOGOUT, TEST_EXECUTE, USER_CREATE

  @Column(name = "resource_type", length = 100)
  String resourceType; // e.g., TEST_EXECUTION, USER

  @Column(name = "resource_id", length = 255)
  String resourceId; // ID of the resource affected

  @Column(name = "ip_address", length = 45) // IPv6 max length
  String ipAddress;

  @Column(name = "user_agent", columnDefinition = "TEXT")
  String userAgent;

  @Column(nullable = false, updatable = false)
  @Builder.Default
  LocalDateTime timestamp = LocalDateTime.now();
}
