package com.iortatechnxt.brokerverse.nbadmin.domain;

/** Why an access request needs a second approval (UAM-NFR-40; USER_ACCESS_DESIGN section 4.2). */
public enum AccessRiskFlag {
  /** The request raises a user or a role to a HIGH or ADMIN privilege level. */
  PRIVILEGE_INCREASE,
  /** The request was submitted or approved outside UAM_WORKING_HOURS. */
  OUTSIDE_HOURS
}
