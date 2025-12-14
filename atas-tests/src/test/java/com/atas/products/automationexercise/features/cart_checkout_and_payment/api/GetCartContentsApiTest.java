package com.atas.products.automationexercise.features.cart_checkout_and_payment.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.api.FluentApiRequest;
import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.CART)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class GetCartContentsApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify get cart contents API returns 200 OK")
  void getCartEndpointReachable() {
    FluentApiRequest api = apiForService("automationexercise");
    int status = api.endpoint("/api/cart").get().expectStatus(200).getStatus();
    assertEquals(200, status, "Get cart API should return 200 OK");
  }
}
