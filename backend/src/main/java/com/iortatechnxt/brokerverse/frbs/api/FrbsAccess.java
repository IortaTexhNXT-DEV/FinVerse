package com.iortatechnxt.brokerverse.frbs.api;

/** Security expressions of the FRBS endpoints (ACCOUNTING_DISBURSEMENT_DESIGN 8.1). */
final class FrbsAccess {

  /** Reading service-fee runs and configuration. */
  static final String VIEW =
      "hasAnyAuthority('FRBS_REPORT_VIEW', 'SERVICE_FEE_MANAGE', 'SERVICE_FEE_APPROVE',"
          + " 'SERVICE_FEE_TAG')";

  /** Computing, recomputing, submitting and sending again (GL officer). */
  static final String MANAGE = "hasAuthority('SERVICE_FEE_MANAGE')";

  /** Approving a run (GL team lead). */
  static final String APPROVE = "hasAuthority('SERVICE_FEE_APPROVE')";

  /** Tagging lines released or liquidated. */
  static final String TAG = "hasAuthority('SERVICE_FEE_TAG')";

  /** Rates and recipients (configuration, team lead). */
  static final String SETUP = "hasAuthority('SERVICE_FEE_APPROVE')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private FrbsAccess() {}
}
