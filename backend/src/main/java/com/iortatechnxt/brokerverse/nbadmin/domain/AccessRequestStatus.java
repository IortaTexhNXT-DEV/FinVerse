package com.iortatechnxt.brokerverse.nbadmin.domain;

/** Status of a user access request. */
public enum AccessRequestStatus {
  /** Waiting for the Approver. */
  PENDING,
  /** Approved and applied. */
  APPROVED,
  /** Rejected with a comment. */
  REJECTED,
  /** Returned to the requester with remarks, to be corrected and resubmitted (BASAU 2.4.1). */
  RETURNED
}
