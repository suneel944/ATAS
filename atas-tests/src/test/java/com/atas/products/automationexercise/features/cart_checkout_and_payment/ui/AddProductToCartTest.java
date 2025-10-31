package com.atas.products.automationexercise.features.cart_checkout_and_payment.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.ProductsPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.CART)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class AddProductToCartTest extends UiTestHooks {

  @Test
  void addFirstProductToCart() {
    ProductsPage productsPage = new ProductsPage(page);
    productsPage.gotoPage().addFirstProductToCart();
    // Validate cart modal shows up or cart link is clickable
    assertTrue(productsPage.isViewCartLinkVisible());
  }
}
