package com.iortatechnxt.brokerverse.nbadmin.domain;

/**
 * What a user access request asks for (BRNB.085, BRD 1.002-1.005 and 3.002; PMADD05).
 *
 * <p>The group-profile types ({@link #isGroupProfile()}) are decided by one or more approvers in
 * order and then implemented by the System Administrator, unless {@code UAM_ROLE_APPLY_ON_APPROVAL}
 * applies them at approval (UQ03).
 */
public enum AccessRequestType {
  /** Create (enrol) a user with roles and home branch (BRD 1.002). */
  CREATE_USER,
  /** Replace the roles of an existing user (kept for compatibility; see MODIFY_USER). */
  MODIFY_ROLES,
  /** Change the data and / or the roles of an existing user in one request (BRD 1.003). */
  MODIFY_USER,
  /** Disable (deactivate) an existing user (BRD 1.004). */
  DISABLE_USER,
  /** Enable (reactivate) a disabled or locked user (BRD 1.005). */
  ENABLE_USER,
  /** Add or remove permissions of a role, and change its name / level (PMADD05, BRD 3.002.2). */
  MODIFY_ROLE_PERMISSIONS,
  /** Create a group profile (BRD 3.002.1). */
  CREATE_ROLE,
  /** Deactivate a group profile (BRD 3.002.3). */
  DEACTIVATE_ROLE,
  /** Reactivate a group profile (BRD 3.002.4). */
  REACTIVATE_ROLE;

  /**
   * Whether the request is about a group profile (role) rather than a user.
   *
   * @return true for the role types
   */
  public boolean isGroupProfile() {
    return this == MODIFY_ROLE_PERMISSIONS
        || this == CREATE_ROLE
        || this == DEACTIVATE_ROLE
        || this == REACTIVATE_ROLE;
  }

  /**
   * Whether the request sets the roles of a user.
   *
   * @return true for a creation or a modification of a user
   */
  public boolean carriesUserRoles() {
    return this == CREATE_USER || this == MODIFY_ROLES || this == MODIFY_USER;
  }
}
