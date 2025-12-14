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
@Tag(TestTags.P2)
public class DeleteUserApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify delete user API endpoint is reachable")
  void deleteEndpointReachable() {
    FluentApiRequest api = apiForService("automationexercise");
    int status =
        api.endpoint("/api/deleteAccount")
            .withHeader("Content-Type", "application/json")
            .delete()
            .getStatus();

    assertTrue(status >= 200 && status < 500, "Delete user API should return valid HTTP status");
  }
}
