package com.iortatechnxt.brokerverse.security.domain;

/**
 * Activity of an access change log row (BRD 4.003.1, audit report sample D): what was done to a
 * user or a role (group profile).
 */
public enum AccessChangeActivity {
  CREATE_USER,
  MODIFY_USER,
  ROLES_CHANGED,
  DISABLE_USER,
  ENABLE_USER,
  UNLOCK,
  PASSWORD_RESET,
  CREATE_ROLE,
  ROLE_PERMISSIONS,
  DEACTIVATE_ROLE,
  REACTIVATE_ROLE
}
