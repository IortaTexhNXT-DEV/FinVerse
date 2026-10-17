package com.iortatechnxt.brokerverse.brokerclaims.domain;

/** Kind of closure of a claim (BRCLM.005/035): a temporary closure can resume. */
public enum ClaimClosureKind {
  /** Temporary closure by a status of phase TEMP_CLOSED. */
  TEMPORARY,
  /** Permanent closure by a closing settlement type. */
  PERMANENT
}
