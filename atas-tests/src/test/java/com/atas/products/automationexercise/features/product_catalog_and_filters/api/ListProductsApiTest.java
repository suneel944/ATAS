package com.atas.products.automationexercise.features.product_catalog_and_filters.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.PRODUCTS)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P0)
public class ListProductsApiTest extends ApiTestHooks {

  @Test
  void listProductsReturns200() {
    APIResponse response = request.get("/api/productsList");
    assertEquals(200, response.status());
  }
}
