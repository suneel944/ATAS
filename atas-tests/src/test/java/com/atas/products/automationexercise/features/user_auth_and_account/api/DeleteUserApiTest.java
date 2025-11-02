package com.atas.products.automationexercise.features.user_auth_and_account.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.AUTH)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class DeleteUserApiTest extends ApiTestHooks {

  @Test
  void deleteEndpointReachable() {
    APIResponse response = request.delete("/api/deleteAccount", RequestOptions.create());
    assertTrue(response.status() >= 200 && response.status() < 500);
  }
}
