package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * User entity for authentication and authorization. Implements Spring Security's UserDetails
 * interface for seamless integration with Spring Security.
 */
@Entity
@Table(name = "users")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(unique = true, nullable = false)
  String username;

  @Column(name = "password_hash", nullable = false)
  String passwordHash;

  @Column String email;

  /** Comma-separated roles: ADMIN,USER,VIEWER */
  @Column(nullable = false)
  String roles;

  @Column(nullable = false)
  @Builder.Default
  Boolean enabled = true;

  @Column(name = "account_locked", nullable = false)
  @Builder.Default
  Boolean accountLocked = false;

  @Column(name = "failed_login_attempts", nullable = false)
  @Builder.Default
  Integer failedLoginAttempts = 0;

  @Column(name = "locked_until")
  LocalDateTime lockedUntil;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // UserDetails implementation

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true; // Account expiration not implemented
  }

  @Override
  public boolean isAccountNonLocked() {
    if (accountLocked && lockedUntil != null) {
      return LocalDateTime.now().isAfter(lockedUntil);
    }
    return !accountLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true; // Credential expiration not implemented
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public java.util.Collection<? extends GrantedAuthority> getAuthorities() {
    Set<GrantedAuthority> authorities = new HashSet<>();
    if (roles != null && !roles.isEmpty()) {
      Arrays.stream(roles.split(","))
          .map(String::trim)
          .map(role -> "ROLE_" + role.toUpperCase())
          .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
    }
    return authorities;
  }

  /** Helper method to check if user has a specific role */
  public boolean hasRole(String role) {
    if (roles == null || roles.isEmpty()) {
      return false;
    }
    return Arrays.stream(roles.split(","))
        .map(String::trim)
        .anyMatch(r -> r.equalsIgnoreCase(role));
  }
}
