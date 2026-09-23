package com.iortatechnxt.finverse.receivables.domain;

/** Life cycle of an official receipt (maker-checker). */
public enum ReceiptStatus {
  PENDING_APPROVAL,
  APPROVED,
  REJECTED,
  CANCELLED,
  BOUNCED
}
