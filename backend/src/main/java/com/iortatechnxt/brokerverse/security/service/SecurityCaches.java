package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cache of the role to permission resolution, cleared by any role change. */
@Configuration(proxyBeanMethods = false)
public class SecurityCaches {

  /** Permissions of a role, keyed by role code. */
  public static final String ROLE_PERMISSIONS = "security-role-permissions";

  private static final Duration TTL = Duration.ofMinutes(15);

  /**
   * Declares the cache.
   *
   * @return spec (15 minutes; any role or grant change clears it)
   */
  @Bean
  public CacheSpec rolePermissionsCache() {
    return CacheSpec.of(ROLE_PERMISSIONS, TTL, Role.class);
  }
}
