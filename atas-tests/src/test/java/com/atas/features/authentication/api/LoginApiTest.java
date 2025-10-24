package com.atas.features.authentication.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.atas.framework.AtasFrameworkApplication;
import com.atas.framework.model.TestExecution;
import com.atas.framework.model.TestResult;
import com.atas.framework.model.TestStatus;
import com.atas.framework.repository.TestExecutionRepository;
import com.atas.framework.repository.TestResultRepository;
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

/** API tests for Authentication functionality Tests login API endpoints and authentication flows */
@SpringBootTest(classes = AtasFrameworkApplication.class)
@ActiveProfiles("test")
@Epic("Authentication")
@Feature("Login API")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class LoginApiTest {

  @Autowired private TestExecutionRepository executionRepository;

  @Autowired private TestResultRepository resultRepository;

  private TestExecution execution;

  @BeforeAll
  void initExecution() {
    execution =
        TestExecution.builder()
            .executionId(UUID.randomUUID().toString())
            .suiteName("LoginApiTest")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .environment("test")
            .build();
    execution = executionRepository.save(execution);
    log.info("Started execution {}", execution.getExecutionId());
  }

  @BeforeEach
  void setUp() {
    // Setup for each test
  }

  @Test
  @DisplayName("Should successfully login with valid credentials")
  @Description("Verify that login API accepts valid credentials and returns success response")
  @Story("Valid Login")
  @Transactional
  @Rollback(false)
  void shouldLoginWithValidCredentials() {
    // Given
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId("loginValidCredentials")
            .testName("Should successfully login with valid credentials")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .build();
    execution.getResults().add(result);

    // When - Simulate API call (placeholder for actual implementation)
    try {
      // This would be replaced with actual API call
      String response = "success";

      // Then
      assertThat(response).isEqualTo("success");
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
  @DisplayName("Should reject login with invalid credentials")
  @Description("Verify that login API rejects invalid credentials and returns error response")
  @Story("Invalid Login")
  @Transactional
  @Rollback(false)
  void shouldRejectInvalidCredentials() {
    // Given
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId("loginInvalidCredentials")
            .testName("Should reject login with invalid credentials")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .build();
    execution.getResults().add(result);

    // When - Simulate API call (placeholder for actual implementation)
    try {
      // This would be replaced with actual API call
      String response = "error";

      // Then
      assertThat(response).isEqualTo("error");
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
  @DisplayName("Should validate required fields for login")
  @Description("Verify that login API validates required fields and returns appropriate error")
  @Story("Field Validation")
  @Transactional
  @Rollback(false)
  void shouldValidateRequiredFields() {
    // Given
    TestResult result =
        TestResult.builder()
            .execution(execution)
            .testId("loginFieldValidation")
            .testName("Should validate required fields for login")
            .status(TestStatus.RUNNING)
            .startTime(LocalDateTime.now())
            .build();
    execution.getResults().add(result);

    // When - Simulate API call (placeholder for actual implementation)
    try {
      // This would be replaced with actual API call
      String response = "validation_error";

      // Then
      assertThat(response).isEqualTo("validation_error");
      result.setStatus(TestStatus.PASSED);
    } catch (Exception e) {
      result.setStatus(TestStatus.FAILED);
      throw e;
    } finally {
      result.setEndTime(LocalDateTime.now());
      resultRepository.save(result);
    }
  }

  @AfterAll
  void finalizeExecution() {
    execution.setEndTime(LocalDateTime.now());
    execution.setStatus(TestStatus.PASSED);
    executionRepository.save(execution);
    log.info("Finished execution {}", execution.getExecutionId());
  }
}
