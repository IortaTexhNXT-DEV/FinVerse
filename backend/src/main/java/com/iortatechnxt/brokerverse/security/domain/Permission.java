package com.iortatechnxt.brokerverse.security.domain;

/**
 * Fine grained permissions checked with {@code @PreAuthorize("hasAuthority('...')")}.
 *
 * <p>Roles are bundles of permissions maintained in the database (see {@link Role}).
 */
public enum Permission {
  // Administration
  USER_MANAGE,
  ROLE_MANAGE,
  SYSTEM_PARAMETER_MANAGE,
  AUDIT_VIEW,

  // Master data (maker / checker)
  MASTER_VIEW,
  MASTER_MAINTAIN,
  MASTER_AUTHORIZE,

  // Journals
  JOURNAL_VIEW,
  JOURNAL_CREATE,
  JOURNAL_AUTHORIZE,
  JOURNAL_REVERSE,

  // Periods and closing
  PERIOD_MANAGE,
  YEAR_END_CLOSE,
  PERIOD_END_RUN,

  // Insurance operations (sub-ledgers)
  POLICY_VIEW,
  POLICY_MAINTAIN,
  POLICY_AUTHORIZE,
  CLAIM_VIEW,
  CLAIM_MAINTAIN,
  CLAIM_AUTHORIZE,
  REINSURANCE_VIEW,
  REINSURANCE_MAINTAIN,
  RECEIPT_PAYMENT_MAINTAIN,
  RECEIPT_PAYMENT_AUTHORIZE,

  // Accounting engine configuration
  ACCOUNTING_RULE_MANAGE,

  // Finance functions
  RECONCILIATION_MANAGE,
  BUDGET_MANAGE,
  CONSOLIDATION_RUN,

  // Reporting
  REPORT_VIEW,
  REPORT_FINANCIAL,
  DASHBOARD_VIEW,

  // Platform features: document attachments, alerts, system monitoring
  ATTACHMENT_VIEW,
  ATTACHMENT_MANAGE,
  ALERT_VIEW,
  ALERT_MANAGE,
  SYSTEM_MONITOR,

  // Fixed assets and investments: disposals, transfers, coupon receipts, maturities, sales and
  // fair value updates (masters use MASTER_*, month-end runs use PERIOD_END_RUN). See V671.
  ASSET_MANAGE,
  INVESTMENT_MANAGE,

  // Actuarial reserves: prepare and submit valuation runs (approve / post / cancel use
  // PERIOD_END_RUN). See V421.
  RESERVE_PREPARE,

  // Reinsurance checker: treaties, facultative placements and statements of account. See V300.
  REINSURANCE_AUTHORIZE,

  // Tax & statutory reporting: view worksheets, returns, certificates and IC schedules; maintain
  // tax masters and prepare, file and pay returns (masters are authorized with MASTER_AUTHORIZE).
  // See V701.
  TAX_VIEW,
  TAX_MANAGE,

  // Broking (BDOI New Business). See docs/architecture/BROKING_ARCHITECTURE.md section 3.7 and
  // V750.
  CLIENT_VIEW,
  CLIENT_MAINTAIN,
  CLIENT_APPROVE,
  QUOTE_VIEW,
  QUOTE_MAINTAIN,
  QUOTE_APPROVE,
  PROPOSAL_REQUEST,
  PROPOSAL_APPROVE,
  TSU_PROCESS,
  TSU_APPROVE,
  ACCOUNT_VIEW,
  ACCOUNT_MAINTAIN,
  ACCOUNT_PROCESS,
  BULK_PROCESS,
  PLACEMENT_MANAGE,
  BILLING_MANAGE,
  EPOLICY_MANAGE,
  EPOLICY_SEND,
  BOOKING_PROCESS,
  BOOKING_ADJUST,
  WORK_VIEW,
  WORK_ASSIGN,
  LOV_MANAGE,
  ACCESS_REQUEST,
  ACCESS_APPROVE,
  MESSAGE_VIEW,

  // Operations (BDOI BRD-2). See docs/architecture/OPERATIONS_DESIGN.md section 6.1 and V760.
  // Operations home, invoice 360 and Operations reports (view vs download / print, CSHID.018).
  OPS_VIEW,
  OPS_REPORT_VIEW,
  OPS_REPORT_EXPORT,
  // Cashiering (CSHID.001-027)
  CASH_RECEIPT,
  CASH_CANCEL,
  CASH_REINSTATE,
  CASH_APPROVE,
  CASH_APPLY,
  CASH_UPLOAD,
  CASH_DISPOSITION,
  CASH_DISPOSITION_APPROVE,
  CASH_SERIES_MANAGE,
  CASH_PRINT,
  // BIR 2307 (MKTID.010/013, CSHID.026/027)
  CWT_TAG,
  CWT_PROCESS,
  // Disbursement queue, default adapter until the Disbursement system is known (DBMID.001, OQ02)
  DISB_PROCESS,
  // Remittance (RMTID), holds and special remittance (MKTID.001-009)
  REMIT_EXTRACT,
  REMIT_PROCESS,
  REMIT_EXCLUDE,
  REMIT_APPROVE,
  REMIT_OR_UPLOAD,
  HOLD_REQUEST,
  HOLD_APPROVE,
  SPECIAL_REMIT_REQUEST,
  SPECIAL_REMIT_APPROVE,
  // Production reconciliation (PRCID)
  RECON_PROCESS,
  RECON_SEND,
  // Adjustment / cancellation (ADJID, MKTID.008)
  ADJ_REQUEST,
  ADJ_PROCESS,
  ADJ_APPROVE,
  ADJ_POST,
  // Commission receivables and incentives (CMRID)
  COMMREC_PROCESS,
  COMMREC_APPROVE,
  INCENTIVE_MANAGE,
  BIR_CERT_SUBMIT,
  BIR_CERT_ACK,
  // Operations interfaces: feed configuration and re-runs (BRQID.004/005)
  FLOWIN_MANAGE,

