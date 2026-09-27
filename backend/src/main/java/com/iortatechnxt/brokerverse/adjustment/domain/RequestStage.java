package com.iortatechnxt.brokerverse.adjustment.domain;

import java.util.Set;

/**
 * Stage of an endorsement request, mirrored from its {@code OPS_ENDORSEMENT} work case
 * (OPERATIONS_DESIGN section 7).
 */
public enum RequestStage {
  /** Being prepared by the requester. */
  DRAFT,
  /** Submitted; the Adjustment processor validates it. */
  FOR_VALIDATION,
  /** Validated; the Adjustment team leader approves it (four eyes). */
  FOR_APPROVAL,
  /** Ready for batch posting. */
  FOR_POSTING,
  /** Posted; the payments of the invoice wait for re-application by cashiering. */
  AWAITING_REAPPLICATION,
  /** Posted and complete. */
  POSTED,
  /** Returned to the requester with a reason (ADJID.005/007). */
  RETURNED,
  /** Cancelled before posting. */
  CANCELLED;

  private static final Set<RequestStage> RELEASED =
      Set.of(POSTED, CANCELLED, AWAITING_REAPPLICATION);
  private static final Set<RequestStage> EDITABLE = Set.of(DRAFT, RETURNED);
  private static final Set<RequestStage> VALIDATED =
      Set.of(FOR_APPROVAL, FOR_POSTING, AWAITING_REAPPLICATION, POSTED);

  /**
   * Whether the request still holds its invoice's lock (not posted, not cancelled).
   *
   * @return true while open
   */
  public boolean isOpen() {
    return !RELEASED.contains(this);
  }

  /**
   * Whether the requester may still change the request.
   *
   * @return true for drafts and returned requests
   */
  public boolean isEditable() {
    return EDITABLE.contains(this);
  }

  /**
   * Whether the request passed validation (validation slip, ADJID.018).
   *
   * @return true from approval onwards
   */
  public boolean isValidated() {
    return VALIDATED.contains(this);
  }
}
