package com.iortatechnxt.brokerverse.nbadmin.domain;

/** What a user access request asks for (BRNB.085, BRD 3.3.5). */
public enum AccessRequestType {
  /** Create a user with roles and home branch. */
  CREATE_USER,
  /** Replace the roles of an existing user. */
  MODIFY_ROLES,
  /** Disable an existing user. */
  DISABLE_USER,
  /** Enable a disabled user. */
  ENABLE_USER
}
