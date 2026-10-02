package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import com.iortatechnxt.brokerverse.system.domain.SystemParameterRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached raw value of a business parameter ({@link SystemCaches#PARAMETERS}); the typed getters of
 * {@link SystemParameterService} read through it.
 */
@Service
public class SystemParameterLookup {

  private final SystemParameterRepository repository;

  /**
   * Creates the lookup.
   *
   * @param repository parameter repository
   */
  public SystemParameterLookup(SystemParameterRepository repository) {
    this.repository = repository;
  }

  /**
   * Raw value of a parameter.
   *
   * @param key key
   * @return stored value, null when the parameter does not exist
   */
  @Cacheable(cacheNames = SystemCaches.PARAMETERS, key = "#key")
  @Transactional(readOnly = true)
  public String value(String key) {
    return repository.findByKey(key).map(SystemParameter::getValue).orElse(null);
  }
}
