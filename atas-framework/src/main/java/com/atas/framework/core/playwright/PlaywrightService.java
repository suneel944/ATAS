package com.atas.framework.core.playwright;

import com.atas.framework.core.driver.BrowserType;
import com.atas.framework.core.driver.DriverConfig;
import com.atas.framework.core.driver.DriverFactory;
import com.atas.framework.model.AttachmentType;
import com.atas.framework.model.TestAttachment;
import com.atas.framework.model.TestResult;
import com.atas.framework.repository.TestAttachmentRepository;
import com.microsoft.playwright.Page;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * High-level service for interacting with Playwright within the framework. Provides convenience
 * methods to obtain pages, capture screenshots and record videos. The service delegates browser
 * management to the configured {@link DriverFactory}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaywrightService {

  private final DriverFactory driverFactory;
  private final TestAttachmentRepository attachmentRepository;

  /**
   * Create a new page using the underlying driver factory.
   *
   * @param browserType desired browser
   * @param config driver configuration
   * @return ready-to-use Playwright page
   */
  public Page createPage(BrowserType browserType, DriverConfig config) {
    return driverFactory.createPage(browserType, config);
  }

  /**
   * Take a screenshot of the current state of the given page and persist it as a test attachment.
   * The screenshot file will be saved into the provided directory. A {@link TestAttachment} entity
   * is created and returned so that the caller can associate it with a {@link TestResult} or {@link
   * com.atas.framework.model.TestStep}.
   *
   * @param page the Playwright page to capture
   * @param result the result this screenshot belongs to
   * @param outputDir directory to save the screenshot file
   * @return the persisted attachment entity
   */
  public TestAttachment captureScreenshot(Page page, TestResult result, Path outputDir) {
    try {
      Files.createDirectories(outputDir);
      Path tempFile = Files.createTempFile("atas-screenshot", ".png");
      page.screenshot(new Page.ScreenshotOptions().setPath(tempFile));
      String fileName = String.format("%s_%d.png", result.getTestId(), System.currentTimeMillis());
      Path target = outputDir.resolve(fileName);
      Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
      TestAttachment attachment =
          TestAttachment.builder()
              .result(result)
              .type(AttachmentType.SCREENSHOT)
              .fileName(fileName)
              .mimeType("image/png")
              .url(target.toString())
              .createdAt(LocalDateTime.now())
              .build();
      attachment = attachmentRepository.save(attachment);
      log.info("Captured screenshot for test {} saved to {}", result.getTestId(), target);
      return attachment;
    } catch (IOException e) {
      throw new RuntimeException("Failed to capture screenshot", e);
    }
  }

  /**
   * Shutdown all browsers and Playwright resources. This method delegates to the underlying
   * factory.
   */
  public void shutdown() {
    driverFactory.closeAll();
  }
}
