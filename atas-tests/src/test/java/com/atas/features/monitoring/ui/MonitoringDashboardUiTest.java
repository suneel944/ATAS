package com.atas.features.monitoring.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.atas.framework.AtasFrameworkApplication;
import com.atas.framework.core.driver.BrowserType;
import com.atas.framework.core.driver.DriverConfig;
import com.atas.framework.core.playwright.PlaywrightService;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
import com.microsoft.playwright.Page;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** UI tests for Monitoring functionality Tests monitoring dashboard and UI components */
@SpringBootTest(classes = AtasFrameworkApplication.class)
@ActiveProfiles("test")
@Epic("Monitoring")
@Feature("Dashboard UI")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class MonitoringDashboardUiTest {

  @Autowired private PlaywrightService playwrightService;

  @Autowired private TestExecutionRepository executionRepository;

  @Autowired private TestResultRepository resultRepository;

  private TestExecution execution;
  private Page page;

  @BeforeAll
  void initExecution() {
    execution =
        TestExecution.builder()
            .executionId(UUID.randomUUID().toString())
            .suiteName("MonitoringDashboardUiTest")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .environment("test")
            .build();
    execution = executionRepository.save(execution);
    log.info("Started execution {}", execution.getExecutionId());
  }

  @BeforeEach
  void setUp() {
    page =
        playwrightService.createPage(
            BrowserType.CHROMIUM,
            DriverConfig.builder()
                .headless(true)
                .recordVideo(false)
                .videoDir("target/test-videos")
                .viewportWidth(1280)
                .viewportHeight(720)
                .build());
  }

  @Test
  @DisplayName("Should display monitoring dashboard")
  @Description("Verify that monitoring dashboard loads correctly and displays key metrics")
  @Story("Dashboard Display")
  @Transactional
  @Rollback(false)
  void shouldDisplayMonitoringDashboard() {
    // Given
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId("displayMonitoringDashboard")
            .testName("Should display monitoring dashboard")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .build();
    execution.getResults().add(result);

    try {
      // When - Navigate to dashboard (placeholder implementation)
      page.navigate("http://localhost:8080/monitoring/dashboard");

      // Then - Verify dashboard elements
      String title = page.title();
      assertThat(title).isNotEmpty();
      result.setStatus(TestStatus.PASSED);
    } catch (Exception e) {
      result.setStatus(TestStatus.FAILED);
      throw e;
    } finally {
      result.setEndTime(LocalDateTime.now());
      resultRepository.save(result);
    }
  }

  @Test
  @DisplayName("Should show test execution status")
  @Description("Verify that test execution status is displayed on the dashboard")
  @Story("Status Display")
  @Transactional
  @Rollback(false)
  void shouldShowTestExecutionStatus() {
    // Given
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId("showTestExecutionStatus")
            .testName("Should show test execution status")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .build();
    execution.getResults().add(result);

    try {
      // When - Navigate to dashboard
      page.navigate("http://localhost:8080/monitoring/dashboard");

      // Then - Verify status display
      assertThat(page.isVisible("test-status")).isTrue();
      result.setStatus(TestStatus.PASSED);
    } catch (Exception e) {
      result.setStatus(TestStatus.FAILED);
      throw e;
    } finally {
      result.setEndTime(LocalDateTime.now());
      resultRepository.save(result);
    }
  }

  @Test
  @DisplayName("Should display test metrics")
  @Description("Verify that test metrics are displayed correctly on the dashboard")
  @Story("Metrics Display")
  @Transactional
  @Rollback(false)
  void shouldDisplayTestMetrics() {
    // Given
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId("displayTestMetrics")
            .testName("Should display test metrics")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .build();
    execution.getResults().add(result);

    try {
      // When - Navigate to dashboard
      page.navigate("http://localhost:8080/monitoring/dashboard");

      // Then - Verify metrics display
      assertThat(page.isVisible("metrics-container")).isTrue();
      assertThat(page.isVisible("total-tests")).isTrue();
      assertThat(page.isVisible("passed-tests")).isTrue();
      assertThat(page.isVisible("failed-tests")).isTrue();
      result.setStatus(TestStatus.PASSED);
    } catch (Exception e) {
      result.setStatus(TestStatus.FAILED);
      throw e;
    } finally {
      result.setEndTime(LocalDateTime.now());
      resultRepository.save(result);
    }
  }

  @Test
  @DisplayName("Should allow filtering by test status")
  @Description("Verify that users can filter tests by status on the dashboard")
  @Story("Filtering")
  @Transactional
  @Rollback(false)
  void shouldAllowFilteringByTestStatus() {
    // Given
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId("allowFilteringByTestStatus")
            .testName("Should allow filtering by test status")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .build();
    execution.getResults().add(result);

    try {
      // When - Navigate to dashboard and interact with filter
      page.navigate("http://localhost:8080/monitoring/dashboard");
      page.click("status-filter");
      page.selectOption("status-filter", "PASSED");

      // Then - Verify filtering works
      assertThat(page.isVisible("test-row")).isTrue();
      result.setStatus(TestStatus.PASSED);
    } catch (Exception e) {
      result.setStatus(TestStatus.FAILED);
      throw e;
    } finally {
      result.setEndTime(LocalDateTime.now());
      resultRepository.save(result);
    }
  }

  @AfterEach
  void tearDown() {
    try {
      page.context().close();
    } catch (Exception e) {
      log.warn("Error closing context", e);
    }
  }

  @AfterAll
  void finalizeExecution() {
    execution.setEndTime(LocalDateTime.now());
    execution.setStatus(TestStatus.PASSED);
    executionRepository.save(execution);
    playwrightService.shutdown();
    log.info("Finished execution {}", execution.getExecutionId());
  }
}
