package com.atas.framework.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to track database query performance and operations. This provides real-time
 * monitoring of database activity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueryPerformanceInterceptor implements HandlerInterceptor {

  private final DatabaseHealthService databaseHealthService;

  private final ConcurrentHashMap<String, AtomicLong> apiCallCounts = new ConcurrentHashMap<>();
  private final AtomicLong totalApiCalls = new AtomicLong(0);

  @Override
  public boolean preHandle(
      @org.springframework.lang.NonNull HttpServletRequest request,
      @org.springframework.lang.NonNull HttpServletResponse response,
      @org.springframework.lang.NonNull Object handler) {
    String requestURI = request.getRequestURI();
    String method = request.getMethod();

    totalApiCalls.incrementAndGet();
    apiCallCounts.computeIfAbsent(requestURI, k -> new AtomicLong(0)).incrementAndGet();

    if (requestURI.contains("/api/") && !requestURI.contains("/health")) {
      trackDatabaseOperations(requestURI, method);
    }

    return true;
  }

  private void trackDatabaseOperations(String requestURI, String method) {
    if (requestURI.contains("/executions")
        || requestURI.contains("/results")
        || requestURI.contains("/steps")
        || requestURI.contains("/attachments")
        || requestURI.contains("/metrics")) {
      if ("GET".equals(method)) {
        databaseHealthService.trackOperation("SELECT");
      } else if ("POST".equals(method)) {
        databaseHealthService.trackOperation("INSERT");
      } else if ("PUT".equals(method) || "PATCH".equals(method)) {
        databaseHealthService.trackOperation("UPDATE");
      } else if ("DELETE".equals(method)) {
        databaseHealthService.trackOperation("DELETE");
      }
    }
  }
}
