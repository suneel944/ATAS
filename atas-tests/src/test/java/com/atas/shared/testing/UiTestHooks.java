package com.atas.shared.testing;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class that manages Playwright UI setup/teardown for tests. Extend this in UI test classes to
 * get ready {@link #page} and {@link #context}.
 */
public abstract class UiTestHooks {

  protected Playwright playwright;
  protected Browser browser;
  protected BrowserContext context;
  protected Page page;

  @BeforeEach
  void uiSetUp() {
    playwright = Playwright.create();
    browser =
        playwright
            .chromium()
            .launch(new BrowserType.LaunchOptions().setHeadless(resolveHeadless()));
    context = browser.newContext();
    page = context.newPage();
  }

  @AfterEach
  void uiTearDown() {
    if (context != null) {
      context.close();
    }
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  protected boolean resolveHeadless() {
    String fromEnv = System.getenv("HEADLESS");
    if (fromEnv != null && !fromEnv.isBlank()) {
      return Boolean.parseBoolean(fromEnv);
    }
    String fromProp = System.getProperty("HEADLESS");
    if (fromProp != null && !fromProp.isBlank()) {
      return Boolean.parseBoolean(fromProp);
    }
    return false;
  }
}
