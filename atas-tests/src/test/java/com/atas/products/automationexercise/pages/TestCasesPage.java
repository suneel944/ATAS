package com.atas.products.automationexercise.pages;
import com.atas.shared.utility.BaseUrlResolver;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class TestCasesPage extends BasePage<TestCasesPage> {

  private static final String BASE_URL = BaseUrlResolver.resolveService("automationexercise");

  private final Locator pageTitle = page.locator("h2.title");

  public TestCasesPage(Page page) {
    super(page);
  }

  public TestCasesPage gotoPage() {
    page.navigate(BASE_URL + "/test_cases");
    page.waitForLoadState();
    return this;
  }

  public boolean isPageLoaded() {
    return pageTitle.isVisible();
  }
}
