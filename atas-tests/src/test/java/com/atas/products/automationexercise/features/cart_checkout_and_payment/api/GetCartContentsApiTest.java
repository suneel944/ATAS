package com.atas.products.automationexercise.features.cart_checkout_and_payment.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.CART)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class GetCartContentsApiTest extends ApiTestHooks {

  @Test
  void getCartEndpointReachable() {
    APIResponse response = request.get("/api/cart");
    assertTrue(response.status() >= 200 && response.status() < 500);
  }
}
