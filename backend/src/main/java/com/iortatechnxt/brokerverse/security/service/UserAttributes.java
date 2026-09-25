package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Role;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The attributes of a user as text, compared before and after a change for the access change log
 * (BRD 4.003.1). Keys are the attribute names written to the log.
 */
final class UserAttributes {

  private UserAttributes() {}

  /**
   * Snapshot of a user.
   *
   * @param u user
   * @return attribute name to value (values may be null)
   */
  static Map<String, String> of(AppUser u) {
    Map<String, String> values = new HashMap<>();
    values.put("fullName", u.getFullName());
    values.put("email", u.getEmail());
    values.put("homeBranchId", text(u.getHomeBranchId()));
    values.put(
        "authorizationLimit",
        u.getAuthorizationLimit() == null ? null : u.getAuthorizationLimit().toPlainString());
    values.put("windowsId", u.getWindowsId());
    values.put("businessUnitCode", u.getBusinessUnitCode());
    values.put("userLevel", u.getUserLevel());
    values.put(
        "roles",
        u.getRoles().stream().map(Role::getCode).sorted().collect(Collectors.joining(",")));
    values.put("enabled", String.valueOf(u.isEnabled()));
    return values;
  }

  private static String text(Object value) {
    return Objects.toString(value, null);
  }
}
