package com.iortatechnxt.brokerverse.underwriting.domain;

/** Lifecycle of a policy or endorsement document. */
public enum PolicyStatus {
  /** Being prepared (or returned by the checker); editable. */
  DRAFT,
  /** Submitted by the maker, awaiting a different user's approval. */
  PENDING_APPROVAL,
  /** Approved: premium accounted for, debit note issued. */
  APPROVED,
  /** Discarded draft, or policy cancelled by a cancellation endorsement. */
  CANCELLED
}
