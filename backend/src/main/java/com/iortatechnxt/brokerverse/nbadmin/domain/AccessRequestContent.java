package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.Set;

/**
 * What an access request asks for.
 *
 * @param type request type
 * @param username user to create or change (user requests)
 * @param fullName full name (create)
 * @param email e-mail (create)
 * @param roleCodes roles (create, modify roles)
 * @param homeBranchId home branch (create)
 * @param justification business justification
 * @param permissionChange role and permissions (MODIFY_ROLE_PERMISSIONS, PMADD05), else null
 */
public record AccessRequestContent(
    AccessRequestType type,
    String username,
    String fullName,
    String email,
    Set<String> roleCodes,
    Long homeBranchId,
    String justification,
    RolePermissionChange permissionChange) {

  /** Defensive copy. */
  public AccessRequestContent {
    roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
  }

  /**
   * A user request (create, change roles, disable, enable).
   *
   * @param type request type
   * @param username user to create or change
   * @param fullName full name (create)
   * @param email e-mail (create)
   * @param roleCodes roles (create, modify roles)
   * @param homeBranchId home branch (create)
   * @param justification business justification
   */
  public AccessRequestContent(
      AccessRequestType type,
      String username,
      String fullName,
      String email,
      Set<String> roleCodes,
      Long homeBranchId,
      String justification) {
    this(type, username, fullName, email, roleCodes, homeBranchId, justification, null);
  }

  /**
   * A role-permission change request (PMADD05).
   *
   * @param change role and permissions added / removed
   * @param justification business justification
   * @return request content
   */
  public static AccessRequestContent rolePermissions(
      RolePermissionChange change, String justification) {
    return new AccessRequestContent(
        AccessRequestType.MODIFY_ROLE_PERMISSIONS,
        null,
        null,
        null,
        Set.of(),
        null,
        justification,
        change);
  }
}
