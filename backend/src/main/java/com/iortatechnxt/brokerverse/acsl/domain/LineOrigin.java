package com.iortatechnxt.brokerverse.acsl.domain;

/** Where a correction line comes from (ACSL 2.9.1). */
public enum LineOrigin {
  /** Reverses a line of the original journal. */
  REVERSAL,
  /** Re-posts the reversed amount to the right account. */
  REPOST,
  /** Entered by the preparer. */
  MANUAL
}
