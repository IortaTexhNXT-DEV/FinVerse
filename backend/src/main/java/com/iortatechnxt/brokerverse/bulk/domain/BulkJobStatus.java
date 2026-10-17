package com.iortatechnxt.brokerverse.bulk.domain;

/** Status of a bulk upload. */
public enum BulkJobStatus {
  /** Parsed and validated; waiting for the user to commit (or submit) the valid rows. */
  VALIDATED,
  /** Submitted for approval: a second user approves (the valid rows are applied) or rejects. */
  SUBMITTED,
  /** Valid rows committed (some may have failed at commit). */
  COMPLETED,
  /** Discarded without committing. */
  CANCELLED,
  /** Rejected by the approver; nothing was applied. */
  REJECTED
}
