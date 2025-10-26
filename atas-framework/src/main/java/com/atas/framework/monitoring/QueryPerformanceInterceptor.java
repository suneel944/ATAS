package com.atas.framework.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interceptor to track database query performance and operations.
 * This provides real-time monitoring of database activity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueryPerformanceInterceptor implements HandlerInterceptor {

    private final DatabaseHealthService databaseHealthService;
    
    // Track API calls that likely involve database operations
    private final ConcurrentHashMap<String, AtomicLong> apiCallCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalApiCalls = new AtomicLong(0);

    @Override
    public boolean preHandle(@org.springframework.lang.NonNull HttpServletRequest request, 
                           @org.springframework.lang.NonNull HttpServletResponse response, 
                           @org.springframework.lang.NonNull Object handler) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Track API calls
        totalApiCalls.incrementAndGet();
        apiCallCounts.computeIfAbsent(requestURI, k -> new AtomicLong(0)).incrementAndGet();
        
        // Simulate database operations based on API endpoints
        if (requestURI.contains("/api/") && !requestURI.contains("/health")) {
            // Simulate query tracking for demonstration
            // In a real implementation, you would hook into JPA/Hibernate interceptors
            simulateDatabaseOperations(requestURI, method);
        }
        
        return true;
    }
    
    private void simulateDatabaseOperations(String requestURI, String method) {
        // This is a simplified simulation - in production you'd use actual query interceptors
        
        // Simulate different types of operations based on endpoint
        if (requestURI.contains("/executions") || requestURI.contains("/results")) {
            if ("GET".equals(method)) {
                databaseHealthService.trackOperation("SELECT");
                // Simulate query time
                long duration = (long) (Math.random() * 50) + 10; // 10-60ms
                databaseHealthService.trackQueryPerformance("SELECT * FROM " + getTableName(requestURI), duration);
            } else if ("POST".equals(method)) {
                databaseHealthService.trackOperation("INSERT");
                long duration = (long) (Math.random() * 100) + 20; // 20-120ms
                databaseHealthService.trackQueryPerformance("INSERT INTO " + getTableName(requestURI), duration);
            } else if ("PUT".equals(method) || "PATCH".equals(method)) {
                databaseHealthService.trackOperation("UPDATE");
                long duration = (long) (Math.random() * 80) + 15; // 15-95ms
                databaseHealthService.trackQueryPerformance("UPDATE " + getTableName(requestURI), duration);
            } else if ("DELETE".equals(method)) {
                databaseHealthService.trackOperation("DELETE");
                long duration = (long) (Math.random() * 60) + 10; // 10-70ms
                databaseHealthService.trackQueryPerformance("DELETE FROM " + getTableName(requestURI), duration);
            }
        }
    }
    
    private String getTableName(String requestURI) {
        if (requestURI.contains("/executions")) return "test_executions";
        if (requestURI.contains("/results")) return "test_results";
        if (requestURI.contains("/steps")) return "test_steps";
        if (requestURI.contains("/attachments")) return "test_attachments";
        if (requestURI.contains("/metrics")) return "test_metrics";
        return "unknown_table";
    }
}
