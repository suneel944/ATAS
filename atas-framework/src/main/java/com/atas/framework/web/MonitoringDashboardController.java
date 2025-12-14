package com.atas.framework.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Simple controller to serve the monitoring dashboard page. This provides a basic web UI for the
 * monitoring functionality.
 */
@Controller
public class MonitoringDashboardController {

  /**
   * Validates that a redirect URL is safe (internal path only). Prevents open redirect
   * vulnerabilities by ensuring redirects only go to internal application paths.
   *
   * @param redirectUrl The redirect URL to validate
   * @return true if the URL is safe (starts with "/" and doesn't contain protocol schemes)
   */
  private boolean isValidRedirectUrl(String redirectUrl) {
    if (redirectUrl == null || redirectUrl.isEmpty()) {
      return false;
    }
    // Only allow relative paths starting with "/"
    // Reject URLs with protocols (http://, https://, javascript:, etc.)
    // Reject protocol-relative URLs (//example.com)
    return redirectUrl.startsWith("/")
        && !redirectUrl.contains("://")
        && !redirectUrl.startsWith("//")
        && !redirectUrl.toLowerCase().startsWith("/javascript:")
        && !redirectUrl.toLowerCase().startsWith("/data:");
  }

  /**
   * Serves the main monitoring dashboard page.
   *
   * @return the dashboard view name
   */
  @GetMapping("/monitoring/dashboard")
  public String dashboard() {
    return "monitoring-dashboard";
  }

  /**
   * Serves the database management page.
   *
   * @return the database management view name
   */
  @GetMapping("/monitoring/database")
  public String databaseManagement() {
    return "database-management";
  }

  /**
   * Serves the login page. If user is already authenticated, redirects to dashboard or redirect
   * URL.
   *
   * @param redirect Redirect URL after login (from query parameter)
   * @param request HTTP request
   * @return login view name or redirect to dashboard
   */
  @GetMapping("/login")
  public String login(@RequestParam(required = false) String redirect, HttpServletRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // If user is already authenticated, redirect to dashboard or redirect URL
    if (authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getName())) {
      // Validate redirect URL to prevent open redirect attacks
      String redirectUrl = "/monitoring/dashboard"; // Default safe redirect
      if (redirect != null && !redirect.isEmpty() && isValidRedirectUrl(redirect)) {
        redirectUrl = redirect;
      }
      return "redirect:" + redirectUrl;
    }

    return "login";
  }

  /**
   * Root path redirects to login page if not authenticated, or to dashboard if authenticated. If
   * user is already authenticated, redirects to dashboard or redirect URL.
   *
   * @param redirect Redirect URL after login (from query parameter)
   * @return redirect to login or dashboard
   */
  @GetMapping("/")
  public String root(@RequestParam(required = false) String redirect) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // If user is already authenticated, redirect to dashboard or redirect URL
    if (authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getName())) {
      // Validate redirect URL to prevent open redirect attacks
      String redirectUrl = "/monitoring/dashboard"; // Default safe redirect
      if (redirect != null && !redirect.isEmpty() && isValidRedirectUrl(redirect)) {
        redirectUrl = redirect;
      }
      return "redirect:" + redirectUrl;
    }

    // Not authenticated, redirect to login with redirect parameter
    // Only include redirect parameter if it's a valid internal path
    if (redirect != null && !redirect.isEmpty() && isValidRedirectUrl(redirect)) {
      return "redirect:/login?redirect="
          + java.net.URLEncoder.encode(redirect, java.nio.charset.StandardCharsets.UTF_8);
    }
    return "redirect:/login";
  }

  /**
   * Handle favicon requests to prevent NoResourceFoundException errors. Returns 204 No Content to
   * satisfy browser requests without serving a file.
   *
   * @param response HTTP response
   */
  @GetMapping("/favicon.ico")
  public void favicon(jakarta.servlet.http.HttpServletResponse response) {
    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_NO_CONTENT);
  }
}
