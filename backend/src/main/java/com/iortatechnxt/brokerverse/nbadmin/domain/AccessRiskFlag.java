package com.iortatechnxt.brokerverse.nbadmin.domain;

/** Why an access request needs a second approval (UAM-NFR-40; USER_ACCESS_DESIGN section 4.2). */
public enum AccessRiskFlag {
  /** The request raises a user or a role to a HIGH or ADMIN privilege level. */
  PRIVILEGE_INCREASE("privilege increase"),
  /** The request was submitted or approved outside UAM_WORKING_HOURS. */
  OUTSIDE_HOURS("outside working hours");

  private final String label;

  AccessRiskFlag(String label) {
    this.label = label;
  }

  /**
   * The flag in words, as users read it in notices and alerts.
   *
   * @return label
   */
  public String label() {
    return label;
  }
}
