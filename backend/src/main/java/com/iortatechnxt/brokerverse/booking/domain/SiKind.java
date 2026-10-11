package com.iortatechnxt.brokerverse.booking.domain;

/** Service invoice or its credit (BRNB.100; ADJID.014). */
public enum SiKind {
  /** Service invoice. */
  INVOICE,
  /** Credit of a service invoice (return commission, cancellation). */
  CREDIT
}
