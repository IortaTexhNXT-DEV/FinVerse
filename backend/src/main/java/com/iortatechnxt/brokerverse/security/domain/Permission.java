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
  FLOWIN_MANAGE
}
