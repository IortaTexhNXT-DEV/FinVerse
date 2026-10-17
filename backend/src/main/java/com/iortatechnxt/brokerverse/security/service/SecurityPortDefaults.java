package com.iortatechnxt.brokerverse.security.service;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default adapters of the security ports, registered only when no module provides the port (the
 * same rule as the Operations port defaults).
 */
@Configuration(proxyBeanMethods = false)
public class SecurityPortDefaults {

  /**
   * No approved group-profile request is known until {@code nbadmin} implements {@link
   * ApprovedRoleRequests} (USER_ACCESS_DESIGN section 9, wave U1-A).
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(ApprovedRoleRequests.class)
  public ApprovedRoleRequests noApprovedRoleRequests() {
    return (requestNo, roleCode) -> Optional.empty();
  }

  /**
   * No company or branch is known until {@code organization} implements {@link OrganizationUnits}:
   * only "All companies" can be granted and a branch without its company is refused.
   *
   * @return default adapter
   */
  @Bean
  @ConditionalOnMissingBean(OrganizationUnits.class)
  public OrganizationUnits noOrganizationUnits() {
    return new OrganizationUnits() {
      @Override
      public List<CompanyUnit> companies() {
        return List.of();
      }

      @Override
      public Optional<Long> companyOfBranch(Long branchId) {
        return Optional.empty();
      }
    };
  }
}
