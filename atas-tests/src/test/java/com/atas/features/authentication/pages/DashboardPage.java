package com.atas.features.authentication.pages;

import static org.assertj.core.api.Assertions.assertThat;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Page;

/**
 * Page Object representing a dashboard page shown after successful login. Provides verification
 * methods to assert that the page loaded correctly.
 */
public class DashboardPage extends BasePage<DashboardPage> {

  public DashboardPage(Page page) {
    super(page);
  }

  /**
   * Verify that the dashboard is loaded by checking the page title. Returns this instance for
   * further assertions.
   */
  public DashboardPage verifyLoaded() {
    String title = page.title();
    assertThat(title).containsIgnoringCase("dashboard");
    return this;
  }
}
