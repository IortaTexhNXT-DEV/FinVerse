package com.iortatechnxt.brokerverse.opsledger.api;

/** Security expressions of the Operations ledger endpoints (OPERATIONS_DESIGN 6.1). */
final class OpsAccess {

  /** Operations home, invoice search and 360, extract repository, hand-offs. */
  static final String VIEW = "hasAuthority('OPS_VIEW')";

  /** Interfaces: feeds, runs, uploads and replays (IT). */
  static final String FLOWIN = "hasAuthority('FLOWIN_MANAGE')";

  /** The in-app Disbursement queue. */
  static final String DISBURSEMENT = "hasAuthority('DISB_PROCESS')";

  /** Closing a hand-off: the team that does the work, or the interface administrators. */
  static final String HANDOFF_CLOSE =
      "hasAnyAuthority('CASH_RECEIPT', 'CASH_DISPOSITION', 'FLOWIN_MANAGE')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private OpsAccess() {}
}
