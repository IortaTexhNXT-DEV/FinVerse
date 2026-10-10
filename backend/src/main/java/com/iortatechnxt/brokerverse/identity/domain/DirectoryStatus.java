package com.iortatechnxt.brokerverse.identity.domain;

/** Status of an account in the Enterprise SSO platform (BDOI FRS FRUM.002.02). */
public enum DirectoryStatus {
  ACTIVE,
  INACTIVE,
  LOCKED,
  DISABLED,
  DEACTIVATED;

  /**
   * Whether the status lets the user reach the system.
   *
   * @return true only for ACTIVE
   */
  public boolean grantsAccess() {
    return this == ACTIVE;
  }
}