  // Product Maintenance (BDOI BRD-3, BRPM.002, PMADD05). See
  // docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md section 6.1 and V755; the action class of
  // each permission (VIEW / CREATE / AMEND / APPROVE) is in sec_permission_action.
  // Catalog product areas: read, archive (expired / retired packages), maintain, authorise and
  // the post-set-up validation of package versions (PMADD06).
  PRODUCT_VIEW,
  PRODUCT_ARCHIVE_VIEW,
  PRODUCT_MAINTAIN,
  PRODUCT_AUTHORIZE,
  PRODUCT_VALIDATE,
  // Incentive criteria on the maintained products matrix (PMADD07/08)
  INCENTIVE_CRITERIA_MAINTAIN,
  // Package request lifecycle (BRPM.008-017)
  PKG_REQUEST,
  PKG_REQUEST_APPROVE,
  PKG_TSU_RECOMMEND,
  PKG_TSU_APPROVE,
  PKG_NEGOTIATE,
  PKG_QS_APPROVE,
  PKG_MANCOM_SIGNOFF,
  PKG_ADVISORY,
  // Package Status Update Report and Product Maintenance home (BRPM.018/019)
  PKG_REPORT_VIEW,

  // Collections (BDOI BRD-4). See docs/architecture/COLLECTIONS_DESIGN.md section 6.1 and V1000.
  // Home, worklist, account page (read-only) and completed collections (BRCLXN.001-012)
  CLX_VIEW,
  // Efforts, promises, PR dispositions and remarks on own / assigned items; soft lock (016-023,
  // 055)
  CLX_WORK,
  // Bulk update in the grid and the upload handler (BRCLXN.051)
  CLX_BULK_UPDATE,
  // Manual escalation (BRCLXN.050) and acting on escalations (TL / UH / Section Head, 049)
  CLX_ESCALATE,
  CLX_ESCALATION_HANDLE,
  // Reassignment and assignment rules (BRCLXN.052)
  CLX_ASSIGN,
  // Collector disposition and application request on unapplied payments (BRCLXN.030-033)
  CLX_UNAPPLIED_WORK,
  // Installment plans and billing statements (BRCLXN.053/058)
  CLX_BILLING,
  // Threshold, escalation rules, billing frequencies, invoice pattern, Collections LOVs
  CLX_SETUP,
  // Export of lists and download of files (caveat p.93); scheduled files and reports; audit log
  CLX_EXPORT,
  CLX_REPORT_VIEW,
  CLX_AUDIT_VIEW,

  // Accounting, Disbursement and ACSL (BDOI BRD-5). See
  // docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section 8.1 and V890.
  // FRBS: journal assignment, revaluation rate, chart upload, close schedule (FRBS 2.2-2.6)
  JOURNAL_ASSIGN,
  REVALUATION_RATE_MAINTAIN,
  COA_UPLOAD,
  GL_CLOSE_SCHEDULE,
  // FRBS report pack and service fee (FRBS 2.10, 3.2)
  FRBS_REPORT_VIEW,
  FRBS_REPORT_EXPORT,
  SERVICE_FEE_MANAGE,
  SERVICE_FEE_APPROVE,
  SERVICE_FEE_TAG,
  // Disbursement (DIS 2.2-3.28); DISB_PROCESS above is the processor permission
  DISB_VIEW,
  DISB_PAYEE_MAINTAIN,
  DISB_PAYEE_AUTHORIZE,
  DISB_PAYEE_VIEW_FULL,
  DISB_UPLOAD,
  DISB_REVIEW,
  DISB_APPROVE,
  DISB_STATUS_APPROVE,
  DISB_EOD,
  DISB_FUNDING_REQUEST,
  DISB_FUNDING_VERIFY,
  DISB_FUNDING_APPROVE,
  DISB_TAG,
  DISB_REPORT_VIEW,
  DISB_REPORT_EXPORT,
  // Marketing refund and cash-advance requests (MKT 1.2-2.26)
  PRQ_CREATE,
  PRQ_ASSIGN,
  PRQ_REVIEW,
  PRQ_APPROVE,
  PRQ_HR_APPROVE,
  PRQ_VIEW,
  // ACSL (ACSL 2.2-2.16, 2.9.1-2.9.2)
  ACSL_VIEW,
  ACSL_UPLOAD,
  ACSL_PROCESS,
  ACSL_ASSIGN,
  ACSL_REVIEW,
  ACSL_APPROVE,
  ACSL_APPLY,
  ACSL_REPORT_VIEW,
  ACSL_REPORT_EXPORT,
  REMIT_DEDUCTION_CONFIRM,
  // Employee / cost-centre master (DIS 3.30.1)
  EMPLOYEE_MAINTAIN
}
