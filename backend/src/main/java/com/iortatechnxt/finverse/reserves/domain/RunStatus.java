package com.iortatechnxt.finverse.reserves.domain;

/** Life cycle of a valuation run: PREVIEW → PENDING_APPROVAL → APPROVED → POSTED. */
public enum RunStatus {
  /** Calculated, may be recalculated or submitted by the preparer. */
  PREVIEW,
  /** Submitted, waiting for a checker. */
  PENDING_APPROVAL,
  /** Approved by a checker, ready to post. */
  APPROVED,
  /** Movement journals posted to the ledger. */
  POSTED,
  /** Discarded before posting, or reversed after posting. */
  CANCELLED
}
