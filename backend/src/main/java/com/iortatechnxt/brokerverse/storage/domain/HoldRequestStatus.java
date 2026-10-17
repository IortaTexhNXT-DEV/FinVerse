package com.iortatechnxt.brokerverse.storage.domain;

/** Status of a legal hold request. */
public enum HoldRequestStatus {
  /** Waits for an approver. */
  PENDING,
  /** Approved and applied. */
  APPROVED,
  /** Rejected; nothing changed. */
  REJECTED
}
