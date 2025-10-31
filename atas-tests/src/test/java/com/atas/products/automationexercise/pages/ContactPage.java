package com.atas.products.automationexercise.pages;

import com.atas.shared.pages.BasePage;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class ContactPage extends BasePage<ContactPage> {

  private static final String BASE_URL = "https://automationexercise.com";

  private final Locator contactForm = page.locator("form[action='/contact_us']");

  public ContactPage(Page page) {
    super(page);
  }

  public ContactPage gotoPage() {
    page.navigate(BASE_URL + "/contact_us");
    page.waitForLoadState();
    return this;
  }

  public boolean isContactFormVisible() {
    return contactForm.isVisible();
  }
}
