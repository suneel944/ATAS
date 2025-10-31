package com.atas.products.automationexercise.pages;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Page;

public class PaymentPage extends BasePage<PaymentPage> {

  private static final String BASE_URL = "https://automationexercise.com";

  public PaymentPage(Page page) {
    super(page);
  }

  public PaymentPage gotoPage() {
    page.navigate(BASE_URL + "/payment");
    page.waitForLoadState();
    return this;
  }

  public boolean isOnPaymentPage() {
    return page.url().contains("/payment");
  }
}
