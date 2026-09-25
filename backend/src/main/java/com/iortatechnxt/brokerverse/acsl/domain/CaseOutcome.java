package com.iortatechnxt.brokerverse.acsl.domain;

/** The result of an ACSL case given to its requester (ACSL 2.5.4). */
public enum CaseOutcome {
  /** Confirmed (e.g. the cancelled premium and the insurer's return are in order). */
  CONFIRMED,
  /** Rejected, with the reason in the remarks. */
  REJECTED,
  /** Investigated, no action needed. */
  NO_ACTION,
  /** A correction entry was raised. */
  CORRECTION
}
