package com.atas.products.automationexercise.features.api_reference.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class StatusCodesAndSchemaValidationTest extends ApiTestHooks {

  @Test
  void productsListIs200() {
    APIResponse response = request.get("/api/productsList");
    assertEquals(200, response.status());
  }
}
