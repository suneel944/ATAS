package com.atas.products.automationexercise.pages;

import com.atas.shared.pages.BasePage;
import com.atas.shared.utility.BaseUrlResolver;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class HomePage extends BasePage<HomePage> {

  private static final String BASE_URL = BaseUrlResolver.resolveService("automationexercise");

  private final Locator homeLink = page.locator("a[href='/' ]");
  private final Locator productsLink = page.locator("a[href='/products']");
  private final Locator cartLink = page.locator("a[href='/view_cart']");
  private final Locator signupLoginLink = page.locator("a[href='/login']");
  private final Locator testCasesLink =
      page.getByRole(
              AriaRole.LINK,
              new com.microsoft.playwright.Page.GetByRoleOptions()
                  .setName("Test Cases")
                  .setExact(true))
          .first();
  private final Locator apiTestingLink = page.locator("a[href='/api_list']");
  private final Locator contactUsLink = page.locator("a[href='/contact_us']");
  private final Locator carouselContainer = page.locator(".carousel-inner").first();

  public HomePage(Page page) {
    super(page);
  }

  public HomePage gotoPage() {
    page.navigate(BASE_URL);
    page.waitForLoadState();
    return this;
  }

  public boolean isCarouselVisible() {
    return carouselContainer.isVisible();
  }

  public boolean areAllNavigationLinksPresent() {
    return homeLink.isVisible()
        && productsLink.isVisible()
        && cartLink.isVisible()
        && signupLoginLink.isVisible()
        && testCasesLink.isVisible()
        && apiTestingLink.isVisible()
        && contactUsLink.isVisible();
  }

  public HomePage navigateToProducts() {
    productsLink.click();
    page.waitForLoadState();
    return this;
  }

  public HomePage navigateToTestCases() {
    testCasesLink.click();
    page.waitForLoadState();
    return this;
  }

  public HomePage navigateToApiList() {
    apiTestingLink.click();
    page.waitForLoadState();
    return this;
  }

  public HomePage navigateToCart() {
    cartLink.click();
    page.waitForLoadState();
    return this;
  }
}
