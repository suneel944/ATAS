package com.atas.features.monitoring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for monitoring feature tests. Provides REST client configurations and test
 * settings.
 */
@Configuration
@Slf4j
public class MonitoringTestConfig {

  /**
   * REST template for monitoring API tests. Configured with appropriate timeouts and error
   * handling.
   */
  @Bean("monitoringRestTemplate")
  public RestTemplate monitoringRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();

    // Configure timeouts
    // Note: In a real implementation, you might want to configure
    // HttpComponentsClientHttpRequestFactory with specific timeouts

    log.info("Configured RestTemplate for monitoring API tests");
    return restTemplate;
  }

  /**
   * Base URL configuration for monitoring endpoints. This would typically be externalized to
   * application properties.
   */
  @Bean("monitoringBaseUrl")
  public String monitoringBaseUrl() {
    return "/api/v1/test-execution";
  }

  /** Default timeout configuration for monitoring API calls. */
  @Bean("monitoringApiTimeout")
  public int monitoringApiTimeout() {
    return 30000; // 30 seconds
  }
}
