package com.atas.shared.auth;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for creating and managing Playwright browser instances.
 *
 * <p>This follows the same pattern as {@link com.atas.shared.testing.UiTestHooks} but provides a
 * reusable utility for cases where browser automation is needed outside of UI test hooks (e.g., for
 * Auth0 authentication in API tests).
 *
 * <p>Usage:
 *
 * <pre>{@code
 * BrowserHelper browserHelper = new BrowserHelper();
 * try {
 *   Page page = browserHelper.createPage();
 *   // Use page for automation
 * } finally {
 *   browserHelper.close();
 * }
 * }</pre>
 */
@Slf4j
public class BrowserHelper {

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  private Page page;

  /**
   * Resolves headless mode from environment variables or system properties.
   *
   * @return true if headless mode should be used, false for headed mode
   */
  private boolean resolveHeadless() {
    return Optional.ofNullable(System.getenv("HEADLESS"))
        .filter(s -> !s.isBlank())
        .map(Boolean::parseBoolean)
        .or(
            () ->
                Optional.ofNullable(System.getProperty("HEADLESS"))
                    .filter(s -> !s.isBlank())
                    .map(Boolean::parseBoolean))
        .orElse(true);
  }

  /**
   * Creates a new browser page for automation.
   *
   * <p>Initializes Playwright, launches a Chromium browser (headless/headed based on HEADLESS env
   * var), creates a context, and returns a ready-to-use page.
   *
   * @return Playwright Page instance
   */
  public Page createPage() {
    playwright = Optional.ofNullable(playwright).orElseGet(Playwright::create);
    browser =
        Optional.ofNullable(browser)
            .orElseGet(
                () -> {
                  var options =
                      new com.microsoft.playwright.BrowserType.LaunchOptions()
                          .setHeadless(resolveHeadless());

                  // Use system Chromium if CHROMIUM_PATH is set (when browser download is skipped)
                  Optional.ofNullable(System.getenv("CHROMIUM_PATH"))
                      .or(() -> Optional.ofNullable(System.getProperty("CHROMIUM_PATH")))
                      .filter(path -> !path.isBlank())
                      .ifPresent(path -> options.setExecutablePath(Paths.get(path)));

                  return playwright.chromium().launch(options);
                });
    context = Optional.ofNullable(context).orElseGet(browser::newContext);
    page = Optional.ofNullable(page).orElseGet(context::newPage);
    return page;
  }

  /**
   * Gets the current page, creating it if necessary.
   *
   * @return Playwright Page instance
   */
  public Page getPage() {
    if (page == null) {
      return createPage();
    }
    return page;
  }

  /**
   * Gets the browser context.
   *
   * @return BrowserContext instance
   */
  public BrowserContext getContext() {
    if (context == null) {
      createPage();
    }
    return context;
  }

  /** Closes all browser resources and cleans up. */
  public void close() {
    closeResource(page, "page", () -> page = null);
    closeResource(context, "context", () -> context = null);
    closeResource(browser, "browser", () -> browser = null);
    closeResource(playwright, "Playwright", () -> playwright = null);
  }

  private void closeResource(AutoCloseable resource, String name, Runnable cleanup) {
    if (resource != null) {
      try {
        resource.close();
      } catch (Exception e) {
        log.warn("Error closing {}", name, e);
      } finally {
        cleanup.run();
      }
    }
  }

  /**
   * Checks if browser resources are still open.
   *
   * @return true if browser is still active
   */
  public boolean isOpen() {
    return playwright != null;
  }
}
