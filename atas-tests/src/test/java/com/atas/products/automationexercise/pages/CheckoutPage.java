package com.atas.products.automationexercise.pages;

import com.atas.shared.pages.BasePage;
import com.atas.shared.utility.BaseUrlResolver;
import com.microsoft.playwright.Page;

public class CheckoutPage extends BasePage<CheckoutPage> {

  private static final String BASE_URL = BaseUrlResolver.resolveService("automationexercise");

  public CheckoutPage(Page page) {
    super(page);
  }

  public CheckoutPage gotoPage() {
    page.navigate(BASE_URL + "/checkout");
    page.waitForLoadState();
    return this;
  }

  public boolean isOnCheckoutPage() {
    return page.url().contains("/checkout");
  }
}
