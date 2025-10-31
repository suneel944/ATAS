package com.atas.framework.monitoring;

import com.atas.framework.repository.*;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service for monitoring database health, performance metrics, and real-time operations. Provides
 * comprehensive database monitoring including connection status, table statistics, and real-time
 * CRUD operation tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthService {

  private final DataSource dataSource;
  private final TestExecutionRepository executionRepository;
  private final TestResultRepository resultRepository;
  private final TestStepRepository stepRepository;
  private final TestAttachmentRepository attachmentRepository;
  private final TestMetricRepository metricRepository;

  // Real-time operation counters
  private final AtomicLong insertCount = new AtomicLong(0);
  private final AtomicLong updateCount = new AtomicLong(0);
  private final AtomicLong deleteCount = new AtomicLong(0);
  private final AtomicLong selectCount = new AtomicLong(0);

  // Performance tracking
  private final List<QueryPerformance> recentQueries =
      Collections.synchronizedList(new ArrayList<>());
  private final AtomicLong totalQueryTime = new AtomicLong(0);
  private final AtomicLong totalQueryCount = new AtomicLong(0);

  // SSE emitters for real-time updates
  private final Map<String, List<SseEmitter>> dbEmitterMap = new ConcurrentHashMap<>();

  // Database health cache (for future use)
  @SuppressWarnings("unused")
  private volatile DatabaseHealthDto cachedHealth;

  @SuppressWarnings("unused")
  private volatile LocalDateTime lastHealthCheck = LocalDateTime.now();

  /** Get comprehensive database health information */
  public DatabaseHealthDto getDatabaseHealth() {
    try {
      DatabaseHealthDto health = new DatabaseHealthDto();

      // Connection health
      health.setConnectionStatus(checkConnectionHealth());
      health.setConnectionPoolStatus(getConnectionPoolStatus());

      // Database metadata
      health.setDatabaseInfo(getDatabaseInfo());

      // Table statistics
      health.setTableStatistics(getTableStatistics());

      // Performance metrics
      health.setPerformanceMetrics(getPerformanceMetrics());

      // Recent operations
      health.setRecentOperations(getRecentOperations());

      health.setLastChecked(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

      cachedHealth = health;
      lastHealthCheck = LocalDateTime.now();

      return health;
    } catch (Exception e) {
      log.error("Error getting database health", e);
      return createErrorHealthResponse(e.getMessage());
    }
  }

  /** Get real-time database operations summary */
  public DatabaseOperationsDto getDatabaseOperations() {
    return DatabaseOperationsDto.builder()
        .totalInserts(insertCount.get())
        .totalUpdates(updateCount.get())
        .totalDeletes(deleteCount.get())
        .totalSelects(selectCount.get())
        .totalOperations(
            insertCount.get() + updateCount.get() + deleteCount.get() + selectCount.get())
        .lastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .build();
  }

  /** Register SSE emitter for real-time database updates */
  public SseEmitter registerDatabaseEmitter(String clientId) {
    SseEmitter emitter = new SseEmitter(0L);
    emitter.onCompletion(() -> removeDatabaseEmitter(clientId, emitter));
    emitter.onTimeout(() -> removeDatabaseEmitter(clientId, emitter));
    dbEmitterMap.computeIfAbsent(clientId, id -> new ArrayList<>()).add(emitter);
    return emitter;
  }

  /** Track database operation (called by interceptors or manually) */
  public void trackOperation(String operation) {
    switch (operation.toUpperCase()) {
      case "INSERT" -> insertCount.incrementAndGet();
      case "UPDATE" -> updateCount.incrementAndGet();
      case "DELETE" -> deleteCount.incrementAndGet();
      case "SELECT" -> selectCount.incrementAndGet();
    }
  }

  /** Track query performance */
  public void trackQueryPerformance(String query, long durationMs) {
    totalQueryTime.addAndGet(durationMs);
    totalQueryCount.incrementAndGet();

    QueryPerformance perf = new QueryPerformance(query, durationMs, LocalDateTime.now());
    recentQueries.add(perf);

    // Keep only last 100 queries
    if (recentQueries.size() > 100) {
      recentQueries.remove(0);
    }
  }

  /** Scheduled task to broadcast database health updates */
  @Scheduled(fixedDelay = 5000) // Every 5 seconds
  public void broadcastDatabaseUpdates() {
    if (dbEmitterMap.isEmpty()) {
      return;
    }

    try {
      DatabaseHealthDto health = getDatabaseHealth();
      DatabaseOperationsDto operations = getDatabaseOperations();

      for (Map.Entry<String, List<SseEmitter>> entry : dbEmitterMap.entrySet()) {
        String clientId = entry.getKey();
        for (Iterator<SseEmitter> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
          SseEmitter emitter = iterator.next();
          try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "database_update");
            update.put("health", health);
            update.put("operations", operations);
            update.put(
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            emitter.send(update);
          } catch (Exception e) {
            log.warn("Removing dead SSE emitter for database client {}", clientId, e);
            iterator.remove();
          }
        }
      }
    } catch (Exception e) {
      log.error("Error broadcasting database updates", e);
    }
  }

  private void removeDatabaseEmitter(String clientId, SseEmitter emitter) {
    List<SseEmitter> emitters = dbEmitterMap.get(clientId);
    if (emitters != null) {
      emitters.remove(emitter);
    }
  }

  private String checkConnectionHealth() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(5) ? "HEALTHY" : "UNHEALTHY";
    } catch (SQLException e) {
      log.error("Database connection health check failed", e);
      return "ERROR";
    }
  }

  private ConnectionPoolStatusDto getConnectionPoolStatus() {
    try {
      if (dataSource instanceof HikariDataSource hikariDataSource) {
        HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
        return ConnectionPoolStatusDto.builder()
            .activeConnections(poolBean.getActiveConnections())
            .idleConnections(poolBean.getIdleConnections())
            .maxConnections(hikariDataSource.getMaximumPoolSize())
            .totalConnections(poolBean.getTotalConnections())
            .threadsAwaitingConnection(poolBean.getThreadsAwaitingConnection())
            .status("ACTIVE")
            .build();
      } else {
        // Fallback for non-HikariCP data sources
        try (Connection connection = dataSource.getConnection()) {
          return ConnectionPoolStatusDto.builder()
              .activeConnections(1)
              .idleConnections(0)
              .maxConnections(10)
              .status("ACTIVE")
              .build();
        }
      }
    } catch (Exception e) {
      log.error("Error getting connection pool status", e);
      return ConnectionPoolStatusDto.builder().status("ERROR").errorMessage(e.getMessage()).build();
    }
  }

  private DatabaseInfoDto getDatabaseInfo() {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      return DatabaseInfoDto.builder()
          .databaseProductName(metaData.getDatabaseProductName())
          .databaseProductVersion(metaData.getDatabaseProductVersion())
          .driverName(metaData.getDriverName())
          .driverVersion(metaData.getDriverVersion())
          .url(metaData.getURL())
          .username(metaData.getUserName())
          .build();
    } catch (SQLException e) {
      log.error("Error getting database info", e);
      return DatabaseInfoDto.builder()
          .databaseProductName("Unknown")
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private List<TableStatisticsDto> getTableStatistics() {
    List<TableStatisticsDto> stats = new ArrayList<>();

    // Get statistics for each entity table
    stats.add(getTableStats("test_executions", executionRepository));
    stats.add(getTableStats("test_results", resultRepository));
    stats.add(getTableStats("test_steps", stepRepository));
    stats.add(getTableStats("test_attachments", attachmentRepository));
    stats.add(getTableStats("test_metrics", metricRepository));

    return stats;
  }

  private TableStatisticsDto getTableStats(String tableName, JpaRepository<?, Long> repository) {
    try {
      long count = repository.count();
      return TableStatisticsDto.builder()
          .tableName(tableName)
          .recordCount(count)
          .status("ACTIVE")
          .lastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .build();
    } catch (Exception e) {
      return TableStatisticsDto.builder()
          .tableName(tableName)
          .status("ERROR")
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private PerformanceMetricsDto getPerformanceMetrics() {
    try {
      // Calculate real average query time
      long totalTime = totalQueryTime.get();
      long totalQueries = totalQueryCount.get();
      double avgQueryTime = totalQueries > 0 ? (double) totalTime / totalQueries : 0.0;

      // Count slow queries (> 100ms)
      long slowQueries =
          recentQueries.stream()
              .mapToLong(q -> q.durationMs)
              .filter(duration -> duration > 100)
              .count();

      // Calculate connection pool utilization
      double poolUtilization = 0.0;
      try {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
          HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
          int active = poolBean.getActiveConnections();
          int max = hikariDataSource.getMaximumPoolSize();
          poolUtilization = max > 0 ? (double) active / max * 100 : 0.0;
        }
      } catch (Exception e) {
        log.warn("Could not get connection pool utilization", e);
      }

      // Get cache hit ratio from database (PostgreSQL specific)
      double cacheHitRatio = getCacheHitRatio();

      return PerformanceMetricsDto.builder()
          .averageQueryTime(String.format("%.2fms", avgQueryTime))
          .slowQueries((int) slowQueries)
          .connectionPoolUtilization(poolUtilization)
          .cacheHitRatio(cacheHitRatio)
          .lastOptimized(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .build();
    } catch (Exception e) {
      log.error("Error getting performance metrics", e);
      return PerformanceMetricsDto.builder()
          .averageQueryTime("N/A")
          .slowQueries(0)
          .connectionPoolUtilization(0.0)
          .cacheHitRatio(0.0)
          .lastOptimized(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .build();
    }
  }

  private List<RecentOperationDto> getRecentOperations() {
    List<RecentOperationDto> operations = new ArrayList<>();

    // Get recent query performance data
    synchronized (recentQueries) {
      recentQueries.stream()
          .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
          .limit(10)
          .forEach(
              query -> {
                String operation = extractOperationType(query.query);
                String table = extractTableName(query.query);

                operations.add(
                    RecentOperationDto.builder()
                        .operation(operation)
                        .table(table)
                        .timestamp(query.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .duration(query.durationMs + "ms")
                        .status("SUCCESS")
                        .build());
              });
    }

    return operations;
  }

  private DatabaseHealthDto createErrorHealthResponse(String errorMessage) {
    return DatabaseHealthDto.builder()
        .connectionStatus("ERROR")
        .errorMessage(errorMessage)
        .lastChecked(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .build();
  }

  private double getCacheHitRatio() {
    try (Connection connection = dataSource.getConnection()) {
      // PostgreSQL specific query for cache hit ratio
      String query =
          "SELECT "
              + "round(100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)), 2) as cache_hit_ratio "
              + "FROM pg_stat_database WHERE datname = current_database()";

      try (PreparedStatement stmt = connection.prepareStatement(query);
          ResultSet rs = stmt.executeQuery()) {

        if (rs.next()) {
          return rs.getDouble("cache_hit_ratio");
        }
      }
    } catch (SQLException e) {
      log.warn("Could not get cache hit ratio from database", e);
    }
    return 0.0;
  }

  private String extractOperationType(String query) {
    if (query == null) return "UNKNOWN";
    String upperQuery = query.trim().toUpperCase();
    if (upperQuery.startsWith("SELECT")) return "SELECT";
    if (upperQuery.startsWith("INSERT")) return "INSERT";
    if (upperQuery.startsWith("UPDATE")) return "UPDATE";
    if (upperQuery.startsWith("DELETE")) return "DELETE";
    return "OTHER";
  }

  private String extractTableName(String query) {
    if (query == null) return "unknown";
    String upperQuery = query.trim().toUpperCase();

    // Simple table name extraction
    if (upperQuery.contains("FROM ")) {
      String[] parts = upperQuery.split("FROM ");
      if (parts.length > 1) {
        String tablePart = parts[1].split("\\s+")[0];
        return tablePart.replaceAll("[^a-zA-Z0-9_]", "");
      }
    }
    if (upperQuery.contains("INTO ")) {
      String[] parts = upperQuery.split("INTO ");
      if (parts.length > 1) {
        String tablePart = parts[1].split("\\s+")[0];
        return tablePart.replaceAll("[^a-zA-Z0-9_]", "");
      }
    }
    if (upperQuery.contains("UPDATE ")) {
      String[] parts = upperQuery.split("UPDATE ");
      if (parts.length > 1) {
        String tablePart = parts[1].split("\\s+")[0];
        return tablePart.replaceAll("[^a-zA-Z0-9_]", "");
      }
    }
    return "unknown";
  }

  // DTOs for database health monitoring
  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class DatabaseHealthDto {
    private String connectionStatus;
    private ConnectionPoolStatusDto connectionPoolStatus;
    private DatabaseInfoDto databaseInfo;
    private List<TableStatisticsDto> tableStatistics;
    private PerformanceMetricsDto performanceMetrics;
    private List<RecentOperationDto> recentOperations;
    private String lastChecked;
    private String errorMessage;
  }

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class ConnectionPoolStatusDto {
    private int activeConnections;
    private int idleConnections;
    private int maxConnections;
    private int totalConnections;
    private int threadsAwaitingConnection;
    private String status;
    private String errorMessage;
  }

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class DatabaseInfoDto {
    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    private String url;
    private String username;
    private String errorMessage;
  }

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class TableStatisticsDto {
    private String tableName;
    private long recordCount;
    private String status;
    private String lastUpdated;
    private String errorMessage;
  }

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class PerformanceMetricsDto {
    private String averageQueryTime;
    private int slowQueries;
    private double connectionPoolUtilization;
    private double cacheHitRatio;
    private String lastOptimized;
  }

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class RecentOperationDto {
    private String operation;
    private String table;
    private String timestamp;
    private String duration;
    private String status;
  }

  @lombok.Data
  @lombok.Builder
  @lombok.AllArgsConstructor
  @lombok.NoArgsConstructor
  public static class DatabaseOperationsDto {
    private long totalInserts;
    private long totalUpdates;
    private long totalDeletes;
    private long totalSelects;
    private long totalOperations;
    private String lastUpdated;
  }

  // Internal class for tracking query performance
  private static class QueryPerformance {
    final String query;
    final long durationMs;
    final LocalDateTime timestamp;

    QueryPerformance(String query, long durationMs, LocalDateTime timestamp) {
      this.query = query;
      this.durationMs = durationMs;
      this.timestamp = timestamp;
    }
  }
}
