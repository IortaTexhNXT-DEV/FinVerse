package com.iortatechnxt.brokerverse.renewal.domain;

/** Kind of a controlled override (BRD 1.011; BRRN.023/031/035). */
public enum OverrideKind {
  /** The bucket (never to Clean with a failed check). */
  BUCKET,
  /** A system disposition. */
  DISPOSITION,
  /** The outstanding-premium flag. */
  OUTSTANDING_BALANCE,
  /** A mismatched insurer response. */
  INSURER_MISMATCH,
  /** Cancels the Renewal Advice and unlocks the account. */
  RA_UNLOCK,
  /** Any other failing check that blocks posting, the RA or acceptance. */
  CHECK;
}
