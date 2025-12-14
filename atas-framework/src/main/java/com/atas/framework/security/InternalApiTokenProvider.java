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
import org.springframework.stereotype.Component;

/**
 * Provider for internal API token generation and validation.
 *
 * <p>Uses a separate secret and token format from public API JWT tokens to ensure internal APIs
 * cannot be accessed with regular user tokens. Internal API tokens are meant for test layer
 * communication with the framework.
 */
@Component
@Slf4j
public class InternalApiTokenProvider {

  @Value(
      "${atas.security.internal-api.secret:internal-api-secret-key-change-this-in-production-minimum-32-characters-long}")
  private String internalApiSecret;

  @Value("${atas.security.internal-api.expiration:3600000}") // 1 hour in milliseconds
  private long internalApiExpirationMs;

  private SecretKey getSigningKey() {
    if (internalApiSecret.length() < 32) {
      log.warn("Internal API secret is too short. Using padded secret for HS256.");
      String paddedSecret = internalApiSecret + "0".repeat(32 - internalApiSecret.length());
      return Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
    }
    return Keys.hmacShaKeyFor(internalApiSecret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Generate internal API token.
   *
   * @param clientId Client identifier (e.g., test execution ID)
   * @return Internal API token string
   */
  public String generateToken(String clientId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + internalApiExpirationMs);

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "internal-api");
    claims.put("clientId", clientId);

    return Jwts.builder()
        .claims(claims)
        .subject("internal-api-client")
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Validate internal API token.
   *
   * @param token Internal API token
   * @return true if valid
   */
  public Boolean validateToken(String token) {
    try {
      Claims claims = getAllClaimsFromToken(token);
      String type = claims.get("type", String.class);
      return "internal-api".equals(type) && !isTokenExpired(token);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Extract client ID from token.
   *
   * @param token Internal API token
   * @return Client ID
   */
  public String getClientIdFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    return claims.get("clientId", String.class);
  }

  private Claims getAllClaimsFromToken(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }

  private Boolean isTokenExpired(String token) {
    Date expiration = getClaimFromToken(token, Claims::getExpiration);
    return expiration.before(new Date());
  }

  private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);
    return claimsResolver.apply(claims);
  }
}
