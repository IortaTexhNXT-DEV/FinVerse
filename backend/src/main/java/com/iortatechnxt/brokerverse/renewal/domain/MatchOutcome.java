package com.iortatechnxt.brokerverse.renewal.domain;

/** How an insurer response row matched its renewal (BRRN.022/035). */
public enum MatchOutcome {
  /** Reference and policy number match. */
  MATCHED,
  /** Unknown or missing renewal reference. */
  REF_MISMATCH,
  /** The policy number differs from the renewal's. */
  POLICY_MISMATCH,
  /** Conflicting responses for one renewal. */
  AMBIGUOUS;
}
