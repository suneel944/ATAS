package com.atas.products.automationexercise.pages;

import com.atas.shared.pages.BasePage;
import com.atas.shared.utility.BaseUrlResolver;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class ApiListPage extends BasePage<ApiListPage> {

  private static final String BASE_URL = BaseUrlResolver.resolveService("automationexercise");

  private final Locator pageTitle = page.locator("text=APIs List for practice").first();

  public ApiListPage(Page page) {
    super(page);
  }

  public ApiListPage gotoPage() {
    page.navigate(BASE_URL + "/api_list");
    page.waitForLoadState();
    return this;
  }

  public boolean isPageLoaded() {
    return pageTitle.isVisible();
  }
}
