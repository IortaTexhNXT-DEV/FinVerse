package com.iortatechnxt.finverse.security.api.dto;

import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.security.domain.Role;
import java.util.Set;

/**
 * Role view.
 *
 * @param id id
 * @param code code
 * @param name name
 * @param permissions permissions
 */
public record RoleResponse(Long id, String code, String name, Set<Permission> permissions) {

  /**
   * Maps an entity.
   *
   * @param r role
   * @return response
   */
  public static RoleResponse from(Role r) {
    return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getPermissions());
  }
}
