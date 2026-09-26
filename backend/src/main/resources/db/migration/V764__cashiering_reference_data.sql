-- =====================================================================================
-- iNXT BrokerVerse - V764 Cashiering reference data (Operations BRD-2)
--   * Accounting event types of OPERATIONS_DESIGN section 5, rows 1-11 and 14-15. The rules are
--     configured by Comptrollership (OQ07); seed rules are in V991.
--   * Workflows OPS_RECEIPT_ACTION (CSHID.001-005, OQ06), OPS_DISPOSITION (CSHID.024/025, OQ15)
--     and OPS_CWT_2307 (CSHID.026/027, MKTID.010/013, DBMID.001, OQ16).
--   * Disposition type actions (CSHID.024), minimal balance rules (CSHID.016, OQ11) and the
--     payment file layouts (CSHID.008, OQ03/OQ04: parked, BRD field lists only).
-- =====================================================================================

-- ---------- Accounting events (OPERATIONS_DESIGN section 5) ----------------------------------
insert into acc_event_type (code, name, category, journal_type, description, amount_components) values
 ('OPS_AR_RECEIPT', 'Acknowledgement receipt (premium)', 'RECEIPT', 'RECEIPT',
  'Premium collected on behalf of an insurer (CSHID.001/014): cash or bank in, credited to unapplied collections '
  || 'of the client until applied. A cancellation re-posts it with a negative amount (CSHID.012).',
  'AMOUNT'),
 ('OPS_PAYMENT_APPLY', 'Payment applied to a booked invoice', 'RECEIPT', 'RECEIPT',
  'Payment applied per premium receivable component in the hierarchy DST, premium tax / VAT, LGT, FST, other, '
  || 'basic (CSHID.020/022); commission realized pro rata when OPS_COMMISSION_REALIZATION = ON_COLLECTION. '
  || 'Negative amounts reverse an application (cancellation, re-application).',
  'APPLIED,PR_BASIC,PR_DST,PR_PTX_VAT,PR_LGT,PR_FST,PR_OTHER,REALIZED_COMMISSION,REALIZED_VAT'),
 ('OPS_CWT_RECLASS', 'PR reclassified to the client 2307', 'RECEIPT', 'RECEIPT',
  'The 2% creditable withholding tax portion of the premium receivable moved to PR2307 when the certificate is '
  || 'validated (CSHID.027): Dr PR2307 / Cr PR by component.',
  'PR2307,PR_BASIC,PR_DST,PR_PTX_VAT,PR_LGT,PR_FST,PR_OTHER'),
 ('OPS_CWT_DTIP_OFFSET', '2307 certificate offset against DTIP', 'RECEIPT', 'RECEIPT',
  'The 2307 certificate forwarded to the insurer settles the PR2307 against the premium due to the insurer '
  || '(DBMID.001): Dr DTIP / Cr PR2307.',
  'DTIP,PR2307'),
 ('OPS_EXCESS_TO_OVERAGES', 'Minimal excess to AP overages', 'RECEIPT', 'RECEIPT',
  'Unapplied or excess payments at or below the minimal amount moved to AP overages (Cashiering summary 5.f).',
  'AMOUNT'),
 ('OPS_MINIMAL_BALANCE_REVERSAL', 'Premium minimal balance reversal', 'RECEIPT', 'RECEIPT',
  'Premium receivable balances at or below the minimal amount reversed (CSHID.016, OQ11 target account).',
  'TOTAL,PR_BASIC,PR_DST,PR_PTX_VAT,PR_LGT,PR_FST,PR_OTHER'),
 ('OPS_UNAPPLIED_REFUND', 'Unapplied payment refunded', 'RECEIPT', 'RECEIPT',
  'Refund disposition of an unapplied payment (CSHID.024): unapplied collections to refund payable, then '
  || 'paid by Disbursement.',
  'AMOUNT'),
 ('OPS_UNAPPLIED_RECLASS', 'Unapplied payment reclassified', 'RECEIPT', 'RECEIPT',
  'Reclass or transfer disposition (CSHID.024): unapplied collections moved from one client or marketing unit '
  || 'to another.',
  'RELEASED,ASSIGNED'),
 ('OPS_RECEIPT_REINSTATE', 'Receipt reinstated', 'RECEIPT', 'RECEIPT',
  'Full or partial reinstatement of a cancelled receipt (CSHID.013): cash or bank in to unapplied collections; '
  || 'the reinstated amount is then applied again.',
  'AMOUNT'),
 ('OPS_AR_INSURANCE_RECEIPT', 'Insurer non-premium receipt', 'RECEIPT', 'RECEIPT',
  'Non-premium payment of an insurer (refund, other expenses, AR Insurance) credited to AR Insurer, segregated '
  || 'from premium (CSHID.021, OQ14).',
  'AMOUNT'),
 ('OPS_OR_ISSUE', 'Official receipt issued', 'RECEIPT', 'RECEIPT',
  'Head Office official receipt for BDOI income (CSHID.002/007/014): cash and creditable withholding tax against '
  || 'income by OR type and output VAT; commission ORs of settlement batches make the deferred VAT due.',
  'CASH,CWT,SERVICE_FEE_INCOME,PROFIT_SHARE_INCOME,INCENTIVE_INCOME,OTHER_INCOME,OUTPUT_VAT,COMMISSION_COLLECTED,VAT_DUE');

