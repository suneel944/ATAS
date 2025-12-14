package com.atas.framework.security;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for internal API token generation.
 *
 * <p>Provides an endpoint for test layers to obtain internal API tokens. Uses API key
 * authentication to allow test layers to generate tokens without requiring regular user
 * authentication.
 */
@RestController
@RequestMapping("/api/v1/internal/auth")
@RequiredArgsConstructor
@Slf4j
public class InternalApiAuthController {

  private final InternalApiTokenProvider tokenProvider;

  @Value("${atas.security.internal-api.api-key:internal-api-key-change-this-in-production}")
  private String apiKey;

  /**
   * Generate an internal API token.
   *
   * <p>This endpoint uses API key authentication (via X-API-Key header) to allow test layers to
   * generate internal API tokens. The clientId should be a unique identifier for the test execution
   * or client.
   *
   * @param request Token generation request with client ID
   * @param apiKeyHeader API key from X-API-Key header
   * @return Internal API token
   */
  @PostMapping("/token")
  public ResponseEntity<Map<String, Object>> generateToken(
      @RequestBody TokenRequest request,
      @RequestHeader(value = "X-API-Key", required = false) String apiKeyHeader) {

    if (apiKeyHeader == null || !apiKey.equals(apiKeyHeader)) {
      log.warn("Invalid or missing API key for internal token generation");
      Map<String, Object> error = new HashMap<>();
      error.put("error", "Unauthorized: Invalid or missing API key");
      return ResponseEntity.status(401).body(error);
    }

    String token = tokenProvider.generateToken(request.getClientId());

    Map<String, Object> response = new HashMap<>();
    response.put("token", token);
    response.put("tokenType", "Bearer");
    response.put("expiresIn", 3600); // 1 hour in seconds

    return ResponseEntity.ok(response);
  }

  @Data
  public static class TokenRequest {
    private String clientId;
  }
}
