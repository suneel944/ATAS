package com.atas.products.automationexercise.features.cart_checkout_and_payment.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.CheckoutPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.CHECKOUT)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class CheckoutWithAddressAndDeliveryDetailsTest extends UiTestHooks {

  @Test
  void checkoutPageReachable() {
    CheckoutPage checkoutPage = new CheckoutPage(page);
    checkoutPage.gotoPage();
    assertTrue(checkoutPage.isOnCheckoutPage());
  }
}
