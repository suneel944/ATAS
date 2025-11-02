package com.atas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * Environment-agnostic test configuration that uses Testcontainers for database testing and
 * environment variables for external service configuration.
 *
 * <p>This configuration ensures tests can run in any environment without hardcoded values.
 */
@Configuration
@TestPropertySource(
    properties = {
      "spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/atas_test}",
      "spring.datasource.username=${DB_USERNAME:atas}",
      "spring.datasource.password=${DB_PASSWORD:ataspass}",
      "atas.storage.bucket=${S3_BUCKET:test-bucket}",
      "atas.storage.region=${S3_REGION:us-east-1}",
      "atas.storage.video-folder=${S3_VIDEO_FOLDER:test-videos}",
      "atas.storage.screenshot-folder=${S3_SCREENSHOT_FOLDER:test-screenshots}"
    })
public class TestConfiguration {

  /**
   * PostgreSQL Testcontainer for database testing. This provides a clean, isolated database for
   * each test run. The container is automatically managed by Testcontainers lifecycle.
   */
  @Container
  @SuppressWarnings("resource") // Testcontainers manages the lifecycle automatically
  public static final PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("atas_test")
          .withUsername("atas")
          .withPassword("ataspass")
          .withReuse(true);

  /**
   * Bean to provide the database URL from the Testcontainer. Falls back to environment variables or
   * system properties if Testcontainer is not used.
   *
   * <p>Priority: Testcontainer > Environment Variable > System Property > Default
   */
  @Bean
  @Primary
  public String databaseUrl() {
    if (postgresContainer.isRunning()) {
      return postgresContainer.getJdbcUrl();
    }
    String value = System.getenv("DB_URL");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    value = System.getProperty("DB_URL");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return "jdbc:postgresql://localhost:5432/atas_test";
  }

  /**
   * Bean to provide the database username. Falls back to environment variables or system properties
   * if Testcontainer is not used.
   *
   * <p>Priority: Testcontainer > Environment Variable > System Property > Default
   */
  @Bean
  @Primary
  public String databaseUsername() {
    if (postgresContainer.isRunning()) {
      return postgresContainer.getUsername();
    }
    String value = System.getenv("DB_USERNAME");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    value = System.getProperty("DB_USERNAME");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return "atas";
  }

  /**
   * Bean to provide the database password. Falls back to environment variables or system properties
   * if Testcontainer is not used.
   *
   * <p>Priority: Testcontainer > Environment Variable > System Property > Default
   */
  @Bean
  @Primary
  public String databasePassword() {
    if (postgresContainer.isRunning()) {
      return postgresContainer.getPassword();
    }
    String value = System.getenv("DB_PASSWORD");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    value = System.getProperty("DB_PASSWORD");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return "ataspass";
  }

  /**
   * Bean to provide the S3 bucket name from environment variables or system properties. Priority:
   * Environment Variable > System Property > Default
   */
  @Bean
  @Primary
  public String s3Bucket() {
    String value = System.getenv("S3_BUCKET");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    value = System.getProperty("S3_BUCKET");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return "test-bucket";
  }

  /**
   * Bean to provide the S3 region from environment variables or system properties. Priority:
   * Environment Variable > System Property > Default
   */
  @Bean
  @Primary
  public String s3Region() {
    String value = System.getenv("S3_REGION");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    value = System.getProperty("S3_REGION");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return "us-east-1";
  }

  /**
   * Bean to provide the S3 video folder from environment variables or system properties. Priority:
   * Environment Variable > System Property > Default
   */
  @Bean
  @Primary
  public String s3VideoFolder() {
    String value = System.getenv("S3_VIDEO_FOLDER");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    value = System.getProperty("S3_VIDEO_FOLDER");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return "test-videos";
  }

  /**
   * Bean to provide the S3 screenshot folder from environment variables or system properties.
   * Priority: Environment Variable > System Property > Default
   */
  @Bean
  @Primary
  public String s3ScreenshotFolder() {
    String value = System.getenv("S3_SCREENSHOT_FOLDER");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    value = System.getProperty("S3_SCREENSHOT_FOLDER");
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return "test-screenshots";
  }
}
