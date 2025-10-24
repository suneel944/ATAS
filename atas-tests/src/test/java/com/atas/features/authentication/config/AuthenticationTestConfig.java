package com.atas.features.authentication.config;

import com.atas.framework.core.driver.DriverConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for authentication feature tests. Provides common driver configurations and
 * test settings.
 */
@Configuration
@Slf4j
public class AuthenticationTestConfig {

  /**
   * Default driver configuration for authentication UI tests. Optimized for login/dashboard testing
   * scenarios.
   */
  @Bean("authenticationDriverConfig")
  public DriverConfig authenticationDriverConfig() {
    return DriverConfig.builder()
        .headless(true)
        .recordVideo(false) // Disable video recording for authentication tests
        .videoDir("target/test-videos/authentication")
        .viewportWidth(1280)
        .viewportHeight(720)
        .build();
  }

  /** Configuration for authentication tests that require video recording. */
  @Bean("authenticationDriverConfigWithRecording")
  public DriverConfig authenticationDriverConfigWithRecording() {
    return DriverConfig.builder()
        .headless(false)
        .recordVideo(true)
        .videoDir("target/test-videos/authentication")
        .viewportWidth(1280)
        .viewportHeight(720)
        .build();
  }

  /** Configuration for mobile viewport testing. */
  @Bean("authenticationMobileDriverConfig")
  public DriverConfig authenticationMobileDriverConfig() {
    return DriverConfig.builder()
        .headless(true)
        .recordVideo(false)
        .videoDir("target/test-videos/authentication/mobile")
        .viewportWidth(375)
        .viewportHeight(667)
        .build();
  }
}
