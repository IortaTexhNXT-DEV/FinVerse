package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status of a masterlist record, mirrored by the stage of its {@code SBM_POLICY} work case (FRS
 * BRD-12 section 5.1).
 */
public enum SbmPolicyStatus {
  /** Loaded from a source; not yet confirmed. */
  RECEIVED,
  /** Details confirmed by a user. */
  VALIDATED,
  /** Classified and bucketed by a processing run. */
  CLASSIFIED,
  /** Policy review and IAAF in progress. */
  IN_REVIEW,
  /** Renewable; waits for the expiry scan. */
  FOR_RENEWAL,
  /** A handler decides. */
  FOR_MANUAL_DISPOSITION,
  /** Non-renewal bucket or manual exclusion. */
  EXCLUDED,
  /** Handed to the Renewal module. */
  RENEWAL_IN_PROGRESS,
  /** Placement slip of the renewal account sent. */
  PLACED,
  /** Renewal booked. */
  BOOKED,
  /** Declined, lost or expired unrenewed. */
  NOT_RENEWED,
  /** No further action. */
  CLOSED;

  private static final Set<SbmPolicyStatus> PROCESSABLE =
      EnumSet.of(
          RECEIVED, VALIDATED, CLASSIFIED, IN_REVIEW, FOR_RENEWAL, FOR_MANUAL_DISPOSITION, EXCLUDED);

  /**
   * Whether a processing run may still classify the record (before the renewal hand-off).
   *
   * @return true before the renewal
   */
  public boolean isProcessable() {
    return PROCESSABLE.contains(this);
  }

  /**
   * Whether the record is in or after the renewal.
   *
   * @return true from the hand-off on
   */
  public boolean isInRenewal() {
    return this == RENEWAL_IN_PROGRESS || this == PLACED || this == BOOKED || this == NOT_RENEWED;
  }
}
