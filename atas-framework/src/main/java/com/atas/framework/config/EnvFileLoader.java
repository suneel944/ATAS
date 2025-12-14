package com.atas.framework.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;

/**
 * Configuration class that loads .env file EARLY in Spring Boot startup (before property
 * resolution) and sets values as system properties so Spring Boot's native property resolution can
 * use them.
 *
 * <p>This follows Spring Boot best practices: 1. Loads .env file BEFORE Spring Boot processes
 * application.yml/properties files 2. Sets values as system properties (Spring Boot reads these
 * automatically) 3. System environment variables take precedence (standard Spring Boot behavior)
 *
 * <p>Priority order (Spring Boot standard): 1. System environment variables (highest priority) 2.
 * System properties (from .env file loaded here) 3. application.yml defaults (lowest priority)
 *
 * <p>Note: Uses ApplicationEnvironmentPreparedEvent which fires
 * BEFORE @PostConstruct/@Configuration processing, ensuring .env values are available during
 * property resolution.
 */
@Slf4j
public class EnvFileLoader implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

  @Override
  public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
    loadEnvFile();
  }

  /**
   * Load .env file and set values as system properties. This method is called early in Spring Boot
   * startup lifecycle.
   */
  private void loadEnvFile() {
    try {
      String userDir = System.getProperty("user.dir");
      Dotenv dotenv = Dotenv.configure().directory(userDir).ignoreIfMissing().load();

      int loadedCount = 0;
      // Load .env variables into system properties so Spring Boot can read them
      // Only set if not already present (system env vars take precedence)
      for (var entry : dotenv.entries()) {
        String key = entry.getKey();
        String value = entry.getValue();

        // Only set if not already present as system property or environment variable
        if (System.getProperty(key) == null && System.getenv(key) == null) {
          System.setProperty(key, value);
          loadedCount++;
        }
      }

      if (loadedCount > 0) {
        log.info("✅ Loaded {} variables from .env file: {}", loadedCount, userDir);
      }
    } catch (Exception e) {
      // .env file not found or error loading it - this is OK
    }
  }
}
