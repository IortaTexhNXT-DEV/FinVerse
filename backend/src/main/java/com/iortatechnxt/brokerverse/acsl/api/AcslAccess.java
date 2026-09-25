package com.iortatechnxt.brokerverse.acsl.api;

/** Security expressions of the ACSL endpoints (ACCOUNTING_DISBURSEMENT_DESIGN 8.1). */
final class AcslAccess {

  /** Reading cases, corrections, uploads and reconciliations. */
  static final String VIEW =
      "hasAnyAuthority('ACSL_VIEW', 'ACSL_PROCESS', 'ACSL_REVIEW', 'ACSL_APPROVE')";

  /** Investigating, preparing corrections, running reconciliations. */
  static final String PROCESS = "hasAuthority('ACSL_PROCESS')";

  /** Assigning cases and corrections, raising corrections (team leader). */
  static final String ASSIGN = "hasAuthority('ACSL_ASSIGN')";

  /** Reviewing corrections and configuring the GL-SL sub-ledgers (team leader). */
  static final String REVIEW = "hasAuthority('ACSL_REVIEW')";

  /** Approving corrections (team head). */
  static final String APPROVE = "hasAuthority('ACSL_APPROVE')";

  /** Uploading insurer SOAs. */
  static final String UPLOAD = "hasAuthority('ACSL_UPLOAD')";

  /** Payment application and reversal requests. */
  static final String APPLY = "hasAuthority('ACSL_APPLY')";

  /** Downloading ACSL reports. */
  static final String EXPORT = "hasAuthority('ACSL_REPORT_EXPORT')";

  /** Largest page served. */
  static final int MAX_PAGE = 200;

  private AcslAccess() {}
}
