package com.iortatechnxt.brokerverse.receivables.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life cycle of a post-dated cheque received.
 *
 * <pre>
 * ON_HAND -&gt; DUE -&gt; DEPOSITED -&gt; CLEARED | BOUNCED
 * ON_HAND / DUE -&gt; RETURNED | REPLACED
 * DEPOSITED -&gt; DUE (the receipt raised on deposit was rejected)
 * </pre>
 */
public enum PdcStatus {
  ON_HAND,
  DUE,
  DEPOSITED,
  CLEARED,
  BOUNCED,
  RETURNED,
  REPLACED;

  /**
   * Whether the cheque is still held by the company (not yet banked or given back).
   *
   * @return true for ON_HAND and DUE
   */
  public boolean isHeld() {
    return this == ON_HAND || this == DUE;
  }

  /**
   * Statuses reachable from this one.
   *
   * @return allowed next statuses
   */
  public Set<PdcStatus> next() {
    return switch (this) {
      case ON_HAND -> EnumSet.of(DUE, DEPOSITED, RETURNED, REPLACED);
      case DUE -> EnumSet.of(DEPOSITED, RETURNED, REPLACED);
      case DEPOSITED -> EnumSet.of(CLEARED, BOUNCED, DUE);
      default -> EnumSet.noneOf(PdcStatus.class);
    };
  }
}
