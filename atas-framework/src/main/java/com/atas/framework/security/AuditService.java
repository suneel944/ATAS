package com.atas.framework.security;

import com.atas.framework.model.AuditLog;
import com.atas.framework.model.User;
import com.atas.framework.repository.AuditLogRepository;
import com.atas.framework.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for audit logging. Records authentication events and sensitive operations for security
 * monitoring and compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final UserRepository userRepository;

  /**
   * Log successful authentication.
   *
   * @param username Username
   * @param ipAddress IP address
   * @param userAgent User agent
   */
  @Transactional
  public void logAuthenticationSuccess(String username, String ipAddress, String userAgent) {
    AuditLog auditLog =
        AuditLog.builder()
            .username(username)
            .action("LOGIN_SUCCESS")
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

    auditLogRepository.save(auditLog);
  }

  /**
   * Log failed authentication attempt.
   *
   * @param username Username
   * @param reason Failure reason
   * @param ipAddress IP address
   * @param userAgent User agent
   */
  @Transactional
  public void logAuthenticationFailure(
      String username, String reason, String ipAddress, String userAgent) {
    AuditLog auditLog =
        AuditLog.builder()
            .username(username)
            .action("LOGIN_FAILURE")
            .resourceType("AUTHENTICATION")
            .resourceId(reason)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

    auditLogRepository.save(auditLog);
  }

  /**
   * Log test execution request.
   *
   * @param executionId Test execution ID
   * @param action Action performed (e.g., "TEST_EXECUTE")
   */
  @Transactional
  public void logTestExecution(String executionId, String action) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication != null ? authentication.getName() : "ANONYMOUS";

    HttpServletRequest request = getCurrentRequest();
    String ipAddress = request != null ? getClientIpAddress(request) : "UNKNOWN";
    String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";

    Optional<User> userOpt = userRepository.findByUsername(username);
    User user = userOpt.orElse(null);

    AuditLog auditLog =
        AuditLog.builder()
            .user(user)
            .username(username)
            .action(action)
            .resourceType("TEST_EXECUTION")
            .resourceId(executionId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

    auditLogRepository.save(auditLog);
  }

  /**
   * Log generic action.
   *
   * @param action Action name
   * @param resourceType Resource type
   * @param resourceId Resource ID
   */
  @Transactional
  public void logAction(String action, String resourceType, String resourceId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication != null ? authentication.getName() : "ANONYMOUS";

    HttpServletRequest request = getCurrentRequest();
    String ipAddress = request != null ? getClientIpAddress(request) : "UNKNOWN";
    String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";

    Optional<User> userOpt = userRepository.findByUsername(username);
    User user = userOpt.orElse(null);

    AuditLog auditLog =
        AuditLog.builder()
            .user(user)
            .username(username)
            .action(action)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

    auditLogRepository.save(auditLog);
  }

  private HttpServletRequest getCurrentRequest() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attributes != null ? attributes.getRequest() : null;
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
}
