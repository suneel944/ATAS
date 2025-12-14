package com.atas.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Provider for JWT token generation and validation. Uses HS256 (symmetric) algorithm for
 * simplicity. For production, consider RS256 (asymmetric) for better security.
 */
@Component
@Slf4j
public class JwtTokenProvider {

  @Value(
      "${atas.security.jwt.secret:your-256-bit-secret-key-change-this-in-production-minimum-32-characters}")
  private String jwtSecret;

  @Value("${atas.security.jwt.expiration:86400000}") // 24 hours in milliseconds
  private long jwtExpirationMs;

  @Value("${atas.security.jwt.refresh-expiration:604800000}") // 7 days in milliseconds
  private long refreshExpirationMs;

  private SecretKey getSigningKey() {
    // Ensure secret is at least 32 characters for HS256
    if (jwtSecret.length() < 32) {
      log.warn("JWT secret is too short. Using padded secret for HS256.");
      String paddedSecret = jwtSecret + "0".repeat(32 - jwtSecret.length());
      return Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
    }
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Generate JWT access token for a user.
   *
   * @param userDetails User details
   * @return JWT token string
   */
  public String generateAccessToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("authorities", userDetails.getAuthorities());
    return generateToken(claims, userDetails.getUsername(), jwtExpirationMs);
  }

  /**
   * Generate refresh token for a user.
   *
   * @param userDetails User details
   * @return Refresh token string
   */
  public String generateRefreshToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    return generateToken(claims, userDetails.getUsername(), refreshExpirationMs);
  }

  private String generateToken(Map<String, Object> claims, String subject, long expiration) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Extract username from token.
   *
   * @param token JWT token
   * @return Username
   */
  public String getUsernameFromToken(String token) {
    return getClaimFromToken(token, Claims::getSubject);
  }

  /**
   * Extract expiration date from token.
   *
   * @param token JWT token
   * @return Expiration date
   */
  public Date getExpirationDateFromToken(String token) {
    return getClaimFromToken(token, Claims::getExpiration);
  }

  /**
   * Extract a specific claim from token.
   *
   * @param token JWT token
   * @param claimsResolver Function to extract claim
   * @param <T> Claim type
   * @return Claim value
   */
  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Get all claims from token.
   *
   * @param token JWT token
   * @return Claims
   */
  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  /**
   * Check if token is expired.
   *
   * @param token JWT token
   * @return true if expired
   */
  private Boolean isTokenExpired(String token) {
    final Date expiration = getExpirationDateFromToken(token);
    return expiration.before(new Date());
  }

  /**
   * Validate token.
   *
   * @param token JWT token
   * @param userDetails User details
   * @return true if valid
   */
  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = getUsernameFromToken(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  /**
   * Validate token without user details (for refresh tokens).
   *
   * @param token JWT token
   * @return true if valid
   */
  public Boolean validateToken(String token) {
    try {
      return !isTokenExpired(token);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Check if token is a refresh token.
   *
   * @param token JWT token
   * @return true if refresh token
   */
  public Boolean isRefreshToken(String token) {
    try {
      Claims claims = getAllClaimsFromToken(token);
      return "refresh".equals(claims.get("type"));
    } catch (Exception e) {
      return false;
    }
  }
}
