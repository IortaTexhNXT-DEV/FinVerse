package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.service.port.LegacyInvoiceSource;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The default adapters of the booking ports. */
@Configuration(proxyBeanMethods = false)
public class BookingPorts {

  /**
   * No migrated invoice, until Operations provides them.
   *
   * @return the source
   */
  @Bean
  @ConditionalOnMissingBean(LegacyInvoiceSource.class)
  public LegacyInvoiceSource noLegacyInvoices() {
    return (companyId, arn, effectiveDate) -> Optional.empty();
  }
}
