package com.atas.framework.config;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis caching. Enables Spring Cache abstraction with Redis backend. Provides
 * different TTLs for different cache types: - Dashboard overview: 60s (frequently updated) - Recent
 * executions: 30s (real-time data) - Execution trends: 5min (historical data) - Execution status:
 * 10s (very real-time)
 */
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(60))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

    // Different TTLs for different cache types
    RedisCacheConfiguration dashboardOverviewConfig =
        defaultConfig.entryTtl(Duration.ofSeconds(60));
    RedisCacheConfiguration dashboardRecentConfig = defaultConfig.entryTtl(Duration.ofSeconds(30));
    // Reduced cache time for trends to ensure latest data is shown quickly
    RedisCacheConfiguration dashboardTrendsConfig = defaultConfig.entryTtl(Duration.ofSeconds(30));
    RedisCacheConfiguration executionStatusConfig = defaultConfig.entryTtl(Duration.ofSeconds(10));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withCacheConfiguration("dashboard-overview", dashboardOverviewConfig)
        .withCacheConfiguration("dashboard-recent", dashboardRecentConfig)
        .withCacheConfiguration("dashboard-trends", dashboardTrendsConfig)
        .withCacheConfiguration("execution-status", executionStatusConfig)
        .build();
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
  }

  @Bean
  @Lazy
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    // Lazy initialization prevents connection attempts during ApplicationContext startup
    // Container will start when RedisSseService.init() is called
    return container;
  }
}
