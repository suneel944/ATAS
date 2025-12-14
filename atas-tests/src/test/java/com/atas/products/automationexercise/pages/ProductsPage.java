package com.atas.products.automationexercise.pages;
import com.atas.shared.utility.BaseUrlResolver;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class ProductsPage extends BasePage<ProductsPage> {

  private static final String BASE_URL = BaseUrlResolver.resolveService("automationexercise");

  private final Locator pageTitle = page.locator("h2.title");
  private final Locator productItems = page.locator(".product-image-wrapper");
  private final Locator firstProductAddToCart =
      page.locator(".product-image-wrapper .add-to-cart").first();
  private final Locator categoryWomen = page.locator("a[href='#Women']");
  private final Locator categoryMen = page.locator("a[href='#Men']");
  private final Locator categoryKids = page.locator("a[href='#Kids']");
  private final Locator brandPolo = page.locator("a[href='/brand_products/Polo']");

  public ProductsPage(Page page) {
    super(page);
  }

  public ProductsPage gotoPage() {
    page.navigate(BASE_URL + "/products");
    page.waitForLoadState();
    return this;
  }

  public boolean isPageLoaded() {
    return pageTitle.isVisible() && pageTitle.textContent().contains("All Products");
  }

  public int getProductCount() {
    return productItems.count();
  }

  public ProductsPage addFirstProductToCart() {
    firstProductAddToCart.click();
    return this;
  }

  public boolean isCategorySidebarVisible() {
    return categoryWomen.isVisible() && categoryMen.isVisible() && categoryKids.isVisible();
  }

  public ProductsPage filterByPoloBrand() {
    brandPolo.click();
    page.waitForLoadState();
    return this;
  }

  public ProductsPage openFirstProductDetails() {
    productItems.first().locator("a[href^='/product_details']").first().click();
    page.waitForLoadState();
    return this;
  }

  public ProductsPage clickViewCart() {
    page.getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("View Cart"))
        .click();
    page.waitForLoadState();
    return this;
  }

  public boolean isViewCartLinkVisible() {
    var viewCart =
        page.getByRole(
            AriaRole.LINK,
            new com.microsoft.playwright.Page.GetByRoleOptions().setName("View Cart"));
    viewCart.waitFor();
    return viewCart.isVisible();
  }
}
