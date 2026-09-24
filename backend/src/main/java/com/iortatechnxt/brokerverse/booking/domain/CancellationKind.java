package com.iortatechnxt.brokerverse.booking.domain;

/** How a booked account is cancelled after issuance (BRNB.094; OPERATIONS_DESIGN section 5). */
public enum CancellationKind {
  /** Cancelled from inception: every premium component and the commission are returned. */
  FLAT,
  /** Cancelled from inception, documentary stamp tax retained. */
  FLAT_RETAIN_DST,
  /**
   * Cancelled mid-term: the unexpired part is returned, pro-rata (days) or short-period (the
   * insurer retains the short-period percentage of the months elapsed); DST is retained.
   */
  PARTIAL
}
