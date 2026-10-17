package com.iortatechnxt.brokerverse.renewal.service.adapter;

import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource;
import com.iortatechnxt.brokerverse.renewal.service.port.RecipientPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapters of the Renewal ports (RENEWAL_DESIGN section 2.2). Each is registered only when
 * no module provides the port: Data Migration replaces {@link LegacyPolicySource} with its {@code
 * MigratedPolicySource}, and a messaging change replaces {@link RecipientPolicy} once BDOI defines
 * the recipient rules.
 */
@Configuration(proxyBeanMethods = false)
public class RenewalPortDefaults {

  /**
   * No migrated policies until Data Migration delivers its adapter; the upload {@code
   * RNW_LEGACY_POLICIES} is the fallback.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(LegacyPolicySource.class)
  public LegacyPolicySource defaultLegacyPolicySource() {
    return new NotConnectedLegacyPolicySource();
  }

  /**
   * Every well-formed address may receive renewal mail.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(RecipientPolicy.class)
  public RecipientPolicy defaultRecipientPolicy() {
    return new WellFormedRecipientPolicy();
  }
}
