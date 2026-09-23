package com.iortatechnxt.finverse.claims.domain;

/** Source of money recovered on a claim. */
public enum RecoveryType {
  /** Sale of damaged property taken over by the insurer. */
  SALVAGE,
  /** Amount recovered from a responsible third party or its insurer. */
  SUBROGATION
}
