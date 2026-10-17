package com.iortatechnxt.brokerverse.migration.archive.service;

import com.iortatechnxt.brokerverse.migration.archive.service.port.LegacyDocumentSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The default adapters of the archive ports (DATA_MIGRATION_DESIGN section 23). */
@Configuration(proxyBeanMethods = false)
public class ArchivePorts {

  /**
   * The documents staged in the console, until the transfer folder is connected.
   *
   * @param drop documents staged in the console
   * @return the source
   */
  @Bean
  @ConditionalOnMissingBean(LegacyDocumentSource.class)
  public LegacyDocumentSource consoleLegacyDocuments(LegacyDocumentDrop drop) {
    return drop::read;
  }
}
