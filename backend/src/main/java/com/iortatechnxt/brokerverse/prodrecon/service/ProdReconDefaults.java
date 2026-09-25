package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.prodrecon.service.port.EarlyIncentiveRules;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapter of the production reconciliation seams: while no early incentive rule is
 * connected (remittance, OQ23), no rule applies and PRCID.028 lines read "rule pending". Declaring
 * any other {@link EarlyIncentiveRules} bean replaces it.
 */
@Configuration(proxyBeanMethods = false)
public class ProdReconDefaults {

  /**
   * The pending rule source.
   *
   * @return a source without rules
   */
  @Bean
  @ConditionalOnMissingBean(EarlyIncentiveRules.class)
  public EarlyIncentiveRules pendingEarlyIncentiveRules() {
    return (companyId, subject) -> Optional.empty();
  }
}
