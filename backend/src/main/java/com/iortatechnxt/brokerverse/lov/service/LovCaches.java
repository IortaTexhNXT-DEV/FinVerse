package com.iortatechnxt.brokerverse.lov.service;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.lov.domain.LovType;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cache of the lists of values: one entry per list type, cleared by any list or value change. */
@Configuration(proxyBeanMethods = false)
public class LovCaches {

  /** Every value of a list, keyed by list type code. */
  public static final String VALUES = "lov-values";

  /**
   * Declares the cache.
   *
   * @return spec (1 hour; any change of a list or value clears it)
   */
  @Bean
  public CacheSpec lovValuesCache() {
    return CacheSpec.of(VALUES, Duration.ofHours(1), LovValue.class, LovType.class);
  }
}
