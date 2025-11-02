package com.atas.products.automationexercise.features.landing_and_navigation.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;

@Epic("Landing and Navigation")
@Feature("Site Health API")
@Tag(TestTags.API)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P0)
public class SiteHealthApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify home page returns 200 OK")
  @Story("Site health check")
  void testHomePageHealth() {
    APIResponse response = request.get("/");
    assertEquals(200, response.status(), "Home page should return 200 OK status");
  }
}
