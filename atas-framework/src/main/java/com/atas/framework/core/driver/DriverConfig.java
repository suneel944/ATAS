package com.atas.framework.core.driver;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Configuration object for creating Playwright browser pages. It encapsulates common settings such
 * as whether to run headless, enable video recording and viewport dimensions. Use the builder
 * pattern to construct instances.
 *
 * <p>Note: Default values can be overridden via Spring properties or environment variables. For
 * videoDir, use ATAS_PLAYWRIGHT_VIDEO_DIR or atas.playwright.video.dir property.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriverConfig {
  /** Run the browser in headless mode (true by default) */
  @Builder.Default boolean headless = true;

  /**
   * Whether to enable video recording for this page. When enabled, Playwright will save a video
   * file after the page is closed.
   */
  @Builder.Default boolean recordVideo = true;

  /**
   * Directory where Playwright should save videos. Defaults to "videos" but can be set via
   * environment variable PLAYWRIGHT_VIDEO_DIR or Spring property atas.playwright.video.dir
   */
  @Builder.Default String videoDir = getDefaultVideoDir();

  private static String getDefaultVideoDir() {
    String envDir = System.getenv("PLAYWRIGHT_VIDEO_DIR");
    if (envDir != null && !envDir.isEmpty()) {
      return envDir;
    }
    String propDir = System.getProperty("atas.playwright.video.dir");
    if (propDir != null && !propDir.isEmpty()) {
      return propDir;
    }
    return "videos";
  }

  /** Viewport width in pixels */
  @Builder.Default int viewportWidth = 1280;

  /** Viewport height in pixels */
  @Builder.Default int viewportHeight = 720;
}
