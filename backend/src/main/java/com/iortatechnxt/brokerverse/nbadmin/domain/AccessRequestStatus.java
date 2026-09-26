package com.iortatechnxt.brokerverse.nbadmin.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status of a user access request (BRD 1.002-1.009, 2.002, 3.002; USER_ACCESS_DESIGN section 4.3).
 */
public enum AccessRequestStatus {
  /** Saved, not submitted; visible to its creator only (BRD 1.002.1.2). */
  DRAFT,
  /** Waiting for the chosen approver (group profiles: the current approver in order). */
  PENDING,
  /** Approved once; waiting for a second approval because of a risk flag (UAM-NFR-40). */
  PENDING_SECOND,
  /** Returned to the requester with remarks, to be corrected and resubmitted (BRD 2.002.7). */
  RETURNED,
  /** Cancelled by the creator (or the approver of a scheduled request) with a reason. */
  CANCELLED,
  /** Rejected with a reason. */
  REJECTED,
  /** Approved and applied. */
  APPROVED,
  /** Approved; applied by the daily job on its effective date (UAM-NFR-14). */
  SCHEDULED,
  /** Group profile: approved, waiting for the System Administrator (BRD-11 p.6; UQ03). */
  FOR_IMPLEMENTATION,
  /** Group profile: implemented by the System Administrator. */
  IMPLEMENTED;

  /** Statuses of a request that is still open (one open request per user or role, R3). */
  public static final Set<AccessRequestStatus> OPEN =
      EnumSet.of(PENDING, PENDING_SECOND, RETURNED, SCHEDULED, FOR_IMPLEMENTATION);

  /** Statuses in which an approver decides. */
  public static final Set<AccessRequestStatus> AWAITING_DECISION =
      EnumSet.of(PENDING, PENDING_SECOND);

  /** Statuses from which a request can be cancelled (BRD 1.007; UQ06 for SCHEDULED). */
  public static final Set<AccessRequestStatus> CANCELLABLE =
      EnumSet.of(DRAFT, PENDING, PENDING_SECOND, RETURNED, SCHEDULED);

  /** Statuses in which the creator edits the request (draft, or correction of a return). */
  public static final Set<AccessRequestStatus> EDITABLE = EnumSet.of(DRAFT, RETURNED);
}
