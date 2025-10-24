package com.atas.features.authentication.ui;

import com.atas.framework.AtasFrameworkApplication;
import com.atas.framework.core.driver.BrowserType;
import com.atas.framework.core.driver.DriverConfig;
import com.atas.framework.core.playwright.PlaywrightService;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.recording.VideoRecordingService;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
import com.atas.features.authentication.pages.LoginPage;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example UI test demonstrating how to use the ATAS framework.  It
 * leverages Spring Boot's test support to wire up services and
 * persists results and recordings to the database.  This test
 * navigates to a dummy login page, enters invalid credentials and
 * verifies that the dashboard does not load.
 */
@SpringBootTest(classes = AtasFrameworkApplication.class)
@org.springframework.test.context.ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class LoginUiTest {

    @Autowired
    private PlaywrightService playwrightService;

    @Autowired
    private VideoRecordingService videoRecordingService;

    @Autowired
    private TestExecutionRepository executionRepository;

    @Autowired
    private TestResultRepository resultRepository;

    private TestExecution execution;

    private Page page;
    private TestResult result;

    @BeforeAll
    void initExecution() {
        execution = TestExecution.builder()
                .executionId(UUID.randomUUID().toString())
                .suiteName("LoginUiTest")
                .status(TestStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .environment("test")
                .build();
        execution = executionRepository.save(execution);
        log.info("Started execution {}", execution.getExecutionId());
    }

    @BeforeEach
    void initTest() {
        page = playwrightService.createPage(BrowserType.CHROMIUM,
                DriverConfig.builder()
                        .headless(true)
                        .recordVideo(false)  // Disable video recording for tests to avoid S3 upload issues
                        .videoDir("target/test-videos")
                        .viewportWidth(1280)
                        .viewportHeight(720)
                        .build());
        result = TestResult.builder()
                .execution(execution)
                .testId("loginInvalidCredentials")
                .testName("Login with invalid credentials should fail")
                .status(TestStatus.RUNNING)
                .startTime(LocalDateTime.now())
                .build();
        execution.getResults().add(result);
    }

    @Test
    @Transactional
    @Rollback(false)
    void loginWithInvalidCredentialsShouldNotLoadDashboard() {
        // Given I am on the login page
        LoginPage loginPage = new LoginPage(page);
        loginPage.navigate()
                .enterUsername("invalid@user.com")
                .enterPassword("wrongpass");

        // When I attempt to login
        loginPage.clickLogin();

        // Then the dashboard should not load
        String title = page.title();
        assertThat(title).doesNotContainIgnoringCase("dashboard");
        result.setStatus(TestStatus.PASSED);
    }

    @AfterEach
    void tearDownTest() {
        result.setEndTime(LocalDateTime.now());
        // Close context to flush video; store recording and update attachment
        try {
            // Closing the page's context also closes the page
            page.context().close();
        } catch (Exception e) {
            log.warn("Error closing context", e);
        }
        videoRecordingService.processVideo(page, result);
        resultRepository.save(result);
    }

    @AfterAll
    void finalizeExecution() {
        execution.setEndTime(LocalDateTime.now());
        execution.setStatus(TestStatus.PASSED);
        executionRepository.save(execution);
        // Shutdown Playwright after all tests
        playwrightService.shutdown();
        log.info("Finished execution {}", execution.getExecutionId());
    }
}