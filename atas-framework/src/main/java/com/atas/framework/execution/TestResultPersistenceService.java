package com.atas.framework.execution;

import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for persisting test execution results. Can work both with Spring-managed repositories
 * (when running in Spring context) or with direct JDBC (when running standalone tests).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestResultPersistenceService {

  @Autowired(required = false)
  private TestExecutionRepository executionRepository;

  @Autowired(required = false)
  private TestResultRepository resultRepository;

  // Default is intentionally left as null to trigger smart detection in getConnection()
  @Value("${spring.datasource.url:#{null}}")
  private String datasourceUrl;

  // No default - must be explicitly configured via environment variable or system property
  @Value("${spring.datasource.username:#{null}}")
  private String datasourceUsername;

  // No default - must be explicitly configured via environment variable or system property
  @Value("${spring.datasource.password:#{null}}")
  private String datasourcePassword;

  private boolean useJdbc = false;

  @PostConstruct
  public void init() {
    if (executionRepository == null || resultRepository == null) {
      log.info("Spring repositories not available, will use JDBC directly");
      useJdbc = true;
    } else {
      log.info("Using Spring-managed repositories for test result persistence");
    }
  }

  /**
   * Get or create a test execution record. If ATAS_EXECUTION_ID is set, uses that, otherwise
   * creates a new one.
   */
  private String getProperty(String envKey, String systemKey) {
    if (envKey == null) {
      return systemKey != null ? System.getProperty(systemKey) : null;
    }

    String value = System.getenv(envKey);
    if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value)) {
      value = System.getProperty(envKey);
      if ((value == null || value.isEmpty() || "null".equalsIgnoreCase(value))
          && systemKey != null) {
        value = System.getProperty(systemKey);
      }
    }
    return (value != null && "null".equalsIgnoreCase(value)) ? null : value;
  }

  public String getOrCreateExecutionId(String suiteName, String environment) {
    String executionId = getProperty("ATAS_EXECUTION_ID", null);
    if (executionId == null || executionId.isEmpty()) {
      executionId = UUID.randomUUID().toString();
      log.info(
          "Created new execution ID: {} for suite: {}, environment: {}",
          executionId,
          suiteName,
          environment);
    } else {
      log.info("Using existing execution ID from environment: {}", executionId);
    }

    // Ensure execution record exists in database
    if (useJdbc) {
      ensureExecutionRecordJdbc(executionId, suiteName, environment);
    } else {
      ensureExecutionRecordSpring(executionId, suiteName, environment);
    }

    return executionId;
  }

  private void ensureExecutionRecordSpring(
      String executionId, String suiteName, String environment) {
    Optional<TestExecution> existing = executionRepository.findByExecutionId(executionId);
    if (existing.isEmpty()) {
      TestExecution execution =
          TestExecution.builder()
              .executionId(executionId)
              .suiteName(suiteName)
              .status(TestStatus.RUNNING)
              .startTime(LocalDateTime.now())
              .environment(environment)
              .build();
      executionRepository.save(execution);
      log.info("Created execution record in database: {}", executionId);
    } else {
      log.debug("Execution record already exists: {}", executionId);
    }
  }

  private void ensureExecutionRecordJdbc(String executionId, String suiteName, String environment) {
    try (Connection conn = getConnection();
        PreparedStatement checkStmt =
            conn.prepareStatement("SELECT id FROM test_executions WHERE execution_id = ?")) {
      checkStmt.setString(1, executionId);
      ResultSet rs = checkStmt.executeQuery();

      if (!rs.next()) {
        try (PreparedStatement insertStmt =
            conn.prepareStatement(
                "INSERT INTO test_executions (execution_id, suite_name, status, start_time, environment) VALUES (?, ?, ?, ?, ?)")) {
          insertStmt.setString(1, executionId);
          insertStmt.setString(2, suiteName);
          insertStmt.setString(3, TestStatus.RUNNING.name());
          insertStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
          insertStmt.setString(5, environment);
          insertStmt.executeUpdate();
          log.info("Created execution record: {}", executionId);
        }
      } else {
        log.debug("Execution record already exists: {}", executionId);
      }
    } catch (SQLException e) {
      log.error("Error ensuring execution record: {}", e.getMessage(), e);
    }
  }

  /** Save or update a test result */
  public void saveTestResult(
      String executionId,
      String testId,
      String testName,
      TestStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    if (useJdbc) {
      saveTestResultJdbc(executionId, testId, testName, status, startTime, endTime);
    } else {
      saveTestResultSpring(executionId, testId, testName, status, startTime, endTime);
    }
  }

  private void saveTestResultSpring(
      String executionId,
      String testId,
      String testName,
      TestStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    Optional<TestExecution> executionOpt = executionRepository.findByExecutionId(executionId);
    if (executionOpt.isEmpty()) {
      log.warn("Execution not found: {}, cannot save test result", executionId);
      return;
    }

    TestExecution execution = executionOpt.get();
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId(testId)
            .testName(testName)
            .status(status)
            .startTime(startTime)
            .endTime(endTime != null ? endTime : LocalDateTime.now())
            .build();

    resultRepository.save(result);
    log.debug("Saved test result: {} - {}", testId, status);
  }

  private void saveTestResultJdbc(
      String executionId,
      String testId,
      String testName,
      TestStatus status,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    try (Connection conn = getConnection();
        PreparedStatement getExecutionStmt =
            conn.prepareStatement("SELECT id FROM test_executions WHERE execution_id = ?")) {
      getExecutionStmt.setString(1, executionId);
      ResultSet rs = getExecutionStmt.executeQuery();

      if (!rs.next()) {
        log.warn("Execution not found: {}, cannot save test result", executionId);
        return;
      }

      long executionDbId = rs.getLong("id");

      try (PreparedStatement checkStmt =
          conn.prepareStatement(
              "SELECT id FROM test_results WHERE execution_id = ? AND test_id = ?")) {
        checkStmt.setLong(1, executionDbId);
        checkStmt.setString(2, testId);
        ResultSet resultRs = checkStmt.executeQuery();

        if (resultRs.next()) {
          try (PreparedStatement updateStmt =
              conn.prepareStatement(
                  "UPDATE test_results SET test_name = ?, status = ?, start_time = ?, end_time = ? WHERE execution_id = ? AND test_id = ?")) {
            updateStmt.setString(1, testName);
            updateStmt.setString(2, status.name());
            updateStmt.setTimestamp(3, Timestamp.valueOf(startTime));
            updateStmt.setTimestamp(
                4, Timestamp.valueOf(endTime != null ? endTime : LocalDateTime.now()));
            updateStmt.setLong(5, executionDbId);
            updateStmt.setString(6, testId);
            updateStmt.executeUpdate();
            log.debug("Updated test result: {} - {}", testId, status);
          }
        } else {
          try (PreparedStatement insertStmt =
              conn.prepareStatement(
                  "INSERT INTO test_results (execution_id, test_id, test_name, status, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)")) {
            insertStmt.setLong(1, executionDbId);
            insertStmt.setString(2, testId);
            insertStmt.setString(3, testName);
            insertStmt.setString(4, status.name());
            insertStmt.setTimestamp(5, Timestamp.valueOf(startTime));
            insertStmt.setTimestamp(
                6, Timestamp.valueOf(endTime != null ? endTime : LocalDateTime.now()));
            insertStmt.executeUpdate();
            log.debug("Inserted test result: {} - {}", testId, status);
          }
        }
      }
    } catch (SQLException e) {
      log.error("Error saving test result: {}", e.getMessage(), e);
    }
  }

  /** Update execution status */
  public void updateExecutionStatus(String executionId, TestStatus status) {
    if (useJdbc) {
      updateExecutionStatusJdbc(executionId, status);
    } else {
      updateExecutionStatusSpring(executionId, status);
    }
  }

  private void updateExecutionStatusSpring(String executionId, TestStatus status) {
    Optional<TestExecution> executionOpt = executionRepository.findByExecutionId(executionId);
    if (executionOpt.isPresent()) {
      TestExecution execution = executionOpt.get();
      execution.setStatus(status);
      if (status == TestStatus.PASSED
          || status == TestStatus.FAILED
          || status == TestStatus.ERROR) {
        execution.setEndTime(LocalDateTime.now());
      }
      executionRepository.save(execution);
      log.info("Updated execution status: {} -> {}", executionId, status);
    }
  }

  private void updateExecutionStatusJdbc(String executionId, TestStatus status) {
    try (Connection conn = getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "UPDATE test_executions SET status = ?, end_time = ? WHERE execution_id = ?")) {
      stmt.setString(1, status.name());
      if (status == TestStatus.PASSED
          || status == TestStatus.FAILED
          || status == TestStatus.ERROR) {
        stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
      } else {
        stmt.setTimestamp(2, null);
      }
      stmt.setString(3, executionId);
      int updated = stmt.executeUpdate();
      if (updated > 0) {
        log.info("Updated execution status: {} -> {}", executionId, status);
      } else {
        log.warn("Execution not found for status update: {}", executionId);
      }
    } catch (SQLException e) {
      log.error("Error updating execution status: {}", e.getMessage(), e);
    }
  }

  private Connection getConnection() throws SQLException {
    String url = getProperty("DB_URL", "spring.datasource.url");
    if (url == null && datasourceUrl != null) {
      url = datasourceUrl;
    }

    if (url == null || url.isEmpty()) {
      url = detectDatabaseConnectionUrl();
    }
    if (url == null || url.isEmpty()) {
      throw new SQLException(
          "Could not determine database connection URL. Set DB_URL in .env file or as environment variable.");
    }

    String username = getProperty("DB_USERNAME", "spring.datasource.username");
    if ((username == null || username.isEmpty())
        && datasourceUsername != null
        && !datasourceUsername.isEmpty()) {
      username = datasourceUsername;
    }
    if (username == null || username.isEmpty()) {
      throw new SQLException(
          "Database username is required. Set DB_USERNAME in .env file or as environment variable.");
    }

    String password = getProperty("DB_PASSWORD", "spring.datasource.password");
    if ((password == null || password.isEmpty())
        && datasourcePassword != null
        && !datasourcePassword.isEmpty()) {
      password = datasourcePassword;
    }
    if (password == null || password.isEmpty()) {
      throw new SQLException(
          "Database password is required. Set DB_PASSWORD in .env file or as environment variable.");
    }

    log.debug(
        "Connecting to database: {} as user: {}",
        url.replaceAll("://([^:]+):([^@]+)@", "://***:***@").replaceAll(":([0-9]+)/", ":***/"),
        username);
    try {
      return DriverManager.getConnection(url, username, password);
    } catch (SQLException e) {
      log.error(
          "Failed to connect to database: {}",
          url.replaceAll("://([^:]+):([^@]+)@", "://***:***@"),
          e);
      log.error("Connection error details: {}", e.getMessage());
      if (url.contains("localhost:5433") || url.contains("localhost:5432")) {
        log.error("Troubleshooting tips:");
        log.error("  1. Ensure Docker containers are running: docker ps");
        log.error("  2. For local development, use: make dev (exposes DB on port 5433)");
        log.error(
            "  3. For production containers, set DB_URL explicitly or expose the database port");
        log.error(
            "  4. Check if database is accessible: docker exec -it atas-db-prod psql -U atas -d atasdb");
      }
      throw e;
    }
  }

  /**
   * Detect the appropriate database connection URL based on the running environment. Detection
   * priority: 1. SPRING_PROFILES_ACTIVE environment variable/property 2. Docker container detection
   * (atas-db for dev/stage, atas-db-prod for prod) 3. Port availability (5433 for local Docker
   * Compose, 5432 for standard PostgreSQL)
   *
   * <p>This ensures tests connect to the correct database based on the active environment.
   */
  private String detectDatabaseConnectionUrl() {
    String activeProfile = getProperty("SPRING_PROFILES_ACTIVE", "spring.profiles.active");
    if (activeProfile == null || activeProfile.isEmpty()) {
      activeProfile = "dev";
    }

    log.info("Environment detection - Spring profile: {}", activeProfile);

    boolean devDbRunning = isDockerContainerRunning("atas-db");
    boolean prodDbRunning = isDockerContainerRunning("atas-db-prod");

    log.info(
        "Docker container status - atas-db (dev/stage): {}, atas-db-prod (prod): {}",
        devDbRunning,
        prodDbRunning);

    if ("dev".equalsIgnoreCase(activeProfile) || "stage".equalsIgnoreCase(activeProfile)) {
      if (devDbRunning && isPortOpen("localhost", 5433)) {
        log.info(
            "✅ Matched {} environment: Connecting to local Docker Compose database (atas-db) on port 5433",
            activeProfile);
        return "jdbc:postgresql://localhost:5433/atasdb";
      }

      if (isPortOpen("localhost", 5433)) {
        log.info(
            "✅ Detected database on port 5433 for {} environment (container may not be named atas-db)",
            activeProfile);
        return "jdbc:postgresql://localhost:5433/atasdb";
      }

      if (prodDbRunning && !devDbRunning) {
        log.warn(
            "⚠️  {} profile active but production containers (atas-db-prod) are running instead of dev containers. "
                + "Start dev environment with: make dev",
            activeProfile);
      }
    }

    if ("prod".equalsIgnoreCase(activeProfile)) {
      if (prodDbRunning) {
        if (isPortOpen("localhost", 5433)) {
          log.info(
              "✅ Production Docker container (atas-db-prod) detected with port 5433 exposed. "
                  + "Connecting to production database for local testing.");
          return "jdbc:postgresql://localhost:5433/atasdb";
        }

        log.warn(
            "⚠️  Production Docker container (atas-db-prod) detected but port 5433 is NOT exposed to host. "
                + "Tests running from host machine cannot connect to production database.\n"
                + "Solutions:\n"
                + "  1. Use 'make dev-prod' which exposes port 5433 for local testing\n"
                + "  2. Set DB_URL environment variable: DB_URL=jdbc:postgresql://your-db-host:port/atasdb\n"
                + "  3. Run tests inside Docker container network");

        if (isPortOpen("localhost", 5432)) {
          log.warn(
              "⚠️  Falling back to database on port 5432. "
                  + "Note: This may not be the production database. "
                  + "Ensure you're connecting to the correct database.");
          return "jdbc:postgresql://localhost:5432/atasdb";
        }

        throw new RuntimeException(
            "❌ Production database is not accessible from host machine.\n"
                + "   Production Docker container (atas-db-prod) is running but port is not exposed.\n"
                + "   Solutions:\n"
                + "   1. Use 'make dev-prod' which exposes port 5433 for local testing\n"
                + "   2. Set DB_URL environment variable: DB_URL=jdbc:postgresql://host:port/atasdb\n"
                + "   3. Run tests inside Docker container network");
      }

      if (isPortOpen("localhost", 5433)) {
        log.warn(
            "⚠️  Production profile active but no production containers detected. "
                + "Connecting to available database on port 5433. "
                + "Consider starting production environment with: make dev-prod");
        return "jdbc:postgresql://localhost:5433/atasdb";
      }
    }

    if (isPortOpen("localhost", 5433)) {
      log.info("✅ Detected database on port 5433 (local Docker Compose)");
      return "jdbc:postgresql://localhost:5433/atasdb";
    }

    if (isPortOpen("localhost", 5432)) {
      log.info("✅ Detected database on standard PostgreSQL port 5432");
      return "jdbc:postgresql://localhost:5432/atasdb";
    }

    log.error("❌ Could not detect or connect to any database!");
    log.error("   Current configuration:");
    log.error("   - Spring profile: {}", activeProfile);
    log.error("   - Dev container (atas-db): {}", devDbRunning ? "RUNNING" : "NOT RUNNING");
    log.error("   - Prod container (atas-db-prod): {}", prodDbRunning ? "RUNNING" : "NOT RUNNING");
    log.error("   - Port 5433 open: {}", isPortOpen("localhost", 5433));
    log.error("   - Port 5432 open: {}", isPortOpen("localhost", 5432));
    log.error("   Solutions:");
    log.error("   1. Start development environment: make dev");
    log.error("   2. Set DB_URL environment variable: DB_URL=jdbc:postgresql://host:port/atasdb");
    log.error("   3. Ensure database is running and accessible");

    throw new RuntimeException(
        "Could not detect database connection. Set DB_URL environment variable or ensure database is accessible on port 5433 or 5432.");
  }

  /** Check if a Docker container is running by name */
  private boolean isDockerContainerRunning(String containerName) {
    try {
      ProcessBuilder pb = new ProcessBuilder("docker", "ps", "--format", "{{.Names}}");
      Process process = pb.start();
      try (java.io.BufferedReader reader =
          new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          if (line.trim().equals(containerName)) {
            return true;
          }
        }
      }
      int exitCode = process.waitFor();
      return exitCode == 0;
    } catch (Exception e) {
      log.debug("Could not check Docker container status: {}", e.getMessage());
      return false;
    }
  }

  /** Check if a port is open on localhost */
  private boolean isPortOpen(String host, int port) {
    try (java.net.Socket socket = new java.net.Socket()) {
      socket.connect(new java.net.InetSocketAddress(host, port), 100);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