-- ---------- Workflow OPS_RECEIPT_ACTION (CSHID.001-005; approval assumed, OQ06) --------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('OPS_RECEIPT_ACTION', 'REQUESTED', 'Requested', 'CASH_CANCEL', 8, true, false, 10),
       ('OPS_RECEIPT_ACTION', 'FOR_APPROVAL', 'For approval', 'CASH_APPROVE', 8, false, false, 20),
       ('OPS_RECEIPT_ACTION', 'POSTED', 'Posted', null, null, false, true, 30),
       ('OPS_RECEIPT_ACTION', 'WITHDRAWN', 'Withdrawn', null, null, false, true, 40);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_RECEIPT_ACTION', 'REQUESTED', 'submit', 'FOR_APPROVAL', 'Submit for Approval', 'CASH_CANCEL,CASH_REINSTATE', false, null, 10),
       ('OPS_RECEIPT_ACTION', 'REQUESTED', 'withdraw', 'WITHDRAWN', 'Withdraw', 'CASH_CANCEL,CASH_REINSTATE', true, null, 20),
       ('OPS_RECEIPT_ACTION', 'FOR_APPROVAL', 'approve', 'POSTED', 'Approve and Post', 'CASH_APPROVE', false, null, 10),
       ('OPS_RECEIPT_ACTION', 'FOR_APPROVAL', 'return', 'REQUESTED', 'Return to Requester', 'CASH_APPROVE', true, 'RETURN_REASON', 20);

