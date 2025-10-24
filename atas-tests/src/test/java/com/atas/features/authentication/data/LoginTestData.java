package com.atas.features.authentication.data;

import lombok.Builder;
import lombok.Data;

/**
 * Test data class for authentication feature tests. Contains various user credentials and test
 * scenarios.
 */
@Data
@Builder
public class LoginTestData {

  private String username;
  private String password;
  private String expectedResult;
  private String testDescription;

  // Predefined test data scenarios
  public static LoginTestData validCredentials() {
    return LoginTestData.builder()
        .username("admin@test.com")
        .password("password123")
        .expectedResult("success")
        .testDescription("Valid user credentials should login successfully")
        .build();
  }

  public static LoginTestData invalidCredentials() {
    return LoginTestData.builder()
        .username("invalid@user.com")
        .password("wrongpass")
        .expectedResult("failure")
        .testDescription("Invalid credentials should fail login")
        .build();
  }

  public static LoginTestData emptyCredentials() {
    return LoginTestData.builder()
        .username("")
        .password("")
        .expectedResult("failure")
        .testDescription("Empty credentials should fail login")
        .build();
  }

  public static LoginTestData invalidEmailFormat() {
    return LoginTestData.builder()
        .username("invalid-email")
        .password("password123")
        .expectedResult("failure")
        .testDescription("Invalid email format should fail login")
        .build();
  }
}
