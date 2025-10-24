package com.atas.features.authentication.pages;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Page;

/**
 * Page Object representing a login page. Provides fluent methods to enter credentials and submit
 * the form. Demonstrates method chaining for clear and concise test code.
 */
public class LoginPage extends BasePage<LoginPage> {

  public LoginPage(Page page) {
    super(page);
  }

  /** Navigate to the login page. Returns this instance for chaining. */
  public LoginPage navigate() {
    // Use local test HTML file instead of external URL
    String testLoginUrl =
        getClass().getClassLoader().getResource("test-login.html").toExternalForm();
    page.navigate(testLoginUrl);
    return this;
  }

  /** Enter a username into the username field. */
  public LoginPage enterUsername(String username) {
    page.locator("#username").fill(username);
    return this;
  }

  /** Enter a password into the password field. */
  public LoginPage enterPassword(String password) {
    page.locator("#password").fill(password);
    return this;
  }

  /**
   * Click the login button. Returns a new {@link DashboardPage} representing the next page in the
   * workflow.
   */
  public DashboardPage clickLogin() {
    page.locator("#login-button").click();
    return new DashboardPage(page);
  }
}
