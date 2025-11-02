package com.atas.framework.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Database configuration properties loaded from environment variables or .env file. Spring Boot
 * automatically binds environment variables and system properties to these fields.
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource")
@Getter
@Setter
public class DatabaseProperties {
  private String url;
  private String username;
  private String password;
  private String driverClassName;
}
