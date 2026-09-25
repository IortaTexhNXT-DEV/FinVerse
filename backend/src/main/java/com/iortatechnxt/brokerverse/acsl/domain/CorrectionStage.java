package com.iortatechnxt.brokerverse.acsl.domain;

/** Stage of a correction entry, mirrored from its work case (workflow ACSL_CORRECTION of V890). */
public enum CorrectionStage {
  /** Waiting for the team leader to assign a preparer (ACSL 2.7.0). */
  ASSIGNED,
  /** Being prepared (ACSL 2.9.0). */
  DRAFT,
  /** With the team leader for review (ACSL 2.10.0). */
  FOR_REVIEW,
  /** With the approver (ACSL 2.11.0). */
  FOR_APPROVAL,
  /** Approved and posted (ACSL 2.15.0). */
  POSTED,
  /** Cancelled before review. */
  CANCELLED;

  /**
   * Whether the lines may still change.
   *
   * @return true in DRAFT
   */
  public boolean isEditable() {
    return this == DRAFT;
  }
}
