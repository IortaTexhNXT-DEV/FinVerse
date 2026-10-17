package com.iortatechnxt.brokerverse.brokerclaims.domain;

/**
 * Fixed lifecycle phase of a claim (CLAIMS_BROKING_DESIGN 8.1). The 18 BDOI statuses are values of
 * the list {@code BCL_CLAIM_STATUS}, each mapped to one phase by its {@code phase} attribute; the
 * phase drives the code and is mirrored by the stage of workflow {@code BCL_CLAIM}, whose stage
 * codes are the phase names (BRCLM.010/035).
 */
public enum ClaimPhase {
  /** Newly filed, with or without complete documents (statuses 1-2). */
  NEW,
  /** Being handled; waiting on the insurer, adjuster, claimant, assured or BDOI (statuses 3-16). */
  IN_PROGRESS,
  /** Temporarily closed; can resume and keeps ageing (statuses 17-18, CLQ06). */
  TEMP_CLOSED,
  /** Permanently closed by a closing settlement type; reopened only with a reason. */
  CLOSED;

  /**
   * Whether a claim in this phase is outstanding (listed and aged by the outstanding and ageing
   * reports, BRCLM.025-031).
   *
   * @return true unless CLOSED
   */
  public boolean isOutstanding() {
    return this != CLOSED;
  }

  /**
   * Stage code of workflow {@code BCL_CLAIM} for this phase.
   *
   * @return stage code (the phase name)
   */
  public String stageCode() {
    return name();
  }
}
