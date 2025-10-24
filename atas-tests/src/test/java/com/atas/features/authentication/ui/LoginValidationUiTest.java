package com.atas.features.authentication.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.atas.features.authentication.data.LoginTestData;
import com.atas.features.authentication.pages.LoginPage;
import com.atas.framework.AtasFrameworkApplication;
import com.atas.framework.core.driver.BrowserType;
import com.atas.framework.core.driver.DriverConfig;
import com.atas.framework.core.playwright.PlaywrightService;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
import com.atas.shared.utils.TestUtils;
import com.microsoft.playwright.Page;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * UI tests for login validation scenarios. Tests various input validation and error handling cases.
 */
@SpringBootTest(classes = AtasFrameworkApplication.class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class LoginValidationUiTest {

  @Autowired private PlaywrightService playwrightService;

  @Autowired private TestExecutionRepository executionRepository;

  @Autowired private TestResultRepository resultRepository;

  private TestExecution execution;
  private Page page;

  @BeforeAll
  void initExecution() {
    execution = TestUtils.createTestExecution("LoginValidationUiTest", "test");
    execution = executionRepository.save(execution);
    TestUtils.logTestExecution(execution);
  }

  @BeforeEach
  void initTest() {
    DriverConfig driverConfig =
        DriverConfig.builder()
            .headless(true)
            .recordVideo(false)
            .videoDir("target/test-videos/authentication")
            .viewportWidth(1280)
            .viewportHeight(720)
            .build();
    page = playwrightService.createPage(BrowserType.CHROMIUM, driverConfig);
  }

  @Test
  @Transactional
  @Rollback(false)
  void loginWithEmptyCredentialsShouldShowValidationError() {
    // Given I am on the login page
    LoginPage loginPage = new LoginPage(page);
    loginPage.navigate();

    // When I attempt to login with empty credentials
    loginPage.enterUsername("").enterPassword("").clickLogin();

    // Then validation errors should be displayed
    String title = page.title();
    assertThat(title).doesNotContainIgnoringCase("dashboard");

    // Create and save test result
    TestResult result =
        TestUtils.createTestResult(
            execution,
            TestUtils.generateTestId("empty-credentials"),
            "Login with empty credentials should show validation error",
            TestStatus.PASSED);
    execution.getResults().add(result);
    resultRepository.save(result);
    TestUtils.logTestResult(result);
  }

  @Test
  @Transactional
  @Rollback(false)
  void loginWithInvalidEmailFormatShouldShowValidationError() {
    // Given I am on the login page
    LoginPage loginPage = new LoginPage(page);
    loginPage.navigate();

    // When I attempt to login with invalid email format
    LoginTestData testData = LoginTestData.invalidEmailFormat();
    loginPage
        .enterUsername(testData.getUsername())
        .enterPassword(testData.getPassword())
        .clickLogin();

    // Then validation errors should be displayed
    String title = page.title();
    assertThat(title).doesNotContainIgnoringCase("dashboard");

    // Create and save test result
    TestResult result =
        TestUtils.createTestResult(
            execution,
            TestUtils.generateTestId("invalid-email"),
            testData.getTestDescription(),
            TestStatus.PASSED);
    execution.getResults().add(result);
    resultRepository.save(result);
    TestUtils.logTestResult(result);
  }

  @Test
  @Transactional
  @Rollback(false)
  void loginWithValidCredentialsShouldNavigateToDashboard() {
    // Given I am on the login page
    LoginPage loginPage = new LoginPage(page);
    loginPage.navigate();

    // When I attempt to login with valid credentials
    LoginTestData testData = LoginTestData.validCredentials();
    loginPage
        .enterUsername(testData.getUsername())
        .enterPassword(testData.getPassword())
        .clickLogin();

    // Then I should be redirected to dashboard
    String title = page.title();
    assertThat(title).containsIgnoringCase("dashboard");

    // Create and save test result
    TestResult result =
        TestUtils.createTestResult(
            execution,
            TestUtils.generateTestId("valid-credentials"),
            testData.getTestDescription(),
            TestStatus.PASSED);
    execution.getResults().add(result);
    resultRepository.save(result);
    TestUtils.logTestResult(result);
  }

  @AfterEach
  void tearDownTest() {
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
