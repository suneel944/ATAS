package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Entity representing a refresh token for JWT token renewal. Refresh tokens allow users to obtain
 * new access tokens without re-authenticating.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  @Column(unique = true, nullable = false, length = 512)
  String token;

  @Column(name = "expires_at", nullable = false)
  LocalDateTime expiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  LocalDateTime createdAt = LocalDateTime.now();

  /** Check if the token is expired */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }
}
