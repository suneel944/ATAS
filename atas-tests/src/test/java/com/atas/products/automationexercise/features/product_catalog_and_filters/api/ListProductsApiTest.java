package com.atas.products.automationexercise.features.product_catalog_and_filters.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.PRODUCTS)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P0)
public class ListProductsApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify products list API returns 200 OK")
  void listProductsReturns200() {
    FluentApiRequest api = apiForService("automationexercise");
    int status = api.endpoint("/api/productsList").get().expectStatus(200).getStatus();
    assertEquals(200, status, "Products list API should return 200 OK");
  }
}
