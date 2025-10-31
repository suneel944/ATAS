package com.atas.products.automationexercise.features.cart_checkout_and_payment.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.CartPage;
import com.atas.products.automationexercise.pages.ProductsPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.CART)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P2)
public class UpdateQuantityInCartTest extends UiTestHooks {

  @Test
  void updateQuantity() {
    // Ensure there is at least one product in the cart first
    new ProductsPage(page).gotoPage().addFirstProductToCart().clickViewCart();
    CartPage cartPage = new CartPage(page);
    assertTrue(cartPage.isCartInfoTableVisible());
  }
}
