package com.atas.products.automationexercise.features.user_auth_and_account.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.AUTH)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class RegisterUserApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify register user API endpoint is reachable")
  void registerEndpointReachable() {
    FluentApiRequest api = apiForService("automationexercise");
    int status =
        api.endpoint("/api/createAccount")
            .withHeader("Content-Type", "application/json")
            .post()
            .getStatus();

    assertTrue(status >= 200 && status < 500, "Register API should return valid HTTP status");
  }
}
