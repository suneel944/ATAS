package com.atas.framework.core.driver;

/**
 * Enumeration of supported browser types. These values map onto Playwright's browser types.
 * Additional browsers can be added if Playwright introduces new ones.
 */
public enum BrowserType {
  CHROMIUM,
  FIREFOX,
  WEBKIT;

  /**
   * Resolve the Playwright browser type name used by the API.
   *
   * @return the lowercase browser name
   */
  public String getName() {
    return this.name().toLowerCase();
  }
}
