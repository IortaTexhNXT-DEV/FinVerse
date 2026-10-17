package com.iortatechnxt.brokerverse.system.service;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.system.domain.ProductModuleSwitch;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cache of the product module switches, read on every request; cleared by any switch change. */
@Configuration(proxyBeanMethods = false)
public class ModuleCaches {

  /** Codes of the modules switched off (one entry). */
  public static final String SWITCHED_OFF = "system-modules-off";

  private static final Duration TTL = Duration.ofMinutes(5);

  /**
   * Declares the cache.
   *
   * @return spec (5 minutes; any switch change clears it)
   */
  @Bean
  public CacheSpec productModuleSwitchCache() {
    return CacheSpec.of(SWITCHED_OFF, TTL, ProductModuleSwitch.class);
  }
}
