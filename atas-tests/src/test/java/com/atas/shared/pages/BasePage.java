package com.atas.shared.pages;

import com.microsoft.playwright.Page;

/**
 * Abstract base class for all Page Objects. Holds a reference to the underlying Playwright {@link
 * Page} and provides common utility methods. Concrete pages extend this class and expose
 * domain-specific actions via a fluent API.
 */
public abstract class BasePage<T extends BasePage<T>> {
  protected final Page page;

  protected BasePage(Page page) {
    this.page = page;
  }

  /**
   * Get the underlying Playwright page. Useful for taking screenshots or performing operations
   * outside the page object.
   *
   * @return the Playwright page
   */
  public Page getPage() {
    return page;
  }
}
