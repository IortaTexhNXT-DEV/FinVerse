package com.iortatechnxt.brokerverse.submitted.service.adapter;

import com.iortatechnxt.brokerverse.submitted.service.port.MailHouseGateway;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff;
import com.iortatechnxt.brokerverse.submitted.service.port.SignatureProvider;
import com.iortatechnxt.brokerverse.submitted.service.port.SubmittedSourceFeed;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapters of the Submitted Policies ports (SUBMITTED_POLICIES_DESIGN section 2.2), each
 * registered only when no module provides the port: the Renewal module replaces {@link
 * RenewalHandOff}, a qualified e-signature {@link SignatureProvider}, a bank feed {@link
 * SubmittedSourceFeed}; the default {@link MailHouseGateway}, the print batch, is the {@code
 * PrintBatchMailHouse} component.
 */
@Configuration(proxyBeanMethods = false)
public class SubmittedPortDefaults {

  /**
   * Hand-offs recorded as pending until the Renewal module is connected.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(RenewalHandOff.class)
  public RenewalHandOff pendingRenewalHandOff() {
    return new PendingRenewalHandOff();
  }

  /**
   * Stamped signatures until a qualified e-signature is connected.
   *
   * @param clock clock
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(SignatureProvider.class)
  public SignatureProvider stampedSignature(Clock clock) {
    return new StampedSignature(clock);
  }

  /**
   * No bank feed; the sources are uploaded.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(SubmittedSourceFeed.class)
  public SubmittedSourceFeed noSourceFeed() {
    return new NoSourceFeed();
  }
}
