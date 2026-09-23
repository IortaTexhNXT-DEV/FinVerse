package com.iortatechnxt.finverse.underwriting.domain;

/** Lifecycle of a quotation. */
public enum QuotationStatus {
  /** Being prepared or re-iterated; editable. */
  DRAFT,
  /** Submitted, awaiting a different user's approval. */
  PENDING_APPROVAL,
  /** Approved terms that may be offered and converted. */
  APPROVED,
  /** Declined by the checker. */
  REJECTED,
  /** Converted into a policy. */
  CONVERTED,
  /** Validity lapsed before conversion. */
  EXPIRED
}
