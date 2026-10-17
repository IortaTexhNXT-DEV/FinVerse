package com.iortatechnxt.brokerverse.issuance.domain;

/** Life of a received e-policy (BRNB.073/074). */
public enum EpolicyStatus {
  /** Stored on the account; not yet extracted. */
  RECEIVED,
  /** Extracted; waiting for the review (extraction review task). */
  REVIEW,
  /** Reviewed: the policy numbers are on the account. */
  CONFIRMED,
  /** Rejected at review (wrong document or account). */
  REJECTED
}