-- ---------- Workflow OPS_DISPOSITION (CSHID.024/025, OQ15) -----------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('OPS_DISPOSITION', 'UNAPPLIED', 'Unapplied', 'CASH_DISPOSITION', 48, true, false, 10),
       ('OPS_DISPOSITION', 'MONITORING', 'Monitoring', 'CASH_DISPOSITION', 72, false, false, 20),
       ('OPS_DISPOSITION', 'FOR_APPROVAL', 'For approval', 'CASH_DISPOSITION_APPROVE', 24, false, false, 30),
       ('OPS_DISPOSITION', 'IN_PROCESS', 'In process', null, null, false, false, 40),
       ('OPS_DISPOSITION', 'COMPLETED', 'Completed', null, null, false, false, 50),
       ('OPS_DISPOSITION', 'FOR_REVERSAL', 'For reversal', 'CASH_DISPOSITION_APPROVE', 24, false, false, 60),
       ('OPS_DISPOSITION', 'CLOSED', 'Closed', null, null, false, true, 70);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_DISPOSITION', 'UNAPPLIED', 'assign_disposition', 'MONITORING', 'Assign Disposition', 'CASH_DISPOSITION', false, null, 10),
       ('OPS_DISPOSITION', 'UNAPPLIED', 'sweep_overages', 'CLOSED', 'Move to AP Overages', 'CASH_DISPOSITION', false, null, 20),
       ('OPS_DISPOSITION', 'UNAPPLIED', 'auto_apply', 'CLOSED', 'Applied by Automatch', 'CASH_APPLY', false, null, 30),
       ('OPS_DISPOSITION', 'UNAPPLIED', 'receipt_cancelled', 'CLOSED', 'Closed by Receipt Cancellation', 'CASH_APPROVE', false, null, 40),
       ('OPS_DISPOSITION', 'MONITORING', 'update', 'MONITORING', 'Update Disposition', 'CASH_DISPOSITION', false, null, 10),
       ('OPS_DISPOSITION', 'MONITORING', 'submit', 'FOR_APPROVAL', 'Submit for Approval', 'CASH_DISPOSITION', false, null, 20),
       ('OPS_DISPOSITION', 'MONITORING', 'complete', 'COMPLETED', 'Process Disposition', 'CASH_DISPOSITION', false, null, 30),
       ('OPS_DISPOSITION', 'MONITORING', 'withdraw', 'UNAPPLIED', 'Withdraw Disposition', 'CASH_DISPOSITION', false, null, 40),
       ('OPS_DISPOSITION', 'FOR_APPROVAL', 'approve', 'IN_PROCESS', 'Approve', 'CASH_DISPOSITION_APPROVE', false, null, 10),
       ('OPS_DISPOSITION', 'FOR_APPROVAL', 'return', 'MONITORING', 'Return to Cashier', 'CASH_DISPOSITION_APPROVE', true, 'RETURN_REASON', 20),
       ('OPS_DISPOSITION', 'IN_PROCESS', 'complete', 'COMPLETED', 'Complete', 'CASH_DISPOSITION_APPROVE', false, null, 10),
       ('OPS_DISPOSITION', 'COMPLETED', 'mark_reversal', 'FOR_REVERSAL', 'Mark for Reversal', 'CASH_DISPOSITION', false, null, 10),
       ('OPS_DISPOSITION', 'COMPLETED', 'reopen', 'UNAPPLIED', 'Reopen Remaining Balance', 'CASH_DISPOSITION', false, null, 20),
       ('OPS_DISPOSITION', 'COMPLETED', 'close', 'CLOSED', 'Close', 'CASH_DISPOSITION', true, null, 30),
       ('OPS_DISPOSITION', 'FOR_REVERSAL', 'approve_reversal', 'UNAPPLIED', 'Approve Reversal', 'CASH_DISPOSITION_APPROVE', false, null, 10),
       ('OPS_DISPOSITION', 'FOR_REVERSAL', 'reject_reversal', 'COMPLETED', 'Reject Reversal', 'CASH_DISPOSITION_APPROVE', true, 'RETURN_REASON', 20);

-- ---------- Workflow OPS_CWT_2307 (CSHID.026/027, MKTID.010/013, DBMID.001, OQ16) ------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('OPS_CWT_2307', 'TAGGED', 'Tagged by Marketing', 'CWT_TAG', 48, true, false, 10),
       ('OPS_CWT_2307', 'VALIDATING', 'Validating', 'CWT_PROCESS', 24, false, false, 20),
       ('OPS_CWT_2307', 'REPORT_POSTED', 'Report posted', 'CWT_PROCESS', 24, false, false, 30),
       ('OPS_CWT_2307', 'WITH_DISBURSEMENT', 'With Disbursement', 'DISB_PROCESS', 72, false, false, 40),
       ('OPS_CWT_2307', 'RELEASED', 'Released to insurer', null, null, false, true, 50),
       ('OPS_CWT_2307', 'SETTLED_CASH', 'Settled in cash', null, null, false, true, 60),
       ('OPS_CWT_2307', 'CANCELLED', 'Cancelled', null, null, false, true, 70);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_CWT_2307', 'TAGGED', 'receive', 'VALIDATING', 'Receive for Validation', 'CWT_PROCESS', false, null, 10),
       ('OPS_CWT_2307', 'TAGGED', 'cancel', 'CANCELLED', 'Cancel Tag', 'CWT_TAG', true, null, 20),
       ('OPS_CWT_2307', 'VALIDATING', 'validate', 'REPORT_POSTED', 'Validate and Post', 'CWT_PROCESS', false, null, 10),
       ('OPS_CWT_2307', 'VALIDATING', 'settle_cash', 'SETTLED_CASH', 'Settle in Cash', 'CWT_PROCESS', false, null, 20),
       ('OPS_CWT_2307', 'VALIDATING', 'return', 'TAGGED', 'Return to Marketing', 'CWT_PROCESS', true, 'RETURN_REASON', 30),
       ('OPS_CWT_2307', 'REPORT_POSTED', 'route', 'WITH_DISBURSEMENT', 'Route to Disbursement', 'CWT_PROCESS', false, null, 10),
       ('OPS_CWT_2307', 'WITH_DISBURSEMENT', 'release_to_insurer', 'RELEASED', 'Release to Insurer', 'DISB_PROCESS', false, null, 10);

