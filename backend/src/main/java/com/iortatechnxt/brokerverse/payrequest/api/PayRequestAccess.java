package com.iortatechnxt.brokerverse.payrequest.api;

/** Security expressions of the request endpoints (ACCOUNTING_DISBURSEMENT_DESIGN 8.1). */
final class PayRequestAccess {

  /** Reading requests: every request role and the auditors. */
  static final String VIEW =
      "hasAnyAuthority('PRQ_VIEW', 'PRQ_CREATE', 'PRQ_REVIEW', 'PRQ_APPROVE', 'PRQ_HR_APPROVE')";

  /** Raising, changing and submitting requests (Marketing AO / preparer, employee). */
  static final String CREATE = "hasAuthority('PRQ_CREATE')";

  /** Assigning refunds and entering handed-over validation results (reviewer / team leader). */
  static final String ASSIGN = "hasAuthority('PRQ_ASSIGN')";

  /** Reviewing requests and checking liquidations. */
  static final String REVIEW = "hasAuthority('PRQ_REVIEW')";

  /** Marketing or HR approval (the workflow checks which one the stage needs). */
  static final String APPROVE = "hasAnyAuthority('PRQ_APPROVE', 'PRQ_HR_APPROVE')";

  /** Accounts of the liquidation event roles (Comptrollership). */
  static final String ACCOUNTS = "hasAuthority('ACCOUNTING_RULE_MANAGE')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private PayRequestAccess() {}
}
