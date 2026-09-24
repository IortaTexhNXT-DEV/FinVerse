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
  MESSAGE_VIEW
}
