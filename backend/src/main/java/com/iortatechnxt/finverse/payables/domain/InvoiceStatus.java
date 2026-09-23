package com.iortatechnxt.finverse.payables.domain;

/** Lifecycle of a supplier invoice (maker-checker). */
public enum InvoiceStatus {
  /** Captured by the maker, editable. */
  DRAFT,
  /** Submitted, waiting for a checker. */
  PENDING_APPROVAL,
  /** Approved and posted to the GL and the payables sub-ledger. */
  APPROVED,
  /** Withdrawn before approval. */
  CANCELLED
}
