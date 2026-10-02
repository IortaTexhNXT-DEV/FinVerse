package com.iortatechnxt.brokerverse.nbadmin.domain;

/** Status of a bulk access request batch; it mirrors the statuses of its lines (BRD 1.009). */
public enum AccessBatchStatus {
  /** Lines saved as drafts. */
  DRAFT,
  /** Lines waiting for the approver. */
  PENDING,
  /** Lines returned for correction. */
  RETURNED,
  /** Every line cancelled. */
  CANCELLED,
  /** Every line rejected. */
  REJECTED,
  /** Every line approved (applied or scheduled). */
  APPROVED,
  /** Lines decided differently, or some failed when applied. */
  PARTIAL
}
