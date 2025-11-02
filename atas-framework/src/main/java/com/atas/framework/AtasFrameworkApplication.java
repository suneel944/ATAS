package com.atas.framework;

import com.atas.framework.config.EnvFileLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ATAS framework. This class launches the Spring Boot application which exposes
 * REST APIs for managing and monitoring test executions. It also boots up the persistence layer and
 * schedules asynchronous tasks (such as video uploads).
 */
@SpringBootApplication
@org.springframework.boot.context.properties.EnableConfigurationProperties({
  com.atas.framework.config.StorageProperties.class,
  com.atas.framework.config.DatabaseProperties.class
})
public class AtasFrameworkApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(AtasFrameworkApplication.class);
    // Register EnvFileLoader to load .env file BEFORE property resolution
    application.addListeners(new EnvFileLoader());
    application.run(args);
  }
}
