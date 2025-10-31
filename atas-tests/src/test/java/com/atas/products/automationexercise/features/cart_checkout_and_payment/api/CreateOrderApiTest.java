package com.atas.products.automationexercise.features.cart_checkout_and_payment.api;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.API)
@Tag(TestTags.CHECKOUT)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class CreateOrderApiTest extends ApiTestHooks {

  @Test
  void createOrderEndpointReachable() {
    APIResponse response = request.post("/api/createOrder", RequestOptions.create());
    assertTrue(response.status() >= 200 && response.status() < 500);
  }
}
