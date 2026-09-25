package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapters of the {@code nbadmin} ports, registered only when no module provides the port
 * (the same rule as the security and Operations port defaults).
 */
@Configuration(proxyBeanMethods = false)
public class NbadminPortDefaults {

  /**
   * Refuses external (portal) users until the {@code portal} module implements {@link
   * ExternalUserProvisioner} (decision D7, Employee Benefits wave E1-A).
   *
   * @return refusing adapter
   */
  @Bean
  @ConditionalOnMissingBean(ExternalUserProvisioner.class)
  public ExternalUserProvisioner noExternalUsers() {
    return new RefusingExternalUserProvisioner();
  }

  /** The refusing default: every call is refused with {@code EXTERNAL_USERS_NOT_AVAILABLE}. */
  static final class RefusingExternalUserProvisioner implements ExternalUserProvisioner {

    @Override
    public boolean available() {
      return false;
    }

    @Override
    public void validate(ExternalUserAction action, ExternalUserAccount account) {
      refuse();
    }

    @Override
    public void create(ExternalUserAccount account) {
      refuse();
    }

    @Override
    public void disable(ExternalUserAccount account) {
      refuse();
    }

    @Override
    public void enable(ExternalUserAccount account) {
      refuse();
    }

    private static void refuse() {
      throw new BusinessRuleException(
          NOT_AVAILABLE, "External (portal) users are not available: the portal is not installed");
    }
  }
}
