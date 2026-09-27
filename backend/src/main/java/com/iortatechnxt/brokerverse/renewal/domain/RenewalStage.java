package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stage of a renewal candidate, mirrored from its {@code RNW_CASE} work case (RENEWAL_DESIGN
 * section 7.1; FRS BRD-6 section 5).
 */
public enum RenewalStage {
  /** Extracted, not yet initiated (BRRN.021). */
  EXTRACTED("Extracted"),
  /** Initiated; checks, bucket and matrix being applied. */
  EVALUATING("Evaluating"),
  /** Waiting for a Marketing TL to assign it (Unassigned Disposition). */
  UNASSIGNED("Unassigned Disposition"),
  /** Transfer to another Marketing unit requested. */
  TRANSFER_PENDING("Transfer Pending"),
  /** With the Marketing AO for disposition (or returned). */
  FOR_DISPOSITION("For Disposition"),
  /** Dispositioned; Team Leader review in progress. */
  FOR_TL_REVIEW("Review in Progress"),
  /** Renewal on the New Business path (quotation or PRF). */
  NB_PATH("New Business Path"),
  /** Posted For Renewal; waiting for a Processing Officer. */
  FOR_PROCESSING("For Processing"),
  /** With a Processing Officer. */
  IN_PROCESSING("In Processing"),
  /** Sent to the insurer; waiting for its response. */
  WITH_INSURER("With Insurer"),
  /** Renewal Advice can be generated. */
  RA_READY("RA Ready"),
  /** Renewal Advice generated; Marketing side locked. */
  RA_GENERATED("RA Generated"),
  /** Renewal Advice sent; awaiting the client's response. */
  RA_SENT("Awaiting Response"),
  /** Accepted by the client. */
  ACCEPTED("Accepted"),
  /** Renewal account in placement, issuance and booking. */
  FOR_PLACEMENT_BOOKING("For Placement and Booking"),
  /** Not for Renewal; waiting for the NFR or NAL. */
  LETTER_PENDING("Letter Pending"),
  /** Renewal booked (terminal). */
  RENEWED("Renewed"),
  /** Closed without a renewal booked here (terminal). */
  CLOSED("Closed");

  private final String label;

  RenewalStage(String label) {
    this.label = label;
  }

  /**
   * The stage as users read it (the stage name of the workflow).
   *
   * @return label
   */
  public String label() {
    return label;
  }

  private static final Set<RenewalStage> TERMINAL = EnumSet.of(RENEWED, CLOSED);

  private static final Set<RenewalStage> LOCKED =
      EnumSet.of(RA_GENERATED, RA_SENT, ACCEPTED, FOR_PLACEMENT_BOOKING, RENEWED);

  private static final Set<RenewalStage> PROCESSING =
      EnumSet.of(FOR_PROCESSING, IN_PROCESSING, WITH_INSURER, RA_READY, RA_GENERATED, RA_SENT);

  /**
   * Whether the candidate is still open.
   *
   * @return false for RENEWED and CLOSED
   */
  public boolean isOpen() {
    return !TERMINAL.contains(this);
  }

  /**
   * Whether the Marketing side is locked by the Renewal Advice (BRD 2.004.9).
   *
   * @return true from RA_GENERATED on
   */
  public boolean isMarketingLocked() {
    return LOCKED.contains(this);
  }

  /**
   * Whether the stage belongs to Processing (Processing Worklist).
   *
   * @return true for the processing stages
   */
  public boolean isProcessing() {
    return PROCESSING.contains(this);
  }
}
