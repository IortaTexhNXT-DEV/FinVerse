package com.iortatechnxt.brokerverse.claims.domain;

/** Maker-checker status of a claim document (reserve change, settlement, recovery). */
public enum DocumentStatus {
  /** Entered by the maker, waiting for a checker. */
  PENDING_APPROVAL,
  /** Approved and posted. */
  APPROVED,
  /** Rejected by the checker (final: the maker enters a new document). */
  REJECTED
}
