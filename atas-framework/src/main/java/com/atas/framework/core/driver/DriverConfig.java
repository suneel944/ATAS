package com.atas.framework.core.driver;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Configuration object for creating Playwright browser pages. It encapsulates common settings such
 * as whether to run headless, enable video recording and viewport dimensions. Use the builder
 * pattern to construct instances.
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

  /** Directory where Playwright should save videos */
  @Builder.Default String videoDir = System.getProperty("playwright.video.dir", "videos");

  /** Viewport width in pixels */
  @Builder.Default int viewportWidth = 1280;

  /** Viewport height in pixels */
  @Builder.Default int viewportHeight = 720;
}
