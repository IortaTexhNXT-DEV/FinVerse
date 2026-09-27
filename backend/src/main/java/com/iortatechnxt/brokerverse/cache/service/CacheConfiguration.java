package com.iortatechnxt.brokerverse.cache.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Builds the application cache manager from the {@link CacheSpec} beans: Redis caches (JSON values,
 * per-cache time to live, keys {@code <prefix>cache:<name>::<key>}) when {@code
 * brokerverse.redis.enabled} is true, otherwise bounded in-memory Caffeine caches with the same
 * time to live. Unknown cache names fail fast. Cache errors (Redis down, unreadable entry) are
 * logged and read as a miss, so the application keeps working from the database.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties({CacheProperties.class, RedisSettings.class})
public class CacheConfiguration implements CachingConfigurer {

  private static final int SCAN_BATCH = 500;

  /**
   * The cache manager.
   *
   * @param specs cache declarations of every module
   * @param properties cache settings
   * @param redis Redis switch and key prefix
   * @param connectionFactory Redis connection (used only when Redis is enabled)
   * @param mapper application object mapper
   * @return cache manager
   */
  @Bean
  public CacheManager cacheManager(
      List<CacheSpec> specs,
      CacheProperties properties,
      RedisSettings redis,
      ObjectProvider<RedisConnectionFactory> connectionFactory,
      ObjectMapper mapper) {
    requireUniqueNames(specs);
    List<Cache> caches = new ArrayList<>();
    if (redis.enabled()) {
      RedisCacheManager manager = redisManager(specs, properties, redis, connectionFactory, mapper);
      specs.forEach(s -> caches.add(decorate(s, manager.getCache(s.name()))));
    } else {
      specs.forEach(s -> caches.add(decorate(s, caffeine(s, properties))));
    }
    SimpleCacheManager manager = new SimpleCacheManager();
    manager.setCaches(caches);
    manager.initializeCaches();
    return manager;
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new ResilientCacheErrorHandler();
  }

  private static RedisCacheManager redisManager(
      List<CacheSpec> specs,
      CacheProperties properties,
      RedisSettings redis,
      ObjectProvider<RedisConnectionFactory> connectionFactory,
      ObjectMapper mapper) {
    RedisCacheConfiguration defaults =
        RedisCacheConfiguration.defaultCacheConfig()
            .computePrefixWith(name -> redis.key("cache", name) + "::")
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                SerializationPair.fromSerializer(new CacheValueSerializer(mapper)));
    RedisCacheManager.RedisCacheManagerBuilder builder =
        RedisCacheManager.builder(
                RedisCacheWriter.nonLockingRedisCacheWriter(
                    connectionFactory.getObject(), BatchStrategies.scan(SCAN_BATCH)))
            .cacheDefaults(defaults)
            .disableCreateOnMissingCache();
    for (CacheSpec spec : specs) {
      builder.withCacheConfiguration(spec.name(), defaults.entryTtl(properties.ttlOf(spec)));
    }
    RedisCacheManager manager = builder.build();
    manager.afterPropertiesSet();
    return manager;
  }

  private static Cache caffeine(CacheSpec spec, CacheProperties properties) {
    return new CaffeineCache(
        spec.name(),
        Caffeine.newBuilder()
            .expireAfterWrite(properties.ttlOf(spec))
            .maximumSize(properties.maximumSize())
            .build(),
        true);
  }

  private static Cache decorate(CacheSpec spec, Cache cache) {
    return spec.readOnlyTransactionsOnly() ? new WriteTransactionBypassCache(cache) : cache;
  }

  private static void requireUniqueNames(List<CacheSpec> specs) {
    Set<String> names = new HashSet<>();
    for (CacheSpec spec : specs) {
      if (!names.add(spec.name())) {
        throw new IllegalStateException("Cache declared twice: " + spec.name());
      }
    }
  }
}
