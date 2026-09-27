package com.iortatechnxt.brokerverse.cache.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Keeps the application working when the cache fails (Redis unreachable, an entry written by an
 * incompatible release): a failed read is a miss, a failed write is skipped. A failed eviction is
 * logged as an error because the entry may stay stale until its time to live ends (see the runbook
 * in PLATFORM_CACHE_AND_EVENTS.md: flush the cache).
 */
public class ResilientCacheErrorHandler implements CacheErrorHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ResilientCacheErrorHandler.class);

  @Override
  public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
    LOG.warn(
        "Cache {} read failed for key {}, reading the database", cache.getName(), key, exception);
    try {
      cache.evictIfPresent(key);
    } catch (RuntimeException ex) {
      LOG.debug("Cache {} entry {} could not be removed either", cache.getName(), key, ex);
    }
  }

  @Override
  public void handleCachePutError(
      RuntimeException exception, Cache cache, Object key, Object value) {
    LOG.warn("Cache {} write failed for key {}", cache.getName(), key, exception);
  }

  @Override
  public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
    LOG.error("Cache {} eviction failed for key {}", cache.getName(), key, exception);
  }

  @Override
  public void handleCacheClearError(RuntimeException exception, Cache cache) {
    LOG.error("Cache {} clear failed", cache.getName(), exception);
  }
}
