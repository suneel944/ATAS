package com.atas.framework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentication filter for internal API endpoints.
 *
 * <p>Validates internal API tokens and sets authentication in security context. This filter only
 * processes requests to `/api/v1/internal/**` endpoints.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

  private final InternalApiTokenProvider tokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestPath = request.getRequestURI();

    if (requestPath.startsWith("/api/v1/internal/auth/")) {
      filterChain.doFilter(request, response);
      return;
    }

    if (requestPath.startsWith("/api/v1/internal/")) {
      try {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
          String clientId = tokenProvider.getClientIdFromToken(token);

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  clientId,
                  null,
                  Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL_API")));

          SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
          log.warn("Invalid or missing internal API token for path: {}", requestPath);
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response
              .getWriter()
              .write("{\"error\":\"Unauthorized: Invalid or missing internal API token\"}");
          return;
        }
      } catch (Exception ex) {
        log.error("Could not set internal API authentication in security context", ex);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response
            .getWriter()
            .write("{\"error\":\"Unauthorized: Internal API authentication failed\"}");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private String getTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
