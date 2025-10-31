package com.atas.products.automationexercise.pages;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class LoginPage extends BasePage<LoginPage> {

  private static final String BASE_URL = "https://automationexercise.com";

  private final Locator loginEmailField = page.locator("input[data-qa='login-email']");
  private final Locator loginPasswordField = page.locator("input[data-qa='login-password']");
  private final Locator loginButton = page.locator("button[data-qa='login-button']");
  private final Locator loginToAccountText = page.locator("h2:has-text('Login to your account')");
  private final Locator errorMessage = page.locator(".alert-danger");

  public LoginPage(Page page) {
    super(page);
  }

  public LoginPage gotoPage() {
    page.navigate(BASE_URL + "/login");
    page.waitForLoadState();
    return this;
  }

  public boolean isPageLoaded() {
    return loginToAccountText.isVisible();
  }

  public LoginPage login(String email, String password) {
    loginEmailField.fill(email);
    loginPasswordField.fill(password);
    loginButton.click();
    page.waitForLoadState();
    return this;
  }

  public boolean isErrorMessageVisible() {
    return errorMessage.isVisible();
  }
}
