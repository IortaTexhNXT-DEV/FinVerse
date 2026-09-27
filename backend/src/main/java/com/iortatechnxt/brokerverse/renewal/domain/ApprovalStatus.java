package com.iortatechnxt.brokerverse.renewal.domain;

/**
 * Status of a request decided by a second user (package choices, corrections of the Renewal Advices
 * already sent).
 */
public enum ApprovalStatus {
  /** Waiting for the checker. */
  PENDING,
  /** Approved and applied. */
  APPROVED,
  /** Rejected with remarks. */
  REJECTED;
}
