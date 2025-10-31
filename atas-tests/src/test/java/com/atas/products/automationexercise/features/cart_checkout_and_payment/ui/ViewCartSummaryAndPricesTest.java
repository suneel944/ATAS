package com.atas.products.automationexercise.features.cart_checkout_and_payment.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.CartPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.CART)
@Tag(TestTags.SMOKE)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class ViewCartSummaryAndPricesTest extends UiTestHooks {

  @Test
  void cartSummaryVisible() {
    CartPage cartPage = new CartPage(page);
    cartPage.gotoPage();
    assertTrue(cartPage.isCartInfoVisible());
  }
}
