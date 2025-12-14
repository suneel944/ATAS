package com.atas.products.automationexercise.features.api_reference.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class StatusCodesAndSchemaValidationTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify products list API returns 200 OK")
  void productsListIs200() {
    FluentApiRequest api = apiForService("automationexercise");
    int status = api.endpoint("/api/productsList").get().getStatus();
    assertEquals(200, status, "Products list API should return 200 OK");
  }
}
