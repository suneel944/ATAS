package com.atas.shared.testing;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Paths;
import java.util.Optional;
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
    var options = new BrowserType.LaunchOptions().setHeadless(resolveHeadless());

    // Use system Chromium if CHROMIUM_PATH is set (when browser download is skipped)
    Optional.ofNullable(System.getenv("CHROMIUM_PATH"))
        .or(() -> Optional.ofNullable(System.getProperty("CHROMIUM_PATH")))
        .filter(path -> !path.isBlank())
        .ifPresent(path -> options.setExecutablePath(Paths.get(path)));

    browser = playwright.chromium().launch(options);
    context = browser.newContext();
    page = context.newPage();
  }

  @AfterEach
  void uiTearDown() {
    Optional.ofNullable(context).ifPresent(BrowserContext::close);
    Optional.ofNullable(browser).ifPresent(Browser::close);
    Optional.ofNullable(playwright).ifPresent(Playwright::close);
  }

  protected boolean resolveHeadless() {
    return Optional.ofNullable(System.getenv("HEADLESS"))
        .filter(s -> !s.isBlank())
        .or(() -> Optional.ofNullable(System.getProperty("HEADLESS")).filter(s -> !s.isBlank()))
        .map(Boolean::parseBoolean)
        .orElse(false);
  }
}
