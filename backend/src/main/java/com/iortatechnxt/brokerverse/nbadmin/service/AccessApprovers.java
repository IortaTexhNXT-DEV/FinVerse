package com.iortatechnxt.brokerverse.nbadmin.service;

/**
 * The approval right of the group-profile changes (BRD 3.002.x; FR-UA-044): any enabled holder
 * other than the maker decides a change. Users are maintained in UIDM-ISC, so no approver is chosen
 * for a request about a user.
 */
public final class AccessApprovers {

  /** Approval right of the access requests. */
  public static final String ACCESS_APPROVE = "ACCESS_APPROVE";

  private AccessApprovers() {}
}
