package com.iortatechnxt.brokerverse.cache.api;

import com.iortatechnxt.brokerverse.cache.api.dto.CacheResponse;
import com.iortatechnxt.brokerverse.cache.service.CacheInvalidator;
import com.iortatechnxt.brokerverse.cache.service.CacheProperties;
import com.iortatechnxt.brokerverse.cache.service.RedisSettings;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Support API of the reference-data caches (System Administrator only): list the caches and flush
 * one or all of them after a change made outside the application (runbook).
 */
@RestController
@RequestMapping("/api/v1/admin/caches")
@PreAuthorize("hasAuthority('SYSTEM_PARAMETER_MANAGE')")
public class CacheAdminController {

  private static final Logger LOG = LoggerFactory.getLogger(CacheAdminController.class);

  private final CacheInvalidator caches;
  private final CacheProperties properties;
  private final RedisSettings redis;

  /**
   * Creates the controller.
   *
   * @param caches cache registry
   * @param properties cache settings
   * @param redis Redis switch
   */
  public CacheAdminController(
      CacheInvalidator caches, CacheProperties properties, RedisSettings redis) {
    this.caches = caches;
    this.properties = properties;
    this.redis = redis;
  }

  /**
   * Lists the caches.
   *
   * @return caches with their time to live and store
   */
  @GetMapping
  public List<CacheResponse> list() {
    String store = redis.enabled() ? "REDIS" : "IN_MEMORY";
    return caches.specs().stream()
        .map(s -> CacheResponse.from(s, properties.ttlOf(s), store))
        .toList();
  }

  /**
   * Flushes one cache.
   *
   * @param name cache name
   */
  @PostMapping("/{name}/clear")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clear(@PathVariable String name) {
    if (!caches.clear(name)) {
      throw new ResourceNotFoundException("Cache", name);
    }
    LOG.info("Cache {} flushed by an administrator", name);
  }

  /** Flushes every cache. */
  @PostMapping("/clear")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearAll() {
    caches.clearAll();
    LOG.info("All caches flushed by an administrator");
  }
}
