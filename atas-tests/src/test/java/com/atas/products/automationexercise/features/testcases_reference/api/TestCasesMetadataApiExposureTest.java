package com.atas.products.automationexercise.features.testcases_reference.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;

@Tag(TestTags.API)
@Tag(TestTags.NAVIGATION)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class TestCasesMetadataApiExposureTest {

  private Playwright playwright;
  private APIRequestContext request;

  @BeforeEach
  void setUp() {
    playwright = Playwright.create();
    request =
        playwright
            .request()
            .newContext(
                new APIRequest.NewContextOptions().setBaseURL("https://automationexercise.com"));
  }

  @AfterEach
  void tearDown() {
    if (request != null) request.dispose();
    if (playwright != null) playwright.close();
  }

  @Test
  void canFetchTestCasesPageAsHealth() {
    APIResponse response = request.get("/test_cases");
    assertEquals(200, response.status());
  }
}
