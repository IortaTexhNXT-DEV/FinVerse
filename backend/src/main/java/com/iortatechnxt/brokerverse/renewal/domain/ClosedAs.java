package com.iortatechnxt.brokerverse.renewal.domain;

/** How a candidate closed (RENEWAL_DESIGN section 4.1). */
public enum ClosedAs {
  /** The renewal was booked. */
  RENEWED,
  /** Not for Renewal, letter sent. */
  NOT_RENEWED,
  /** Lost business, or the New Business path was declined. */
  LOST,
  /** Expired without acceptance (BRRN.037). */
  EXPIRED_UNRENEWED,
  /** Renewed under another invoice. */
  BOOKED_OTHER_INVOICE;
}
