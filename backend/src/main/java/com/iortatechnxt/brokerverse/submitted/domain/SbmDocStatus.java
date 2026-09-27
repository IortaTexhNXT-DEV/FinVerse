package com.iortatechnxt.brokerverse.submitted.domain;

/** Status of an IAAF or a Terms of Reference (workflows SBM_IAAF and SBM_TOR). */
public enum SbmDocStatus {
  /** Being prepared. */
  DRAFT,
  /** At an approval level. */
  FOR_APPROVAL,
  /** Returned to the preparer. */
  RETURNED,
  /** Every level approved; signed PDF ready. */
  APPROVED,
  /** IAAF sent to the bank counterpart. */
  ISSUED,
  /** TOR opened or downloaded by the Account Officer. */
  RELEASED,
  /** Cancelled. */
  CANCELLED
}
