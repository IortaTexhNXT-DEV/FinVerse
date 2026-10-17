package com.iortatechnxt.brokerverse.configpromo.domain;

/** Lifecycle of a configuration import. */
public enum ImportStatus {
  /** Uploaded, checked and dry run done; the preparer may change the options and submit. */
  CHECKED,
  /** Waiting for the approval of a second user. */
  SUBMITTED,
  /** Applied and reconciled. */
  APPLIED,
  /** Rejected by the approver. */
  REJECTED,
  /** The apply failed; nothing of it was kept. */
  FAILED,
  /** Withdrawn by the preparer. */
  CANCELLED
}
