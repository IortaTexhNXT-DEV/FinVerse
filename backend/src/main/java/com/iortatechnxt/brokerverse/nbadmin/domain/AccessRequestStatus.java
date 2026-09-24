package com.iortatechnxt.brokerverse.nbadmin.domain;

/** Status of a user access request. */
public enum AccessRequestStatus {
  /** Waiting for the Approver. */
  PENDING,
  /** Approved and applied. */
  APPROVED,
  /** Rejected with a comment. */
  REJECTED
}
