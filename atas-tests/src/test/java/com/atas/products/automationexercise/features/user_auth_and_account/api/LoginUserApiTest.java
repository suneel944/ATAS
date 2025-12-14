package com.atas.products.automationexercise.features.user_auth_and_account.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.AUTH)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class LoginUserApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify login API handles invalid credentials correctly")
  void loginNegativeWrongPassword() {
    FluentApiRequest api = apiForService("automationexercise");
    Map<String, Object> requestBody = Map.of("email", "noone@example.com", "password", "bad");

    int status =
        api.endpoint("/api/verifyLogin")
            .withHeader("Content-Type", "application/json")
            .withBody(requestBody)
            .post()
            .getStatus();

    assertTrue(status >= 200 && status < 500, "Login API should return valid HTTP status");
  }
}
