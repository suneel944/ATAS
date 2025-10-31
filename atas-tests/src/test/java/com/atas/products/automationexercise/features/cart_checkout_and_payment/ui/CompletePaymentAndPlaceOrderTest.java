package com.atas.products.automationexercise.features.cart_checkout_and_payment.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.atas.products.automationexercise.pages.PaymentPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(TestTags.UI)
@Tag(TestTags.PAYMENT)
@Tag(TestTags.CHECKOUT)
@Tag(TestTags.REGRESSION)
@Tag(TestTags.FAST)
@Tag(TestTags.P1)
public class CompletePaymentAndPlaceOrderTest extends UiTestHooks {

  @Test
  void paymentPageReachable() {
    PaymentPage paymentPage = new PaymentPage(page);
    paymentPage.gotoPage();
    assertTrue(paymentPage.isOnPaymentPage());
  }
}
