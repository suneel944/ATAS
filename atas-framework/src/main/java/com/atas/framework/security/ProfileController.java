package com.atas.framework.security;

import com.atas.framework.model.User;
import com.atas.framework.repository.UserRepository;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for user profile endpoints. */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

  private final UserRepository userRepository;

  /**
   * Get current user profile information.
   *
   * @return User profile information
   */
  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      log.info(
          "Profile request - Authentication: {}",
          authentication != null ? authentication.getName() : "null");

      if (authentication == null) {
        log.warn("No authentication found in security context");
        return ResponseEntity.status(401)
            .body(
                Map.of(
                    "error", "Unauthorized",
                    "message", "Authentication required"));
      }

      if (!authentication.isAuthenticated()) {
        log.warn("Authentication not authenticated");
        return ResponseEntity.status(401)
            .body(
                Map.of(
                    "error", "Unauthorized",
                    "message", "Authentication required"));
      }

      String username = authentication.getName();
      log.info("Retrieving profile for user: {}", username);

      if (username == null || username.isEmpty()) {
        log.error("Username is null or empty");
        return ResponseEntity.status(401)
            .body(
                Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid authentication"));
      }

      if (userRepository == null) {
        log.error("UserRepository is null!");
        return ResponseEntity.status(500)
            .body(
                Map.of(
                    "error", "Internal Server Error",
                    "message", "UserRepository not initialized"));
      }

      java.util.Optional<User> userOpt = userRepository.findByUsername(username);
      if (userOpt.isEmpty()) {
        log.error("User not found in database: {}", username);
        return ResponseEntity.status(404)
            .body(Map.of("error", "Not Found", "message", "User not found: " + username));
      }

      User user = userOpt.get();

      String email = user.getEmail() != null ? user.getEmail() : "";
      log.info("User found: {}, email: {}", user.getUsername(), email);

      ProfileResponse response =
          ProfileResponse.builder()
              .username(user.getUsername())
              .email(email)
              .enabled(user.isEnabled())
              .accountNonLocked(user.isAccountNonLocked())
              .authorities(
                  user.getAuthorities() != null
                      ? user.getAuthorities().stream()
                          .map(auth -> auth.getAuthority() != null ? auth.getAuthority() : "")
                          .toList()
                      : java.util.Collections.emptyList())
              .build();

      log.info("Successfully retrieved profile for user: {}", username);
      return ResponseEntity.ok(response);

    } catch (UsernameNotFoundException e) {
      log.warn("User not found: {}", e.getMessage());
      return ResponseEntity.status(404)
          .body(
              Map.of(
                  "error", "Not Found",
                  "message", "User not found"));

    } catch (Exception e) {
      log.error("Error retrieving user profile", e);
      e.printStackTrace(); // Print full stack trace for debugging
      return ResponseEntity.status(500)
          .body(
              Map.of(
                  "error",
                  "Internal Server Error",
                  "message",
                  "An unexpected error occurred: "
                      + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
    }
  }

  /** Profile response DTO */
  @Data
  @lombok.Builder
  public static class ProfileResponse {
    private String username;
    private String email;
    private Boolean enabled;
    private Boolean accountNonLocked;
    private java.util.List<String> authorities;
  }
}
