package com.atas.framework.core.driver;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of {@link DriverFactory} backed by Playwright. This factory manages
 * browser instances per browser type and creates new browser contexts for each page request to
 * ensure isolation between tests. Video recording is configured according to the provided {@link
 * DriverConfig}.
 */
@Slf4j
@Component
public class PlaywrightDriverFactory implements DriverFactory {

  /** Shared Playwright instance. Lazily initialised on first use. */
  private Playwright playwright;

  /** Map of browser type to an active browser instance. */
  private final Map<BrowserType, Browser> browsers = new ConcurrentHashMap<>();

  @Override
  public synchronized Page createPage(BrowserType browserType, DriverConfig config) {
    // Initialise Playwright if not already done
    if (playwright == null) {
      playwright = Playwright.create();
      log.info("Playwright initialised");
    }

    // Obtain or create the browser for the given type
    Browser browser =
        browsers.computeIfAbsent(
            browserType,
            type -> {
              // Use Playwright's BrowserType.LaunchOptions explicitly (avoid our BrowserType)
              com.microsoft.playwright.BrowserType.LaunchOptions options =
                  new com.microsoft.playwright.BrowserType.LaunchOptions()
                      .setHeadless(config.isHeadless());

              switch (type) {
                case CHROMIUM:
                  // Use system Chromium if CHROMIUM_PATH is set (when browser download is skipped)
                  Optional.ofNullable(System.getenv("CHROMIUM_PATH"))
                      .or(() -> Optional.ofNullable(System.getProperty("CHROMIUM_PATH")))
                      .filter(path -> !path.isBlank())
                      .ifPresent(path -> options.setExecutablePath(Paths.get(path)));
                  return playwright.chromium().launch(options);
                case FIREFOX:
                  return playwright.firefox().launch(options);
                case WEBKIT:
                  return playwright.webkit().launch(options);
                default:
                  throw new IllegalArgumentException("Unsupported browser type: " + type);
              }
            });

    // Create a new browser context for isolation. Configure video recording if enabled.
    com.microsoft.playwright.Browser.NewContextOptions contextOptions =
        new com.microsoft.playwright.Browser.NewContextOptions()
            .setViewportSize(config.getViewportWidth(), config.getViewportHeight());

    if (config.isRecordVideo()) {
      Path videoDir = Paths.get(config.getVideoDir());
      contextOptions
          .setRecordVideoDir(videoDir)
          .setRecordVideoSize(config.getViewportWidth(), config.getViewportHeight());
    }

    BrowserContext context = browser.newContext(contextOptions);
    return context.newPage();
  }

  @Override
  public synchronized APIRequestContext createApiRequestContext(String baseUrl) {
    if (playwright == null) {
      playwright = Playwright.create();
      log.info("Playwright initialised");
    }
    return playwright.request().newContext(new APIRequest.NewContextOptions().setBaseURL(baseUrl));
  }

  @Override
  public synchronized void closeAll() {
    browsers
        .values()
        .forEach(
            b -> {
              try {
                b.close();
              } catch (Exception e) {
                log.warn("Error closing browser", e);
              }
            });
    browsers.clear();
    if (playwright != null) {
      playwright.close();
      playwright = null;
    }
    log.info("All browsers closed and Playwright shutdown");
  }
}
