package com.iortatechnxt.brokerverse.payables.domain;

/** Lifecycle of a payment voucher. */
public enum VoucherStatus {
  /** Captured by the maker, editable. */
  DRAFT,
  /** Submitted, waiting for a checker. */
  PENDING_APPROVAL,
  /** Approved: posted, payables matched, cheque number allocated. */
  APPROVED,
  /** Withdrawn before approval. */
  CANCELLED,
  /** Approved payment reversed (cheque voided before presentation or PDC cancelled). */
  VOIDED
}
