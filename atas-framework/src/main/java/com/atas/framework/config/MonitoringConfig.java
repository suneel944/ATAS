package com.atas.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration enabling scheduled tasks used by the monitoring
 * services.  Without this annotation, the {@code @Scheduled}
 * annotations in {@link com.atas.framework.monitoring.TestMonitoringService}
 * would have no effect.
 */
@Configuration
@EnableScheduling
public class MonitoringConfig {
}