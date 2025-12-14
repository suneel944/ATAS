package com.atas.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration enabling scheduled tasks and async processing used by the monitoring services. Also
 * configures CORS to allow SSE connections from the dashboard.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class MonitoringConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(@org.springframework.lang.NonNull CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("*")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
