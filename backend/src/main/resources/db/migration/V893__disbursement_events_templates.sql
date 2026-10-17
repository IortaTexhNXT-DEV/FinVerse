-- =====================================================================================
-- iNXT BrokerVerse - V893 Disbursement (BRD-5, wave A1-DSB): document templates and parameters
--   The accounting event types DISB_VOUCHER, DISB_CHECK_NEGOTIATED, DISB_CHECK_STALE and
--   DISB_FUND_TRANSFER, the workflows, LOV types, alerts and the parameters DISB_STALE_DAYS,
--   DISB_CHECK_CLEARING and DISB_AUTO_APPROVER_ROUTING are seeded by the foundation (V890).
--   DIS 2.7.5-2.7.9  draft texts of the DV, ATD, MC / DD, credit ticket, TT and check forms
--   DIS 2.7.7        ATD e-mail to the processing branch with the requestor in copy
--   DIS 2.7.12       payment advice e-mailed to the payee after end of day
--   DIS 2.2.3        amendment of an active payee goes back to the checker (transition amend)
--   The layouts, signatories and bank check formats are parked (AQ13, AQ14): each is a
--   versioned template that Business Administration replaces without a release.
-- =====================================================================================

insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('DSB_VOUCHER', 1, 'Disbursement voucher',
 'Pay to the order of {{payeeName}} the amount of {{currency}} {{netAmount}} for {{purpose}}. Disbursement voucher {{dvNo}} of request {{requestNo}}; mode of payment {{mode}}.', date '2020-01-01', now(), 'SYSTEM'),
('DSB_ATD', 1, 'Authority to debit',
 'Please debit the BDOIR main account {{bankAccountNo}} ({{bankName}}) for {{currency}} {{netAmount}} and credit {{payeeName}}, account {{payeeAccountNo}}, reference {{dvNo}}. This authority to debit is issued under disbursement voucher {{dvNo}} dated {{valueDate}}.', date '2020-01-01', now(), 'SYSTEM'),
('DSB_ATD_EMAIL', 1, 'Authority to debit {{dvNo}}',
 'Dear Branch Team,

Attached is the authority to debit {{instrumentNo}} for {{currency}} {{netAmount}} in favour of {{payeeName}} under disbursement voucher {{dvNo}}. Kindly process the debit and confirm by reply to this e-mail.

Thank you,
BDOI Disbursement', date '2020-01-01', now(), 'SYSTEM'),
('DSB_MC_DD', 1, 'Manager''s check / demand draft request',
 'Please issue a manager''s check or demand draft payable to {{payeeName}} for {{currency}} {{netAmount}}, debiting account {{bankAccountNo}} ({{bankName}}). Reference {{dvNo}}.', date '2020-01-01', now(), 'SYSTEM'),
('DSB_CREDIT_TICKET', 1, 'Credit ticket',
 'Please debit account {{bankAccountNo}} ({{bankName}}) and credit {{payeeName}}, account {{payeeAccountNo}}, for {{currency}} {{netAmount}}. Reference {{dvNo}}.', date '2020-01-01', now(), 'SYSTEM'),
('DSB_TT', 1, 'Telegraphic transfer',
 'Please remit by telegraphic transfer {{currency}} {{netAmount}} from account {{bankAccountNo}} ({{bankName}}) to {{payeeName}}, account {{payeeAccountNo}} at {{payeeBank}}. Reference {{dvNo}}.', date '2020-01-01', now(), 'SYSTEM'),
('DSB_CHECK', 1, 'Check',
 'Pay to the order of {{payeeName}} the sum of {{currency}} {{netAmount}}. Check {{instrumentNo}} drawn on {{bankName}} account {{bankAccountNo}}, dated {{printDate}}, voucher {{dvNo}}.', date '2020-01-01', now(), 'SYSTEM'),
('DSB_PAYMENT_ADVICE', 1, 'Payment advice {{dvNo}}',
 'Dear {{payeeName}},

This is to advise that BDO Insurance and Reinsurance Brokers, Inc. processed on {{valueDate}} a payment of {{currency}} {{netAmount}} by {{mode}} under reference {{dvNo}}{{instrumentText}}. For remittances, the schedule of accounts is attached.

Thank you,
BDOI Disbursement', date '2020-01-01', now(), 'SYSTEM');

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('DISB_NO_PAYEE_ACTION', 'HOLD', 'STRING', 'DISBURSEMENT',
     'Gateway request whose payee is not maintained (DIS 3.25.0, AQ11/AQ12): HOLD keeps it as NO_PAYEE, raises DISB_PAYEE_NO_MATCH and a payee request and resumes it once the payee is authorised; RETURN returns it at once to its source', null, null, now(), 'SYSTEM'),
    ('DISB_CHECK_SERIES_WARNING', '20', 'INTEGER', 'DISBURSEMENT',
     'Remaining check leaves of a paying account at which CHECK_SERIES_LOW is raised (DIS 2.23.x)', 1, 1000, now(), 'SYSTEM'),
    ('DISB_CHECK_CLEARING_ACCOUNT', '2241', 'STRING', 'DISBURSEMENT',
     'Checks outstanding clearing account credited for issued checks while DISB_CHECK_CLEARING = ON (@PAY_ACCOUNT of DISB_VOUCHER, DIS 3.27.1; real account AQ02)', null, null, now(), 'SYSTEM');

-- Voucher type of a stale check re-issued (design 6 row 10): the DISB_VOUCHER component
-- STALE_REISSUE, requested by Disbursement itself.
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
values ('DISBURSEMENT_TYPE', 'STALE_REISSUE', 'Re-issue of a stale check', 95, null, date '2020-01-01',
        'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM');

-- Amendment of an active payee goes back to the checker (DIS 2.2.3 "edit with maker-checker"):
-- the only transition V890 lacks for it.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('DISB_PAYEE', 'ACTIVE', 'amend', 'FOR_AUTHORIZATION', 'Submit changes for authorisation', 'DISB_PAYEE_MAINTAIN', false, null, 20);
