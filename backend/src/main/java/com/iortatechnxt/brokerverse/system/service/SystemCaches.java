package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cache of the business parameters: raw value per key, cleared by any parameter change. */
@Configuration(proxyBeanMethods = false)
public class SystemCaches {

  /** Raw parameter value (null when missing), keyed by parameter key. */
  public static final String PARAMETERS = "system-parameters";

  private static final Duration TTL = Duration.ofMinutes(15);

  /**
   * Declares the cache.
   *
   * @return spec (15 minutes; any parameter change clears it)
   */
  @Bean
  public CacheSpec systemParametersCache() {
    return CacheSpec.of(PARAMETERS, TTL, SystemParameter.class);
  }
}
