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
  EMPLOYEE_MAINTAIN,

  // Sanction Screening and Risk Profiling (BDOI BRD-10). See
  // docs/architecture/SANCTION_SCREENING_DESIGN.md section 6.1 and V1050.
  // Cases, the client screening tab and the watchlists (read)
  SCR_VIEW,
  // Versioned configuration: draft / submit (SNSRP-101-108) and approve / reject (SNSRP-109)
  SCR_CONFIG_MAINTAIN,
  SCR_CONFIG_APPROVE,
  // Watchlist entries and list files: maintain (SNSRP-203) and approve changes (SNSRP-204)
  SCR_LIST_MAINTAIN,
  SCR_LIST_APPROVE,
  // Investigation: reviews, documents, dispositions (SNSRP-501, 502, 601)
  SCR_INVESTIGATE,
  // Manual risk-tag update with justification (SNSRP-304)
  SCR_RISK_TAG,
  // Case re-assignment (SNSRP-404)
  SCR_CASE_ASSIGN,
  // Unit Head approval (SNSRP-702), BU escalation review and STR preparation (SNSRP-703, 705)
  SCR_CASE_APPROVE,
  SCR_COMPLIANCE_REVIEW,
  // AML Committee vote (SNSRP-704)
  SCR_COMMITTEE,
  // STR extraction and filing reference (SNSRP-706)
  SCR_STR_EXTRACT,
  // Compliance reports and export (SNSRP-901); screening audit log (SNSRP-903)
  SCR_REPORT_VIEW,
  SCR_AUDIT_VIEW,

  // User Access Maintenance (BDOI BRD-11, BRD 4.002.2). See
  // docs/architecture/USER_ACCESS_DESIGN.md section 6.1 and V1060. ACCESS_REQUEST and
  // ACCESS_APPROVE (above) stay: the umbrella request permission and the approver permission.
  // 1-4. Enroll, modify, deactivate and reactivate users by request
  UAM_ENROLL,
  UAM_MODIFY,
  UAM_DEACTIVATE,
  UAM_REACTIVATE,
  // 5. Apply a correction to a returned request; 6. cancel a request
  UAM_CORRECT,
  UAM_CANCEL,
  // 7. View requests
  UAM_VIEW,
  // 9. Group-profile (role) requests
  UAM_GROUP_REQUEST,
  // 10. User access reports
  UAM_REPORT_VIEW,
  // Second approval of privileged or out-of-hours changes (UAM-NFR-40)
  UAM_SECOND_APPROVE,

  // Claims Handling (BDOI BRD-7, broking claims). See docs/architecture/CLAIMS_BROKING_DESIGN.md
  // section 7.1 and V1020. The insurer-side CLAIM_* permissions above stay hidden from BDOI roles.
  // Claims home, worklist and claim record (read); Cover Lookup (BRCLM.002/003)
  BCL_VIEW,
  BCL_COVER_VIEW,
  // Record a claim, loss details, locations, insurer claim numbers, updates, diary (BRCLM.003/037)
  BCL_RECORD,
  // Claims authorization code when the premium is paid (BRCLM.001)
  BCL_AUTHORIZE,
  // Status change within the status access matrix, reported date, resume (BRCLM.004/011/035)
  BCL_STATUS_UPDATE,
  // Permanent closure (BRCLM.005/035) and reopen of a closed claim (CLQ06)
  BCL_CLOSE,
  BCL_REOPEN,
  // Team Lead / Team Head rights (BRCLM.006/015/018/019/024)
  BCL_CLAIMANT_OVERRIDE,
  BCL_SETTLEMENT_UPDATE,
  BCL_ADJUSTER_ASSIGN,
  BCL_FOLLOW_UP_OVERRIDE,
  BCL_RESERVE_AMEND,
  // Next action plan summary (BRCLM.021)
  BCL_ACTION_PLAN,
  // Insurer location references (BRCLM.042)
  BCL_LOCATION_REF_MAINTAIN,
  // Status / settlement attributes, status access matrix, handler register, Claims lists (010/012)
  BCL_SETUP,
  // Claims Handling reports: on screen, export, flat data extract (BRCLM.026-034/038/040)
  BCL_REPORT_VIEW,
  BCL_REPORT_EXPORT,
  BCL_DATA_EXTRACT,

  // Employee Benefits (BDOI BRD-8). See docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md section 6.1
  // and V1030. Documents are further restricted by the access classes of V1031 (BRID-025).
  // EB screens, programmes, cycles and documents (BRID-023)
  EB_VIEW,
  // AO maker actions: programme, RA, feedback, BOR, franchise, TOR, requests, proposals,
  // comparative, revisions, confirmation, placement trigger, member changes (BRID-001-017)
  EB_MARKET,
  // Authorised signatory of the comparative, never its maker (BRID-010)
  EB_COMPARATIVE_APPROVE,
  // BDOI Management approval above the value threshold (BRID-016)
  EB_THRESHOLD_APPROVE,
  // Processing: validate member changes, policy forms and SOAs (BRID-018-021, 025)
  EB_PROCESS,
  // Collection: SOA and billing view, release acknowledgement (BRID-021, 025)
  EB_COLLECT,
  // Threshold rules, required documents, EB parameters and templates (BRID-016)
  EB_SETUP,
  // EB reports (BRID-022, 022.01)
  EB_REPORT_VIEW
  // The portal permissions of design 6.1 (PORTAL_USER_REQUEST, PORTAL_USER_APPROVE, PORTAL_ADMIN)
  // are parked with the partner portal (BDOI Drop 2 "Employee Benefits (No Portal Feature)"):
  // adding
  // PORTAL_USER_APPROVE switches the approver of EXTERNAL access requests (AccessApprovers), so it
  // comes with the portal wave.
}
