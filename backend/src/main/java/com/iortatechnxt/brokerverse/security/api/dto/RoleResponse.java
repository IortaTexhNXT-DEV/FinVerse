package com.iortatechnxt.brokerverse.security.api.dto;

import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.Set;

/**
 * Role view.
 *
 * @param id id
 * @param code code
 * @param name name
 * @param permissions permissions
 * @param active false when deactivated (grants nothing, BRD 3.002.3)
 * @param description description
 * @param privilegeLevel privilege level (UAM-NFR-40)
 */
public record RoleResponse(
    Long id,
    String code,
    String name,
    Set<Permission> permissions,
    boolean active,
    String description,
    PrivilegeLevel privilegeLevel) {

  /**
   * Maps an entity.
   *
   * @param r role
   * @return response
   */
  public static RoleResponse from(Role r) {
    return new RoleResponse(
        r.getId(),
        r.getCode(),
        r.getName(),
        r.getPermissions(),
        r.isActive(),
        r.getDescription(),
        r.getPrivilegeLevel());
  }
}
