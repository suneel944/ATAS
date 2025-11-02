package com.atas.framework.execution;

import com.atas.framework.model.TestStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * JUnit 5 extension that captures test execution and persists results to the database. Works both
 * with Spring context and standalone (using JDBC directly).
 */
@Slf4j
public class TestExecutionListener
    implements BeforeAllCallback,
        AfterAllCallback,
        BeforeEachCallback,
        AfterEachCallback,
        TestExecutionExceptionHandler {

  private static final String EXECUTION_ID_KEY = "ATAS_EXECUTION_ID";
  private static final String EXECUTION_START_KEY = "ATAS_EXECUTION_START";
  private static final String TEST_RESULTS_KEY = "ATAS_TEST_RESULTS";
  private static final String RECORDING_ENABLED_KEY = "ATAS_RECORDING_ENABLED";

  private static volatile boolean envFileLoaded = false;

  private static synchronized void ensureEnvFileLoaded() {
    if (envFileLoaded) {
      return;
    }
    try {
      String currentDir = System.getProperty("user.dir");
      String projectDir = currentDir;

      java.io.File envFile = new java.io.File(currentDir, ".env");
      if (!envFile.exists()) {
        java.io.File parentDir = new java.io.File(currentDir).getParentFile();
        if (parentDir != null) {
          envFile = new java.io.File(parentDir, ".env");
          if (envFile.exists()) {
            projectDir = parentDir.getAbsolutePath();
          }
        }
      }

      java.io.File envFileCheck = new java.io.File(projectDir, ".env");
      if (!envFileCheck.exists()) {
        envFileLoaded = true;
        return;
      }

      java.util.Properties props = new java.util.Properties();
      try (java.io.BufferedReader reader =
          new java.io.BufferedReader(new java.io.FileReader(envFileCheck))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty() || line.startsWith("#")) {
            continue;
          }
          int equalsIndex = line.indexOf('=');
          if (equalsIndex > 0) {
            String key = line.substring(0, equalsIndex).trim();
            String value = line.substring(equalsIndex + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
              value = value.substring(1, value.length() - 1);
            }
            props.setProperty(key, value);
          }
        }
      }

      String[] keysToLoad = {
        "DB_URL",
        "DB_USERNAME",
        "DB_PASSWORD",
        "SPRING_PROFILES_ACTIVE",
        "S3_BUCKET",
        "S3_REGION",
        "S3_VIDEO_FOLDER",
        "S3_SCREENSHOT_FOLDER",
        "ATAS_EXECUTION_ID",
        "ATAS_RECORD_LOCAL"
      };

      int loadedCount = 0;
      for (String key : keysToLoad) {
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
          continue;
        }

        String existingProp = System.getProperty(key);
        String existingEnv = System.getenv(key);

        boolean propIsNull =
            existingProp == null || existingProp.isEmpty() || "null".equalsIgnoreCase(existingProp);
        boolean envIsNull =
            existingEnv == null || existingEnv.isEmpty() || "null".equalsIgnoreCase(existingEnv);

        if (propIsNull && envIsNull) {
          System.setProperty(key, value);
          loadedCount++;
        }
      }

      envFileLoaded = true;
      if (loadedCount > 0) {
        log.info("Loaded {} variables from .env file", loadedCount);
      }
    } catch (Exception e) {
      log.warn("Could not load .env file: {}", e.getMessage());
    }
  }

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

  private boolean isRecordingEnabled() {
    ensureEnvFileLoaded();
    String executionId = getProperty("ATAS_EXECUTION_ID", null);
    if (executionId != null && !executionId.isEmpty()) {
      log.info("Recording enabled: ATAS_EXECUTION_ID is set");
      return true;
    }

    String recordLocal = getProperty("ATAS_RECORD_LOCAL", null);
    if (recordLocal != null
        && !recordLocal.isEmpty()
        && !"null".equalsIgnoreCase(recordLocal)
        && ("true".equalsIgnoreCase(recordLocal) || "1".equals(recordLocal))) {
      log.info("Recording enabled: ATAS_RECORD_LOCAL=true");
      return true;
    }

    String recordLocalProp = System.getProperty("atas.record.local");
    if (recordLocalProp != null
        && ("true".equalsIgnoreCase(recordLocalProp) || "1".equals(recordLocalProp))) {
      log.info("Recording enabled: atas.record.local system property={}", recordLocalProp);
      return true;
    }

    return false;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    boolean recordingEnabled = isRecordingEnabled();
    ExtensionContext rootContext = context.getRoot();

    rootContext
        .getStore(ExtensionContext.Namespace.GLOBAL)
        .put(RECORDING_ENABLED_KEY, recordingEnabled);

    if (!recordingEnabled) {
      log.info(
          "Test execution recording is disabled. Set ATAS_EXECUTION_ID (for API runs) or ATAS_RECORD_LOCAL=true to enable recording.");
      return;
    }

    TestExecutionData existingData =
        (TestExecutionData)
            rootContext.getStore(ExtensionContext.Namespace.GLOBAL).get(TEST_RESULTS_KEY);

    if (existingData == null) {
      String executionId = getProperty("ATAS_EXECUTION_ID", null);
      if (executionId == null || executionId.isEmpty()) {
        executionId = UUID.randomUUID().toString();
        log.info("Created new execution ID for local recording: {}", executionId);
      } else {
        log.info("Using execution ID from environment (API-triggered): {}", executionId);
      }

      String suiteName = "maven-test-run";
      String environment = getProperty("SPRING_PROFILES_ACTIVE", "spring.profiles.active");
      if (environment == null || environment.isEmpty()) {
        environment = "dev";
      }

      TestExecutionData data = new TestExecutionData(executionId, suiteName, environment);
      ensureExecutionRecord(executionId, suiteName, environment);

      rootContext.getStore(ExtensionContext.Namespace.GLOBAL).put(EXECUTION_ID_KEY, executionId);
      rootContext
          .getStore(ExtensionContext.Namespace.GLOBAL)
          .put(EXECUTION_START_KEY, LocalDateTime.now());
      rootContext.getStore(ExtensionContext.Namespace.GLOBAL).put(TEST_RESULTS_KEY, data);
      log.info(
          "Test execution recording enabled: {} (suite: {}, environment: {})",
          executionId,
          suiteName,
          environment);
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Boolean recordingEnabled =
        (Boolean)
            context
                .getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .get(RECORDING_ENABLED_KEY);
    if (recordingEnabled == null || !recordingEnabled) {
      return;
    }

    TestExecutionData data =
        (TestExecutionData)
            context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).get(TEST_RESULTS_KEY);
    if (data != null) {
      String testId = generateTestId(context);
      data.testStarts.put(testId, LocalDateTime.now());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    Boolean recordingEnabled =
        (Boolean)
            context
                .getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .get(RECORDING_ENABLED_KEY);
    if (recordingEnabled == null || !recordingEnabled) {
      return;
    }

    TestExecutionData data =
        (TestExecutionData)
            context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).get(TEST_RESULTS_KEY);
    if (data == null) {
      return;
    }

    String testId = generateTestId(context);

    if (data.processedTests.contains(testId)) {
      return;
    }

    String testName = context.getDisplayName();
    LocalDateTime startTime = data.testStarts.get(testId);
    LocalDateTime endTime = LocalDateTime.now();

    boolean isSkipped = false;
    try {
      if (context
          .getRequiredTestClass()
          .isAnnotationPresent(org.junit.jupiter.api.Disabled.class)) {
        isSkipped = true;
      }
      if (context
          .getRequiredTestMethod()
          .isAnnotationPresent(org.junit.jupiter.api.Disabled.class)) {
        isSkipped = true;
      }
    } catch (Exception e) {
    }

    TestStatus status = isSkipped ? TestStatus.SKIPPED : TestStatus.PASSED;

    saveTestResult(
        data.executionId,
        testId,
        testName,
        status,
        startTime != null ? startTime : endTime,
        endTime);

    if (status == TestStatus.FAILED || status == TestStatus.ERROR) {
      data.hasFailures = true;
      updateExecutionStatus(data.executionId, TestStatus.FAILED);
    }

    data.testCount++;
    data.processedTests.add(testId);
  }

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
      throws Throwable {
    Boolean recordingEnabled =
        (Boolean)
            context
                .getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .get(RECORDING_ENABLED_KEY);
    if (recordingEnabled == null || !recordingEnabled) {
      throw throwable;
    }

    TestExecutionData data =
        (TestExecutionData)
            context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).get(TEST_RESULTS_KEY);
    if (data != null) {
      String testId = generateTestId(context);
      data.processedTests.add(testId);

      String testName = context.getDisplayName();
      LocalDateTime startTime = data.testStarts.get(testId);
      LocalDateTime endTime = LocalDateTime.now();

      TestStatus status;
      if (throwable instanceof org.opentest4j.TestAbortedException) {
        status = TestStatus.SKIPPED;
      } else if (throwable instanceof AssertionError) {
        status = TestStatus.FAILED;
      } else {
        status = TestStatus.ERROR;
      }

      saveTestResult(
          data.executionId,
          testId,
          testName,
          status,
          startTime != null ? startTime : endTime,
          endTime);

      if (status == TestStatus.FAILED || status == TestStatus.ERROR) {
        data.hasFailures = true;
        updateExecutionStatus(data.executionId, TestStatus.FAILED);
      }

      data.testCount++;
    }
    throw throwable;
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Boolean recordingEnabled =
        (Boolean)
            context
                .getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .get(RECORDING_ENABLED_KEY);
    if (recordingEnabled == null || !recordingEnabled) {
      return;
    }

    TestExecutionData data =
        (TestExecutionData)
            context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).get(TEST_RESULTS_KEY);
    if (data == null) {
      return;
    }

    if (!data.hasFailures && data.testCount > 0) {
      try (Connection conn = getConnection();
          PreparedStatement checkStmt =
              conn.prepareStatement("SELECT status FROM test_executions WHERE execution_id = ?")) {
        checkStmt.setString(1, data.executionId);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
          String currentStatus = rs.getString("status");
          if (TestStatus.RUNNING.name().equals(currentStatus)) {
            updateExecutionStatus(data.executionId, TestStatus.PASSED);
            log.info("Finalized execution status: {} -> PASSED", data.executionId);
          }
        }
      } catch (SQLException e) {
        log.error("Error checking execution status: {}", e.getMessage(), e);
      }
    }
  }

  private String generateTestId(ExtensionContext context) {
    return context.getRequiredTestClass().getName()
        + "#"
        + context.getRequiredTestMethod().getName();
  }

  private void ensureExecutionRecord(String executionId, String suiteName, String environment) {
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
      }
    } catch (SQLException e) {
      log.error("Error ensuring execution record: {}", e.getMessage(), e);
    }
  }

  private void saveTestResult(
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
            updateStmt.setTimestamp(4, Timestamp.valueOf(endTime));
            updateStmt.setLong(5, executionDbId);
            updateStmt.setString(6, testId);
            updateStmt.executeUpdate();
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
            insertStmt.setTimestamp(6, Timestamp.valueOf(endTime));
            insertStmt.executeUpdate();
          }
        }
      }
    } catch (SQLException e) {
      log.error("Error saving test result: {}", e.getMessage(), e);
    }
  }

  private void updateExecutionStatus(String executionId, TestStatus status) {
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
      }
    } catch (SQLException e) {
      log.error("Error updating execution status: {}", e.getMessage(), e);
    }
  }

  private Connection getConnection() throws SQLException {
    ensureEnvFileLoaded();
    String url = getProperty("DB_URL", "spring.datasource.url");
    if (url == null || url.isEmpty()) {
      url = detectDatabaseConnectionUrl();
      if (url == null || url.isEmpty()) {
        throw new SQLException(
            "Could not determine database connection URL. Set DB_URL in .env file or as environment variable.");
      }
    }

    String username = getProperty("DB_USERNAME", "spring.datasource.username");
    if (username == null || username.isEmpty()) {
      throw new SQLException(
          "Database username is required. Set DB_USERNAME in .env file or as environment variable.");
    }

    String password = getProperty("DB_PASSWORD", "spring.datasource.password");
    if (password == null || password.isEmpty()) {
      throw new SQLException(
          "Database password is required. Set DB_PASSWORD in .env file or as environment variable.");
    }

    try {
      return DriverManager.getConnection(url, username, password);
    } catch (SQLException e) {
      log.error(
          "Failed to connect to database: {}",
          url.replaceAll("://([^:]+):([^@]+)@", "://***:***@"),
          e);
      if (url.contains("localhost:5433") || url.contains("localhost:5432")) {
        log.error("Ensure Docker containers are running: docker ps");
        log.error("For local development, use: make dev (exposes DB on port 5433)");
      }
      throw e;
    }
  }

  private String detectDatabaseConnectionUrl() {
    String activeProfile = getProperty("SPRING_PROFILES_ACTIVE", "spring.profiles.active");
    if (activeProfile == null || activeProfile.isEmpty()) {
      activeProfile = "dev";
    }

    boolean devDbRunning = isDockerContainerRunning("atas-db");
    boolean prodDbRunning = isDockerContainerRunning("atas-db-prod");

    if ("dev".equalsIgnoreCase(activeProfile) || "stage".equalsIgnoreCase(activeProfile)) {
      if (devDbRunning && isPortOpen("localhost", 5433)) {
        return "jdbc:postgresql://localhost:5433/atasdb";
      }

      if (isPortOpen("localhost", 5433)) {
        return "jdbc:postgresql://localhost:5433/atasdb";
      }

      if (prodDbRunning && !devDbRunning) {
        log.warn(
            "{} profile active but production containers are running instead of dev containers. Start dev environment with: make dev",
            activeProfile);
      }
    }

    if ("prod".equalsIgnoreCase(activeProfile)) {
      if (prodDbRunning) {
        if (isPortOpen("localhost", 5433)) {
          return "jdbc:postgresql://localhost:5433/atasdb";
        }

        log.warn(
            "Production Docker container detected but port 5433 is NOT exposed. Use 'make dev-prod' or set DB_URL");

        if (isPortOpen("localhost", 5432)) {
          log.warn("Falling back to database on port 5432");
          return "jdbc:postgresql://localhost:5432/atasdb";
        }

        throw new RuntimeException(
            "Production database is not accessible from host machine. "
                + "Use 'make dev-prod' or set DB_URL environment variable");
      }

      if (isPortOpen("localhost", 5433)) {
        log.warn(
            "Production profile active but no production containers detected. Connecting to database on port 5433");
        return "jdbc:postgresql://localhost:5433/atasdb";
      }
    }

    if (isPortOpen("localhost", 5433)) {
      return "jdbc:postgresql://localhost:5433/atasdb";
    }

    if (isPortOpen("localhost", 5432)) {
      return "jdbc:postgresql://localhost:5432/atasdb";
    }

    log.error(
        "Could not detect or connect to any database. Spring profile: {}, Dev container: {}, Prod container: {}",
        activeProfile,
        devDbRunning ? "RUNNING" : "NOT RUNNING",
        prodDbRunning ? "RUNNING" : "NOT RUNNING");
    log.error(
        "Solutions: 1. Start development environment: make dev 2. Set DB_URL environment variable");

    throw new RuntimeException(
        "Could not detect database connection. Set DB_URL environment variable or ensure database is accessible on port 5433 or 5432.");
  }

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
      return false;
    }
  }

  private boolean isPortOpen(String host, int port) {
    try (java.net.Socket socket = new java.net.Socket()) {
      socket.connect(new java.net.InetSocketAddress(host, port), 100);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static class TestExecutionData {
    final String executionId;

    @SuppressWarnings("unused")
    final String suiteName;

    @SuppressWarnings("unused")
    final String environment;

    final Map<String, LocalDateTime> testStarts = new ConcurrentHashMap<>();
    final java.util.Set<String> processedTests = ConcurrentHashMap.newKeySet();
    volatile boolean hasFailures = false;
    volatile int testCount = 0;

    TestExecutionData(String executionId, String suiteName, String environment) {
      this.executionId = executionId;
      this.suiteName = suiteName;
      this.environment = environment;
    }
  }
}
