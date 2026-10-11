package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.UserDataScopeGrant;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caches of the security module: the role to permission resolution, cleared by any role change, and
 * the data scope of a user, cleared by any user or grant change.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityCaches {

  /** Permissions of a role, keyed by role code. */
  public static final String ROLE_PERMISSIONS = "security-role-permissions";

  /** Data scope of a user, keyed by the lower-case user name (DATA_SCOPE_DESIGN.md). */
  public static final String DATA_SCOPE = "security-data-scope";

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

  /**
   * Declares the data scope cache.
   *
   * @return spec (15 minutes; any change of a user or of a data scope grant clears it)
   */
  @Bean
  public CacheSpec dataScopeCache() {
    return CacheSpec.of(DATA_SCOPE, TTL, AppUser.class, UserDataScopeGrant.class);
  }
}
