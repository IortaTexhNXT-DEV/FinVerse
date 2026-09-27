package com.iortatechnxt.brokerverse.collections.bulk.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapter of the Collections bulk port (same rule as {@code OpsPortDefaults}): registered
 * only when the worklist does not provide {@link WorklistUpdates}.
 */
@Configuration(proxyBeanMethods = false)
public class CollectionsBulkDefaults {

  /**
   * Refuses the worklist columns until the worklist implements the port.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(WorklistUpdates.class)
  public WorklistUpdates defaultWorklistUpdates() {
    return new PendingWorklistUpdates();
  }
}
