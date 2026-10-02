package com.iortatechnxt.brokerverse.account.domain;

/** Status of an account, mirrored from its NB_ACCOUNT work case (BRNB.022/115). */
public enum AccountStatus {
  /** Being prepared by Marketing. */
  DRAFT,
  /** Submitted to Processing. */
  SUBMITTED,
  /** Returned by Processing with a reason. */
  RETURNED_TO_MARKETING,
  /** Validated; waiting for payment or client confirmation. */
  AWAITING_PAYMENT,
  /** Payment gate passed; ready to be placed with the insurer. */
  READY_FOR_PLACEMENT,
  /** Placement slip sent to the insurer. */
  PLACED,
  /** Returned by the insurer with remarks. */
  RETURNED_BY_INSURER,
  /** Placement cancelled before issuance (may be reactivated). */
  PLACEMENT_CANCELLED,
  /** Policy issued by the insurer; ready for booking. */
  POLICY_ISSUED,
  /** Booked (GL entry and service invoice). */
  BOOKED,
  /** Cancelled after issuance (BRNB.094). */
  CANCELLED,
  /** Voided while in process: soft delete (BRNB.019). */
  VOIDED
}
