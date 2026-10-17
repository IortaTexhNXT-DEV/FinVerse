package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;

/** Names shared by the service-fee services (FRBS 2.10; design 7.3). */
public final class ServiceFees {

  /** Module on gateway requests, accounting events and the audit trail. */
  public static final String MODULE = "FRBS";

  /** Entity type of the work case, the audit trail and the run attachments. */
  public static final String ENTITY = "FrbsServiceFeeRun";

  /** Entity type of a line's liquidation report. */
  public static final String LINE_ENTITY = "FrbsServiceFeeLine";

  /** Workflow of a run (V890). */
  public static final String WORKFLOW = "FRBS_SERVICE_FEE";

  /** Accrual event (V890; seed rule Dr 5614 / Cr 2250 in V999). */
  public static final String ACCRUAL_EVENT = "FRBS_SERVICE_FEE_ACCRUE";

  /** Amount component of the accrual. */
  public static final String AMOUNT = "AMOUNT";

  /** Actor of automatic tags. */
  public static final String SYSTEM = "SYSTEM";

  private static final String LINK = "/frbs/service-fee/runs/";

  private ServiceFees() {}

  /**
   * The route of a run page.
   *
   * @param id run id
   * @return frontend route
   */
  public static String link(Long id) {
    return LINK + id;
  }

  /**
   * A workflow note from an optional comment.
   *
   * @param comment comment, may be blank
   * @return note
   */
  public static TransitionNote note(String comment) {
    return TransitionNote.comment(comment == null || comment.isBlank() ? null : comment.strip());
  }
}
