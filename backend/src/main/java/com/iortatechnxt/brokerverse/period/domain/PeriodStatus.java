package com.iortatechnxt.brokerverse.period.domain;

/**
 * Accounting period lifecycle: FUTURE → OPEN → CLOSING → CLOSED (→ REOPENED → CLOSED).
 *
 * <ul>
 *   <li>FUTURE: defined, no posting.
 *   <li>OPEN: normal posting.
 *   <li>CLOSING: soft close; only system generated and adjustment journals may post.
 *   <li>CLOSED: hard close; no posting.
 *   <li>REOPENED: temporarily open for authorized adjustments only; must be closed again.
 * </ul>
 */
public enum PeriodStatus {
  FUTURE,
  OPEN,
  CLOSING,
  CLOSED,
  REOPENED
}
