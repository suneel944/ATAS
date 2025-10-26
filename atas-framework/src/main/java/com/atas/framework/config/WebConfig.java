package com.atas.framework.config;

import com.atas.framework.monitoring.QueryPerformanceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering interceptors and other web-related components.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final QueryPerformanceInterceptor queryPerformanceInterceptor;

    @Override
    public void addInterceptors(@org.springframework.lang.NonNull InterceptorRegistry registry) {
        registry.addInterceptor(queryPerformanceInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/database/health", "/api/v1/database/operations", "/api/v1/database/statistics");
    }
}
