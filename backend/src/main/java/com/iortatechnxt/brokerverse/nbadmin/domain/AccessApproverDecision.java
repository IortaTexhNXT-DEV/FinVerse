package com.iortatechnxt.brokerverse.nbadmin.domain;

/** Decision of one approver of an access request (BRD 3.002.x "approver/s"). */
public enum AccessApproverDecision {
  /** Not decided yet. */
  PENDING,
  /** Approved. */
  APPROVED,
  /** Rejected (ends the request). */
  REJECTED,
  /** Returned to the requester (the chain restarts on resubmission). */
  RETURNED
}
