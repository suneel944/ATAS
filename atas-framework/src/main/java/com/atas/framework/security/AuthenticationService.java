package com.atas.framework.security;

import com.atas.framework.model.RefreshToken;
import com.atas.framework.model.User;
import com.atas.framework.repository.RefreshTokenRepository;
import com.atas.framework.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations including login, token generation, and account management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AuditService auditService;

  @Value("${atas.security.account.max-failed-attempts:5}")
  private int maxFailedAttempts;

  @Value("${atas.security.account.lockout-duration-minutes:30}")
  private int lockoutDurationMinutes;

  /**
   * Authenticate user and generate JWT tokens.
   *
   * @param username Username
   * @param password Password
   * @param ipAddress Client IP address
   * @param userAgent Client user agent
   * @return Authentication response with access and refresh tokens
   */
  @Transactional
  public AuthenticationResponse authenticate(
      String username, String password, String ipAddress, String userAgent) {
    try {
      User user =
          userRepository
              .findByUsername(username)
              .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

      // Check if account is locked
      if (user.getAccountLocked() && user.getLockedUntil() != null) {
        if (LocalDateTime.now().isBefore(user.getLockedUntil())) {
          auditService.logAuthenticationFailure(username, "ACCOUNT_LOCKED", ipAddress, userAgent);
          throw new LockedException("Account is locked. Please try again later.");
        } else {
          // Unlock account if lockout period has expired
          unlockAccount(user);
        }
      }

      // Authenticate
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(username, password));

      UserDetails userDetails = (UserDetails) authentication.getPrincipal();

      // Reset failed login attempts on successful login
      resetFailedLoginAttempts(user);

      // Generate tokens
      String accessToken = tokenProvider.generateAccessToken(userDetails);
      String refreshToken = tokenProvider.generateRefreshToken(userDetails);

      // Save refresh token
      saveRefreshToken(user, refreshToken);

      // Log successful authentication
      auditService.logAuthenticationSuccess(username, ipAddress, userAgent);

      log.info("User {} successfully authenticated", username);

      return AuthenticationResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .tokenType("Bearer")
          .username(username)
          .build();

    } catch (BadCredentialsException e) {
      handleFailedLogin(username, ipAddress, userAgent);
      throw e;
    }
  }

  /**
   * Refresh access token using refresh token.
   *
   * @param refreshTokenString Refresh token
   * @return New authentication response
   */
  @Transactional
  public AuthenticationResponse refreshToken(String refreshTokenString) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenString)
            .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

    if (refreshToken.isExpired()) {
      refreshTokenRepository.delete(refreshToken);
      throw new BadCredentialsException("Refresh token has expired");
    }

    User user = refreshToken.getUser();
    if (!user.isEnabled() || !user.isAccountNonLocked()) {
      throw new BadCredentialsException("User account is disabled or locked");
    }

    // Generate new tokens
    String newAccessToken = tokenProvider.generateAccessToken(user);
    String newRefreshToken = tokenProvider.generateRefreshToken(user);

    // Delete old refresh token and save new one
    refreshTokenRepository.delete(refreshToken);
    saveRefreshToken(user, newRefreshToken);

    return AuthenticationResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .tokenType("Bearer")
        .username(user.getUsername())
        .build();
  }

  /**
   * Handle failed login attempt.
   *
   * @param username Username
   * @param ipAddress IP address
   * @param userAgent User agent
   */
  private void handleFailedLogin(String username, String ipAddress, String userAgent) {
    User user = userRepository.findByUsername(username).orElse(null);
    if (user != null) {
      int failedAttempts = user.getFailedLoginAttempts() + 1;
      user.setFailedLoginAttempts(failedAttempts);

      if (failedAttempts >= maxFailedAttempts) {
        lockAccount(user);
        auditService.logAuthenticationFailure(
            username, "ACCOUNT_LOCKED_AFTER_FAILED_ATTEMPTS", ipAddress, userAgent);
        log.warn("Account {} locked after {} failed login attempts", username, failedAttempts);
      } else {
        userRepository.save(user);
        auditService.logAuthenticationFailure(
            username, "INVALID_CREDENTIALS", ipAddress, userAgent);
        log.warn("Failed login attempt {} for user {}", failedAttempts, username);
      }
    } else {
      auditService.logAuthenticationFailure(username, "USER_NOT_FOUND", ipAddress, userAgent);
    }
  }

  /**
   * Reset failed login attempts on successful login.
   *
   * @param user User
   */
  private void resetFailedLoginAttempts(User user) {
    if (user.getFailedLoginAttempts() > 0) {
      user.setFailedLoginAttempts(0);
      userRepository.save(user);
    }
  }

  /**
   * Lock user account.
   *
   * @param user User
   */
  private void lockAccount(User user) {
    user.setAccountLocked(true);
    user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
    userRepository.save(user);
  }

  /**
   * Unlock user account.
   *
   * @param user User
   */
  private void unlockAccount(User user) {
    user.setAccountLocked(false);
    user.setLockedUntil(null);
    user.setFailedLoginAttempts(0);
    userRepository.save(user);
  }

  /**
   * Save refresh token to database.
   *
   * @param user User
   * @param token Refresh token string (JWT)
   */
  private void saveRefreshToken(User user, String token) {
    // Calculate expiration (7 days from now)
    LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

    RefreshToken refreshToken =
        RefreshToken.builder()
            .user(user)
            .token(token) // Store JWT token
            .expiresAt(expiresAt)
            .build();

    refreshTokenRepository.save(refreshToken);
  }

  /** Authentication response DTO. */
  @lombok.Data
  @lombok.Builder
  public static class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String username;
  }
}
