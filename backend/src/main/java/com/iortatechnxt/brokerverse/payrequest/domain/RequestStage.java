package com.iortatechnxt.brokerverse.payrequest.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Stage of a request, mirrored from its work case (workflows PRQ_REFUND, PRQ_CASH_ADVANCE and
 * PRQ_CHECK_CANCEL of V890).
 */
public enum RequestStage {
  /** Being prepared by the requester. */
  DRAFT,
  /** Assigned to a preparer, or returned to the preparer (refunds). */
  PREPARING,
  /** With ACSL and Cashiering for validation (refunds of cancelled policies, MKT 1.11.0). */
  FOR_VALIDATION,
  /** With the Marketing reviewer (MKT 1.15.0). */
  FOR_REVIEW,
  /** With the Marketing approver (MKT 1.16.0). */
  FOR_APPROVAL,
  /** With Human Resources (cash advances, MKT 1.16.2). */
  HR_APPROVAL,
  /** Sent to Disbursement (MKT 2.24.0). */
  SENT_TO_DISBURSEMENT,
  /** Paid by Disbursement (MKT 1.20.0). */
  DISBURSED,
  /** A check cancellation being prepared. */
  REQUESTED,
  /** A check cancellation sent to Disbursement. */
  SENT,
  /** Cancelled or withdrawn before approval (MKT 1.17.0). */
  CANCELLED;

  private static final Set<RequestStage> EDITABLE = EnumSet.of(DRAFT, PREPARING, REQUESTED);
  private static final Set<RequestStage> ENDED = EnumSet.of(DISBURSED, SENT, CANCELLED);

  /**
   * Whether the requester may still change the request.
   *
   * @return true in DRAFT, PREPARING and REQUESTED
   */
  public boolean isEditable() {
    return EDITABLE.contains(this);
  }

  /**
   * Whether the request has ended.
   *
   * @return true when disbursed, sent (check cancellation) or cancelled
   */
  public boolean isEnded() {
    return ENDED.contains(this);
  }
}
