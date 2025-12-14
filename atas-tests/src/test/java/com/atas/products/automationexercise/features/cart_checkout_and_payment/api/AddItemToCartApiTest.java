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
public class AddItemToCartApiTest extends ApiTestHooks {

  @Test
  @DisplayName("Verify add item to cart API endpoint is reachable")
  void addItemEndpointReachable() {
    FluentApiRequest api = apiForService("automationexercise");
    int status =
        api.endpoint("/api/addToCart")
            .withHeader("Content-Type", "application/json")
            .post()
            .getStatus();

    assertTrue(status >= 200 && status < 500, "Add to cart API should return valid HTTP status");
  }
}
