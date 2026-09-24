package com.iortatechnxt.brokerverse.tax.domain;

/**
 * Filing status of a tax return: DRAFT (computed, editable by refresh) → FILED (submitted to the
 * authority, figures frozen) → PAID (remittance posted). A DRAFT can be CANCELLED.
 */
public enum ReturnStatus {
  DRAFT,
  FILED,
  PAID,
  CANCELLED
}
