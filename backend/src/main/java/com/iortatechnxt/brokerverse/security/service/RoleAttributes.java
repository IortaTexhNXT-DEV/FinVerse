package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The attributes of a role (group profile) as text, compared before and after a change for the
 * access change log (BRD 3.003.2, 4.003.1).
 */
final class RoleAttributes {

  private RoleAttributes() {}

  /**
   * Snapshot of a role.
   *
   * @param r role
   * @return attribute name to value (values may be null)
   */
  static Map<String, String> of(Role r) {
    Map<String, String> values = new HashMap<>();
    values.put("name", r.getName());
    values.put("description", r.getDescription());
    values.put("privilegeLevel", r.getPrivilegeLevel().name());
    values.put(
        "permissions",
        r.getPermissions().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
    return values;
  }
}
