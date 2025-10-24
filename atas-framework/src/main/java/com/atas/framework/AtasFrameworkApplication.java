package com.atas.framework;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ATAS framework. This class launches the Spring Boot application which exposes
 * REST APIs for managing and monitoring test executions. It also boots up the persistence layer and
 * schedules asynchronous tasks (such as video uploads).
 */
@SpringBootApplication
@org.springframework.boot.context.properties.EnableConfigurationProperties(
    com.atas.framework.config.StorageProperties.class)
public class AtasFrameworkApplication {

  public static void main(String[] args) {
    SpringApplication.run(AtasFrameworkApplication.class, args);
  }
}
