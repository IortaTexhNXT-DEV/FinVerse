package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached role to permission resolution ({@link SecurityCaches#ROLE_PERMISSIONS}), used on every
 * authenticated request to build the user's authorities.
 */
@Service
public class RolePermissionLookup {

  private final RoleRepository roles;

  /**
   * Creates the lookup.
   *
   * @param roles role repository
   */
  public RolePermissionLookup(RoleRepository roles) {
    this.roles = roles;
  }

  /**
   * Permissions granted by a role.
   *
   * @param roleCode role code
   * @return permission names, sorted; empty for an unknown role
   */
  @Cacheable(cacheNames = SecurityCaches.ROLE_PERMISSIONS, key = "#roleCode")
  @Transactional(readOnly = true)
  public RolePermissions permissionsOf(String roleCode) {
    return new RolePermissions(
        roleCode,
        roles
            .findByCode(roleCode)
            .map(r -> r.getPermissions().stream().map(Enum::name).sorted().toList())
            .orElse(List.of()));
  }

  /**
   * Permissions of one role.
   *
   * @param roleCode role code
   * @param permissions permission names
   */
  public record RolePermissions(String roleCode, List<String> permissions) {

    /** Defensive copy. */
    public RolePermissions {
      permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
  }
}
