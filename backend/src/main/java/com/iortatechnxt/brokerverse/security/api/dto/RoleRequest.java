package com.iortatechnxt.brokerverse.security.api.dto;

import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Role maintenance request. {@code code} is ignored on update. The description and privilege level
 * (BRD 3.002; UAM-NFR-40) are optional: null keeps the current value.
 *
 * @param code code
 * @param name name
 * @param permissions permissions
 * @param description description of the group profile
 * @param privilegeLevel privilege level (second approval of risky changes)
 */
public record RoleRequest(
    @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Z0-9_]+") String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull Set<Permission> permissions,
    @Size(max = 500) String description,
    PrivilegeLevel privilegeLevel) {

  /**
   * The request without description and privilege level.
   *
   * @param code code
   * @param name name
   * @param permissions permissions
   */
  public RoleRequest(String code, String name, Set<Permission> permissions) {
    this(code, name, permissions, null, null);
  }
}
