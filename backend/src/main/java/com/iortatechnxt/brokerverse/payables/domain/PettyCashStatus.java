package com.iortatechnxt.brokerverse.payables.domain;

/** Status of petty cash disbursement vouchers and reimbursement claims. */
public enum PettyCashStatus {
  /** Captured, waiting for a checker. */
  PENDING_APPROVAL,
  /** Approved and posted. */
  APPROVED,
  /** Rejected by the checker (no posting). */
  REJECTED
}
