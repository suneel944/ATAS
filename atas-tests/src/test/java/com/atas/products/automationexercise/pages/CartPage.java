package com.atas.products.automationexercise.pages;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class CartPage extends BasePage<CartPage> {

  private static final String BASE_URL = "https://automationexercise.com";

  private final Locator cartInfoTable = page.locator("#cart_info_table");
  private final Locator cartInfo = page.locator("#cart_info");

  public CartPage(Page page) {
    super(page);
  }

  public CartPage gotoPage() {
    page.navigate(BASE_URL + "/view_cart");
    page.waitForLoadState();
    return this;
  }

  public boolean isCartInfoTableVisible() {
    return cartInfoTable.isVisible();
  }

  public boolean isCartInfoVisible() {
    return cartInfo.isVisible();
  }
}
