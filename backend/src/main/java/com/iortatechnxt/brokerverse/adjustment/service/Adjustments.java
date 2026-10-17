package com.iortatechnxt.brokerverse.adjustment.service;

/** Names shared by the adjustment services (module, entity, workflow, references). */
public final class Adjustments {

  /** Module name on ledger movements, locks, events and hand-offs. */
  public static final String MODULE = "ADJUSTMENT";

  /** Entity type of the work case, the attachments and the audit trail. */
  public static final String ENTITY = "EndorsementRequest";

  /** Workflow of endorsement requests (OPERATIONS_DESIGN section 7). */
  public static final String WORKFLOW = "OPS_ENDORSEMENT";

  /** Notification event of request status changes (V762). */
  public static final String STATUS_EVENT = "ADJ_REQUEST_STATUS";

  /** Alert of cumulative adjustments over the baseline (ADJID.028). */
  public static final String OVER_BASELINE = "ADJ_OVER_BASELINE";

  /** Base route of a request page. */
  public static final String LINK = "/adjustment/requests/";

  private Adjustments() {}

  /**
   * The ledger and accounting source reference of a request.
   *
   * @param requestNo request number
   * @return {@code ADJ:<request>}
   */
  public static String sourceRef(String requestNo) {
    return "ADJ:" + requestNo;
  }

  /**
   * The route of a request page.
   *
   * @param id request id
   * @return frontend route
   */
  public static String link(Long id) {
    return LINK + id;
  }
}
