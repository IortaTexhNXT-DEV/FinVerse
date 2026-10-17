package com.iortatechnxt.brokerverse.nbadmin.domain;

/**
 * What a user access request asks for (BRNB.085, BRD 1.002-1.005 and 3.002; PMADD05).
 *
 * <p>The group-profile types ({@link #isGroupProfile()}) are decided by one or more approvers in
 * order and then implemented by the System Administrator, unless {@code UAM_ROLE_APPLY_ON_APPROVAL}
 * applies them at approval (UQ03).
 */
public enum AccessRequestType {
  /** Create (enrol) a user with home branch; the group profiles come from UIDM-ISC (BRD 1.002). */
  CREATE_USER,
  /**
   * Replace the roles of an existing user: no longer requested, kept for the history; the group
   * profiles of a user are assigned through UIDM-ISC.
   */
  MODIFY_ROLES,
  /** Change the data of an existing user; the group profiles come from UIDM-ISC (BRD 1.003). */
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
   * Whether the request sets the details of a user (the roles of a request made before UIDM-ISC
   * assigned the group profiles, and the data scope).
   *
   * @return true for a creation or a modification of a user
   */
  public boolean carriesUserRoles() {
    return this == CREATE_USER || this == MODIFY_ROLES || this == MODIFY_USER;
  }
}