-- ---------- Disposition types (CSHID.024; approvals to confirm, OQ15) -------------------------
insert into csh_disposition_type_rule (type_code, action, requires_approval, description) values
    ('APPLY_OTHER_INVOICE', 'APPLY', false, 'Applied to another booked invoice through the component hierarchy'),
    ('DST_APPLICATION', 'DST_APPLY', false, 'Applied to the DST component of a booked invoice only'),
    ('REFUND', 'REFUND', true, 'Refunded to the payor through Disbursement'),
    ('RECLASS', 'RECLASS', true, 'Reclassified to another client'),
    ('TRANSFER_UNIT', 'TRANSFER', true, 'Transferred to another marketing unit'),
    ('OTHERS', 'MANUAL', true, 'Other disposition settled outside the system, balance released');

-- ---------- Minimal balance rules (CSHID.016, summary 5.f; OQ11) ------------------------------
insert into csh_minimal_balance_rule (kind, max_amount, exclude_cwt, exclude_dst, exclude_whole_premium, action,
    active, description) values
    ('PREMIUM', 10.00, true, true, true, 'REVERSE', true,
     'Premium receivable balances up to the amount are reversed unless they equal the 2% CWT, the DST or the whole premium (CSHID.016)'),
    ('EXCESS', 10.00, false, false, false, 'OVERAGES', true,
     'Unapplied and excess payments up to the amount are moved to AP overages (Cashiering summary 5.f)'),
    ('COMMISSION', 10.00, false, false, false, 'REVERSE', false,
     'Commission receivable minimal balances: rule and target account to confirm (OQ11), inactive');

-- ---------- Payment file layouts (CSHID.008; exact record layouts parked, OQ03/OQ04) ---------
insert into csh_payment_file_layout (handler_code, kind, delimiter, fields, description) values
    ('PAY_BILLS', 'DELIMITED', '|', null, 'Bills Payment FS01 (IT-DCO): pipe-delimited TXT with the BRD field names until the FS01 layout is supplied (OQ03)'),
    ('PAY_TRADE', 'DELIMITED', '|', null, 'Trade payment (CIB): pipe-delimited TXT with the BRD field names until the layout is supplied (OQ03)'),
    ('PAY_CLPC', 'AUTO', null, null, 'CLPC payment: Excel or CSV with the BRD field names'),
    ('PAY_DIRECT_CREDIT', 'DELIMITED', '|', null, 'Direct Credit FS01/04: pipe-delimited TXT with the BRD field names until the layout is supplied (OQ03)'),
    ('PAY_PDC', 'AUTO', null, null, 'PDC list from PMS: Excel or CSV (layout not given in the BRD, OQ03)'),
    ('COMMISSION_PAYMENT', 'AUTO', null, null, 'Commission payment details from Collection (CSHID.007)'),
    ('CWT_TAGS', 'AUTO', null, null, 'BIR 2307 tags from Marketing Collection (MKTID.013)');

-- ---------- Collection accounts of the @BANK role (set per installation by Comptrollership) --
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CASH_BANK_ACCOUNT', '', 'STRING', 'OPERATIONS',
     'GL account (@BANK role) debited for payments received by check, bank or electronic channels (CSHID.014, OQ07)',
     null, null, now(), 'SYSTEM'),
    ('CASH_ON_HAND_ACCOUNT', '', 'STRING', 'OPERATIONS',
     'GL account (@BANK role) debited for payments received in cash over the counter (CSHID.014, OQ07)',
     null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- ---------- Bulk 2307 tagging by Marketing Collection (CWT_TAGS handler, MKTID.013) -----------
-- The bulk screens need BULK_PROCESS, which V760 did not grant to MKT_COLLECTION.
insert into sec_role_permission (role_id, permission)
select r.id, 'BULK_PROCESS' from sec_role r
where r.code = 'MKT_COLLECTION'
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = 'BULK_PROCESS');
