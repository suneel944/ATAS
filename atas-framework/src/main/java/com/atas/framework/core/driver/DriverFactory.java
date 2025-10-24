package com.atas.framework.core.driver;

import com.microsoft.playwright.Page;

/**
 * Factory API for creating Playwright page instances.  Implementations
 * are responsible for launching browsers, creating isolated contexts
 * and pages and ensuring proper cleanup after use.  The factory
 * abstracts away driver configuration details from the callers.
 */
public interface DriverFactory {

    /**
     * Create a new {@link Page} for the given browser type and
     * configuration.  Implementations must guarantee that pages are
     * isolated from each other (e.g. using separate browser contexts).
     *
     * @param browserType the desired browser type
     * @param config driver configuration
     * @return a ready-to-use Playwright page
     */
    Page createPage(BrowserType browserType, DriverConfig config);

    /**
     * Shut down all browsers and cleanup resources.  This method should
     * be invoked when the JVM is shutting down to ensure there are no
     * leaking processes.
     */
    void closeAll();
}