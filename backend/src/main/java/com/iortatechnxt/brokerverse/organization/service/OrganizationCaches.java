package com.iortatechnxt.brokerverse.organization.service;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cache of the enterprise structure (companies and branches), cleared by any change of either. */
@Configuration(proxyBeanMethods = false)
public class OrganizationCaches {

  /**
   * Companies and branches, keyed {@code company:<id>}, {@code company-code:<code>}, {@code
   * branch:<id>} and {@code branches:<companyId>}.
   */
  public static final String UNITS = "organization-units";

  /**
   * Declares the cache.
   *
   * @return spec (1 hour; any company or branch change clears it)
   */
  @Bean
  public CacheSpec organizationUnitsCache() {
    return CacheSpec.of(UNITS, Duration.ofHours(1), Company.class, Branch.class);
  }
}
