-- =====================================================================================
-- iNXT BrokerVerse - V771 Remittance workflows, accounting events and document templates
--   * OPS_REMITTANCE: Process Remittance of a batch (RMTID.009-011/019/029/036);
--   * OPS_HOLD: Marketing hold requests (MKTID.002-007, RMTID.021);
--   * OPS_SPECIAL_REMIT: special remittance requests (MKTID.009, RMTID.030/033);
--   * accounting events OPS_REMITTANCE (OPERATIONS_DESIGN 5 row 12) and OPS_REMIT_INCENTIVE
--     (row 13); the rules are configured by Comptrollership (OQ07), demo rules in V992;
--   * templates of the remittance schedule and payment request notes (RMTID.011, OQ42).
-- =====================================================================================

-- ---------- OPS_REMITTANCE ----------------------------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('OPS_REMITTANCE', 'REVIEW_IN_PROCESS', 'Review in process', 'REMIT_PROCESS', 24, true, false, 10),
       ('OPS_REMITTANCE', 'ON_HOLD', 'Batch on hold', 'REMIT_PROCESS', null, false, false, 15),
       ('OPS_REMITTANCE', 'FOR_APPROVAL', 'For approval', 'REMIT_APPROVE', 24, false, false, 20),
       ('OPS_REMITTANCE', 'APPROVED', 'Approved - with Disbursement', null, null, false, false, 30),
       ('OPS_REMITTANCE', 'PARTIALLY_REMITTED', 'Partially remitted - awaiting insurer OR', 'REMIT_OR_UPLOAD', null, false, false, 40),
       ('OPS_REMITTANCE', 'FULLY_REMITTED', 'Fully remitted - awaiting insurer OR', 'REMIT_OR_UPLOAD', null, false, false, 50),
       ('OPS_REMITTANCE', 'OR_RECEIVED', 'Insurer OR received', null, null, false, true, 60),
       ('OPS_REMITTANCE', 'RETURNED', 'Returned', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_REMITTANCE', 'REVIEW_IN_PROCESS', 'submit', 'FOR_APPROVAL', 'Submit for approval', 'REMIT_PROCESS', false, null, 10),
       ('OPS_REMITTANCE', 'REVIEW_IN_PROCESS', 'hold', 'ON_HOLD', 'Hold batch', 'REMIT_PROCESS', true, 'HOLD_REASON', 20),
       ('OPS_REMITTANCE', 'REVIEW_IN_PROCESS', 'return', 'RETURNED', 'Return batch', 'REMIT_PROCESS,REMIT_APPROVE', false, 'REMIT_RETURN_REASON', 90),
       ('OPS_REMITTANCE', 'ON_HOLD', 'release', 'REVIEW_IN_PROCESS', 'Release batch', 'REMIT_PROCESS', true, null, 10),
       ('OPS_REMITTANCE', 'FOR_APPROVAL', 'approve', 'APPROVED', 'Approve and push to Disbursement', 'REMIT_APPROVE', false, null, 10),
       ('OPS_REMITTANCE', 'FOR_APPROVAL', 'send_back', 'REVIEW_IN_PROCESS', 'Send back to processor', 'REMIT_APPROVE', true, 'REMIT_RETURN_REASON', 20),
       ('OPS_REMITTANCE', 'FOR_APPROVAL', 'return', 'RETURNED', 'Return batch', 'REMIT_APPROVE', false, 'REMIT_RETURN_REASON', 90),
       ('OPS_REMITTANCE', 'APPROVED', 'dv_full', 'FULLY_REMITTED', 'DV assigned - fully remitted', 'DISB_PROCESS', false, null, 10),
       ('OPS_REMITTANCE', 'APPROVED', 'dv_partial', 'PARTIALLY_REMITTED', 'DV assigned - partially remitted', 'DISB_PROCESS', false, null, 20),
       ('OPS_REMITTANCE', 'PARTIALLY_REMITTED', 'or_received', 'OR_RECEIVED', 'Insurer OR uploaded', 'REMIT_OR_UPLOAD', false, null, 10),
       ('OPS_REMITTANCE', 'FULLY_REMITTED', 'or_received', 'OR_RECEIVED', 'Insurer OR uploaded', 'REMIT_OR_UPLOAD', false, null, 10);

-- ---------- OPS_HOLD ----------------------------------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('OPS_HOLD', 'DRAFT', 'Hold request draft', 'HOLD_REQUEST', null, true, false, 10),
       ('OPS_HOLD', 'FOR_APPROVAL', 'Hold for approval', 'HOLD_APPROVE', 24, false, false, 20),
       ('OPS_HOLD', 'ACTIVE', 'On hold', 'REMIT_PROCESS', null, false, false, 30),
       ('OPS_HOLD', 'EXTENSION_FOR_APPROVAL', 'Extension for approval', 'HOLD_APPROVE', 24, false, false, 40),
       ('OPS_HOLD', 'CANCEL_FOR_APPROVAL', 'Cancellation for approval', 'HOLD_APPROVE', 24, false, false, 50),
       ('OPS_HOLD', 'RELEASED', 'Released', null, null, false, true, 60),
       ('OPS_HOLD', 'REJECTED', 'Rejected', null, null, false, true, 70),
       ('OPS_HOLD', 'CANCELLED', 'Cancelled', null, null, false, true, 80);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_HOLD', 'DRAFT', 'submit', 'FOR_APPROVAL', 'Submit for approval', 'HOLD_REQUEST', false, null, 10),
       ('OPS_HOLD', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel request', 'HOLD_REQUEST', false, null, 90),
       ('OPS_HOLD', 'FOR_APPROVAL', 'approve', 'ACTIVE', 'Approve hold', 'HOLD_APPROVE', false, null, 10),
       ('OPS_HOLD', 'FOR_APPROVAL', 'reject', 'REJECTED', 'Reject hold', 'HOLD_APPROVE', false, null, 20),
       ('OPS_HOLD', 'ACTIVE', 'extend', 'EXTENSION_FOR_APPROVAL', 'Request extension', 'HOLD_REQUEST', false, null, 10),
       ('OPS_HOLD', 'ACTIVE', 'request_cancel', 'CANCEL_FOR_APPROVAL', 'Request cancellation', 'HOLD_REQUEST', false, null, 20),
       ('OPS_HOLD', 'ACTIVE', 'release', 'RELEASED', 'Release hold', 'HOLD_REQUEST', false, null, 30),
       ('OPS_HOLD', 'ACTIVE', 'expire', 'RELEASED', 'Hold expired', 'HOLD_APPROVE', false, null, 40),
       ('OPS_HOLD', 'EXTENSION_FOR_APPROVAL', 'approve_extension', 'ACTIVE', 'Approve extension', 'HOLD_APPROVE', false, null, 10),
       ('OPS_HOLD', 'EXTENSION_FOR_APPROVAL', 'reject_extension', 'ACTIVE', 'Reject extension', 'HOLD_APPROVE', false, null, 20),
       ('OPS_HOLD', 'CANCEL_FOR_APPROVAL', 'approve_cancel', 'RELEASED', 'Approve cancellation', 'HOLD_APPROVE', false, null, 10),
       ('OPS_HOLD', 'CANCEL_FOR_APPROVAL', 'reject_cancel', 'ACTIVE', 'Reject cancellation', 'HOLD_APPROVE', false, null, 20);

-- ---------- OPS_SPECIAL_REMIT ------------------------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('OPS_SPECIAL_REMIT', 'REQUESTED', 'Requested - validating', 'SPECIAL_REMIT_REQUEST', null, true, false, 10),
       ('OPS_SPECIAL_REMIT', 'FOR_APPROVAL', 'Special remittance for approval', 'SPECIAL_REMIT_APPROVE', 24, false, false, 20),
       ('OPS_SPECIAL_REMIT', 'IN_PROCESS_REMITTANCE', 'In Process Remittance', 'REMIT_PROCESS', 24, false, false, 30),
       ('OPS_SPECIAL_REMIT', 'PUSHED_TO_DISBURSEMENT', 'Pushed to Disbursement', null, null, false, true, 40),
       ('OPS_SPECIAL_REMIT', 'REJECTED', 'Rejected', null, null, false, true, 80),
       ('OPS_SPECIAL_REMIT', 'RETURNED', 'Returned by Remittance', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_SPECIAL_REMIT', 'REQUESTED', 'validate', 'FOR_APPROVAL', 'Validated', 'SPECIAL_REMIT_REQUEST', false, null, 10),
       ('OPS_SPECIAL_REMIT', 'FOR_APPROVAL', 'approve', 'IN_PROCESS_REMITTANCE', 'Approve special remittance', 'SPECIAL_REMIT_APPROVE', false, null, 10),
       ('OPS_SPECIAL_REMIT', 'FOR_APPROVAL', 'reject', 'REJECTED', 'Reject special remittance', 'SPECIAL_REMIT_APPROVE', false, 'REMIT_RETURN_REASON', 20),
       ('OPS_SPECIAL_REMIT', 'IN_PROCESS_REMITTANCE', 'pushed', 'PUSHED_TO_DISBURSEMENT', 'Pushed to Disbursement', 'REMIT_APPROVE', false, null, 10),
       ('OPS_SPECIAL_REMIT', 'IN_PROCESS_REMITTANCE', 'returned', 'RETURNED', 'Batch returned', 'REMIT_PROCESS,REMIT_APPROVE', false, null, 20);

-- ---------- Accounting events (OPERATIONS_DESIGN section 5) --------------------------------
insert into acc_event_type (code, name, category, journal_type, description, amount_components) values
 ('OPS_REMITTANCE', 'Remittance to insurer', 'PAYMENT', 'PAYMENT',
  'Remittance batch approved and pushed to Disbursement, per invoice line (RMTID.010/011): the paid AR part of '
  || 'the DTIP and the withholding tax the insurer withholds on the commission, against the commission '
  || 'receivable (commission and VAT) and the net due to the insurer for disbursement. Source RMB:<batch>:<invoice>.',
  'DTIP,CWT,COMMISSION_RECEIVABLE,DUE_FOR_DISBURSEMENT'),
 ('OPS_REMIT_INCENTIVE', 'Early remittance incentive', 'COMMISSION', 'COMMISSION',
  'Early remittance incentive of a With Incentives batch (RMTID.023): deducted from the amount due to the insurer '
  || 'for disbursement as incentive income and its output VAT; the incentive OR is issued through ReceiptIssuer. '
  || 'Source RMB:<batch>:INC.',
  'DUE_FOR_DISBURSEMENT,INCENTIVE_INCOME,OUTPUT_VAT');

-- ---------- Document templates (RMTID.011; layouts to confirm, OQ42) ------------------------
insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('REMITTANCE_SCHEDULE', 1, 'Remittance schedule',
 'Remittance schedule {{reference}} of {{insurerName}} ({{remittanceType}}). The amounts are the premium collected '
 || 'and applied for the accounts listed, net of the realized commission, VAT and withholding tax{{incentiveNote}}. '
 || 'Please return this schedule with your official receipt number and date for each account.',
 date '2020-01-01', now(), 'SYSTEM'),
('REMITTANCE_PAYMENT_REQUEST', 1, 'Payment request',
 'Please release payment of {{currency}} {{amount}} to {{insurerName}} for remittance batch {{reference}}, '
 || 'approved by {{approvedBy}}. The remittance schedule is attached.',
 date '2020-01-01', now(), 'SYSTEM');
