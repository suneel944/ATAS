package com.atas.framework.security;

import com.atas.framework.exception.ErrorResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.*;

/** REST controller for authentication endpoints. Provides login and token refresh functionality. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  /**
   * Login endpoint. Authenticates user and returns JWT tokens.
   *
   * @param request Login request
   * @param httpRequest HTTP request for IP and user agent
   * @return Authentication response with tokens
   */
  @PostMapping("/login")
  public ResponseEntity<?> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {
    try {
      String ipAddress = getClientIpAddress(httpRequest);
      String userAgent = httpRequest.getHeader("User-Agent");

      AuthenticationService.AuthenticationResponse response =
          authenticationService.authenticate(
              request.getUsername(), request.getPassword(), ipAddress, userAgent);

      // Set access token in cookie for server-side authentication checks
      // HttpOnly=true prevents JavaScript access, protecting against XSS attacks
      // Frontend uses localStorage for API calls, so cookie doesn't need JS access
      if (response != null && response.getAccessToken() != null) {
        try {
          Cookie accessTokenCookie = new Cookie("accessToken", response.getAccessToken());
          accessTokenCookie.setHttpOnly(true); // Security: prevent JavaScript access
          accessTokenCookie.setPath("/");
          accessTokenCookie.setMaxAge(24 * 60 * 60); // 24 hours
          httpResponse.addCookie(accessTokenCookie);
        } catch (Exception e) {
          log.warn("Failed to set accessToken cookie: {}", e.getMessage(), e);
          // Continue without cookie - client can still use localStorage
        }
      }

      return ResponseEntity.ok(response);

    } catch (BadCredentialsException e) {
      log.warn("Login failed for user: {}", request.getUsername());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              ErrorResponse.builder()
                  .status(HttpStatus.UNAUTHORIZED.value())
                  .error("Unauthorized")
                  .message("Invalid username or password")
                  .timestamp(LocalDateTime.now())
                  .build());

    } catch (LockedException e) {
      log.warn("Login attempt for locked account: {}", request.getUsername());
      return ResponseEntity.status(HttpStatus.LOCKED)
          .body(
              ErrorResponse.builder()
                  .status(HttpStatus.LOCKED.value())
                  .error("Locked")
                  .message("Account is locked. Please try again later.")
                  .timestamp(LocalDateTime.now())
                  .build());

    } catch (Exception e) {
      log.error("Unexpected error during login", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              ErrorResponse.builder()
                  .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                  .error("Internal Server Error")
                  .message("An error occurred during authentication")
                  .timestamp(LocalDateTime.now())
                  .build());
    }
  }

  /**
   * Refresh access token using refresh token.
   *
   * @param request Refresh token request
   * @return New authentication response
   */
  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    try {
      AuthenticationService.AuthenticationResponse response =
          authenticationService.refreshToken(request.getRefreshToken());

      return ResponseEntity.ok(response);

    } catch (BadCredentialsException e) {
      log.warn("Token refresh failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              ErrorResponse.builder()
                  .status(HttpStatus.UNAUTHORIZED.value())
                  .error("Unauthorized")
                  .message("Invalid or expired refresh token")
                  .timestamp(LocalDateTime.now())
                  .build());

    } catch (Exception e) {
      log.error("Unexpected error during token refresh", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(
              ErrorResponse.builder()
                  .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                  .error("Internal Server Error")
                  .message("An error occurred during token refresh")
                  .timestamp(LocalDateTime.now())
                  .build());
    }
  }

  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }

  /** Login request DTO */
  @Data
  public static class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
  }

  /** Refresh token request DTO */
  @Data
  public static class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
  }
}
