package com.atas.framework.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Custom authentication entry point that redirects web requests to login page and returns JSON
 * error for API requests.
 */
@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    // Check if response is already committed (e.g., SSE connections, async requests)
    if (response.isCommitted()) {
      return;
    }

    String requestPath = request.getRequestURI();
    String acceptHeader = request.getHeader("Accept");

    // Check if this is a web page request (HTML) or API request (JSON)
    boolean isWebRequest = acceptHeader != null && acceptHeader.contains("text/html");
    boolean isMonitoringPage = requestPath.startsWith("/monitoring/");
    boolean isRootPath = requestPath.equals("/");
    boolean isStaticResource =
        requestPath.startsWith("/css/")
            || requestPath.startsWith("/js/")
            || requestPath.startsWith("/images/")
            || requestPath.startsWith("/static/")
            || requestPath.startsWith("/webjars/")
            || requestPath.startsWith("/favicon");

    // Redirect web requests (HTML pages) to login
    if (isWebRequest || isMonitoringPage || isRootPath) {
      // Redirect to login page with redirect parameter
      String redirectUrl = "/login?redirect=" + java.net.URLEncoder.encode(requestPath, "UTF-8");
      response.sendRedirect(redirectUrl);
    } else if (isStaticResource) {
      // Static resources should be allowed but if we reach here, return 404
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      // Return JSON error for API requests
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"status\":401}");
    }
  }
}
