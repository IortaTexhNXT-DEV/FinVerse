package com.iortatechnxt.brokerverse.adjustment.api;

/** Security expressions of the adjustment endpoints (OPERATIONS_DESIGN 6.1). */
final class AdjustmentAccess {

  /** Reading requests, batches and write-offs: the adjustment roles and Operations viewers. */
  static final String VIEW =
      "hasAnyAuthority('ADJ_REQUEST', 'ADJ_PROCESS', 'ADJ_APPROVE', 'ADJ_POST', 'OPS_VIEW')";

  /** Raising, changing and submitting requests: Marketing and the Adjustment processor. */
  static final String REQUEST = "hasAnyAuthority('ADJ_REQUEST', 'ADJ_PROCESS')";

  /** Endorsement slip (ADJID.015, MKTID.008). */
  static final String SLIP =
      "hasAnyAuthority('ADJ_REQUEST', 'ADJ_PROCESS', 'ADJ_APPROVE', 'ADJ_POST')";

  /** Validation (Adjustment processor). */
  static final String PROCESS = "hasAuthority('ADJ_PROCESS')";

  /** Approval (Adjustment team leader). */
  static final String APPROVE = "hasAuthority('ADJ_APPROVE')";

  /** Posting, batches, re-application and the minimal balance file. */
  static final String POST = "hasAuthority('ADJ_POST')";

  /** Returning requests out of a batch (the workflow checks the stage's permission). */
  static final String RETURN = "hasAnyAuthority('ADJ_PROCESS', 'ADJ_APPROVE', 'ADJ_POST')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private AdjustmentAccess() {}
}
