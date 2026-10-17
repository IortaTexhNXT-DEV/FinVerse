package com.iortatechnxt.brokerverse.csf.service.adapter;

import com.iortatechnxt.brokerverse.csf.service.port.ContactSyncGateway;
import com.iortatechnxt.brokerverse.csf.service.port.LegacyAccountLookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapters of the Customer Servicing Facility ports (CUSTOMER_SERVICING_DESIGN section
 * 2.2), each registered only when no module provides the port.
 */
@Configuration(proxyBeanMethods = false)
public class CsfPortDefaults {

  /**
   * No transport to the legacy systems.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(ContactSyncGateway.class)
  public ContactSyncGateway notConfiguredContactSync() {
    return new NotConfiguredContactSync();
  }

  /**
   * No lookup of legacy-only accounts.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(LegacyAccountLookup.class)
  public LegacyAccountLookup noLegacyAccountLookup() {
    return new NoLegacyAccountLookup();
  }
}
