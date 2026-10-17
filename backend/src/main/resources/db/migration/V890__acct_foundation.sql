-- =====================================================================================
-- iNXT BrokerVerse - V890 Accounting, Disbursement and ACSL (BRD-5) foundation: roles and grants,
-- permission action classes, lists of values, workflows of the four new modules, accounting event
-- types, business parameters and exception codes.
--   Requirements: docs/requirements/BDOI_ACCT_BRD_SPEC.md
--     Access (A), FRBS 2.2-3.6, DIS 2.2-3.30, MKT 1.2-2.26, ACSL 2.2-2.16, 2.9.1-2.9.2
--     DIS 2.2.2 payee classes, DIS 2.6.1 / 3.25.1 disbursement types, DIS 2.7-2.21 voucher stages,
--     DIS 2.17 account funding (maker, verifier, two approvers), MKT 1.11.0 refund validation,
--     ACSL 2.7-2.15 correction entries, ACSL 2.9.2 remittance deduction, FRBS 2.10 service fee
--   Design: docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 6 (event types), 7
--   (workflows), 8 (security) and 9 (parameters, alerts, LOV types). The permissions are the enum
--   security.domain.Permission; the grants follow the role matrices of the BRD (final matrix AQ28).
--   The accounting event types are seeded here (not in V893 / V899 / V772) so that the seed rules of
--   V999 can reference them; the modules publish the events. ACSL corrections post as a system
--   journal with the lines of the correction and need no event type.
--   References only V1-V754 tables (roles, permissions, LOV, workflow, events), so it is safe on a
--   fresh database before the broking tables.
-- =====================================================================================

-- ---------- Role profiles (design 8.2) ------------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('FRBS_PROCESSOR', 'FRBS GL Officer (Processor)', now(), 'SYSTEM'),
       ('FRBS_TL', 'FRBS GL Team Lead', now(), 'SYSTEM'),
       ('FRBS_HEAD', 'FRBS GL Team Head / Section Head', now(), 'SYSTEM'),
       ('DISB_TL', 'Disbursement Team Leader', now(), 'SYSTEM'),
       ('DISB_APPROVER', 'Disbursement Approver', now(), 'SYSTEM'),
       ('PRQ_PROCESSOR', 'Marketing Refund / Cash Advance Processor', now(), 'SYSTEM'),
       ('PRQ_REVIEWER', 'Marketing Refund / Cash Advance Reviewer', now(), 'SYSTEM'),
       ('PRQ_APPROVER', 'Marketing Refund / Cash Advance Approver', now(), 'SYSTEM'),
       ('HR_APPROVER', 'Human Resources Approver', now(), 'SYSTEM'),
       ('ACSL_PROCESSOR', 'ACSL Processor', now(), 'SYSTEM'),
       ('ACSL_TL', 'ACSL Team Leader', now(), 'SYSTEM'),
       ('ACSL_HEAD', 'ACSL Team Head (Approver)', now(), 'SYSTEM');

-- Every new role: My Work, documents, report catalogue and dashboard.
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('WORK_VIEW'), ('ATTACHMENT_VIEW'), ('REPORT_VIEW'), ('DASHBOARD_VIEW')) as p(permission)
where r.code in ('FRBS_PROCESSOR', 'FRBS_TL', 'FRBS_HEAD', 'DISB_TL', 'DISB_APPROVER', 'PRQ_PROCESSOR',
                 'PRQ_REVIEWER', 'PRQ_APPROVER', 'HR_APPROVER', 'ACSL_PROCESSOR', 'ACSL_TL', 'ACSL_HEAD')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Disbursement, requests and ACSL work on invoices and clients: Operations home, Invoice 360 and
-- Operations reports, client look-up, documents.
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('OPS_VIEW'), ('OPS_REPORT_VIEW'), ('CLIENT_VIEW'), ('ATTACHMENT_MANAGE')) as p(permission)
where r.code in ('DISB_TL', 'DISB_APPROVER', 'PRQ_PROCESSOR', 'PRQ_REVIEWER', 'PRQ_APPROVER',
                 'ACSL_PROCESSOR', 'ACSL_TL', 'ACSL_HEAD')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Role-specific grants (design 8.2; a proposal until AQ28 is answered).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- FRBS GL Officer (processor): manual entries, service fee runs and tags, report pack
    ('FRBS_PROCESSOR', 'JOURNAL_CREATE'), ('FRBS_PROCESSOR', 'JOURNAL_VIEW'), ('FRBS_PROCESSOR', 'MASTER_VIEW'),
    ('FRBS_PROCESSOR', 'SERVICE_FEE_MANAGE'), ('FRBS_PROCESSOR', 'SERVICE_FEE_TAG'),
    ('FRBS_PROCESSOR', 'FRBS_REPORT_VIEW'), ('FRBS_PROCESSOR', 'FRBS_REPORT_EXPORT'),
    ('FRBS_PROCESSOR', 'REPORT_FINANCIAL'),
    -- FRBS GL Team Lead: + authorise and assign entries, periods and close schedule, chart
    ('FRBS_TL', 'JOURNAL_CREATE'), ('FRBS_TL', 'JOURNAL_VIEW'), ('FRBS_TL', 'MASTER_VIEW'),
    ('FRBS_TL', 'SERVICE_FEE_MANAGE'), ('FRBS_TL', 'SERVICE_FEE_TAG'), ('FRBS_TL', 'FRBS_REPORT_VIEW'),
    ('FRBS_TL', 'FRBS_REPORT_EXPORT'), ('FRBS_TL', 'REPORT_FINANCIAL'),
    ('FRBS_TL', 'JOURNAL_AUTHORIZE'), ('FRBS_TL', 'JOURNAL_ASSIGN'), ('FRBS_TL', 'PERIOD_MANAGE'),
    ('FRBS_TL', 'PERIOD_END_RUN'), ('FRBS_TL', 'GL_CLOSE_SCHEDULE'), ('FRBS_TL', 'MASTER_MAINTAIN'),
    ('FRBS_TL', 'COA_UPLOAD'), ('FRBS_TL', 'SERVICE_FEE_APPROVE'),
    -- FRBS GL Team Head / Section Head: + revaluation rate, master authorisation, reversals
    ('FRBS_HEAD', 'JOURNAL_VIEW'), ('FRBS_HEAD', 'MASTER_VIEW'), ('FRBS_HEAD', 'FRBS_REPORT_VIEW'),
    ('FRBS_HEAD', 'FRBS_REPORT_EXPORT'), ('FRBS_HEAD', 'REPORT_FINANCIAL'), ('FRBS_HEAD', 'JOURNAL_AUTHORIZE'),
    ('FRBS_HEAD', 'JOURNAL_ASSIGN'), ('FRBS_HEAD', 'PERIOD_MANAGE'), ('FRBS_HEAD', 'PERIOD_END_RUN'),
    ('FRBS_HEAD', 'GL_CLOSE_SCHEDULE'), ('FRBS_HEAD', 'SERVICE_FEE_APPROVE'),
    ('FRBS_HEAD', 'REVALUATION_RATE_MAINTAIN'), ('FRBS_HEAD', 'MASTER_AUTHORIZE'), ('FRBS_HEAD', 'JOURNAL_REVERSE'),
    -- Disbursement processor (existing role DISBURSEMENT, V760)
    ('DISBURSEMENT', 'DISB_VIEW'), ('DISBURSEMENT', 'DISB_UPLOAD'), ('DISBURSEMENT', 'DISB_TAG'),
    ('DISBURSEMENT', 'DISB_REPORT_VIEW'), ('DISBURSEMENT', 'DISB_REPORT_EXPORT'), ('DISBURSEMENT', 'TAX_VIEW'),
    -- Disbursement Team Leader: + review, payees, funding request and verification, EOD, status edits
    ('DISB_TL', 'DISB_VIEW'), ('DISB_TL', 'DISB_PROCESS'), ('DISB_TL', 'DISB_UPLOAD'), ('DISB_TL', 'DISB_TAG'),
    ('DISB_TL', 'DISB_REPORT_VIEW'), ('DISB_TL', 'DISB_REPORT_EXPORT'), ('DISB_TL', 'TAX_VIEW'),
    ('DISB_TL', 'DISB_REVIEW'), ('DISB_TL', 'DISB_PAYEE_MAINTAIN'), ('DISB_TL', 'DISB_PAYEE_VIEW_FULL'),
    ('DISB_TL', 'DISB_FUNDING_REQUEST'), ('DISB_TL', 'DISB_FUNDING_VERIFY'), ('DISB_TL', 'DISB_EOD'),
    ('DISB_TL', 'DISB_STATUS_APPROVE'), ('DISB_TL', 'BULK_PROCESS'),
    -- Disbursement Approver: approve / decline / reject, payee authorisation, funding approval
    ('DISB_APPROVER', 'DISB_VIEW'), ('DISB_APPROVER', 'DISB_APPROVE'), ('DISB_APPROVER', 'DISB_PAYEE_AUTHORIZE'),
    ('DISB_APPROVER', 'DISB_PAYEE_VIEW_FULL'), ('DISB_APPROVER', 'DISB_FUNDING_APPROVE'),
    ('DISB_APPROVER', 'DISB_EOD'), ('DISB_APPROVER', 'MASTER_MAINTAIN'), ('DISB_APPROVER', 'MASTER_VIEW'),
    ('DISB_APPROVER', 'DISB_REPORT_VIEW'), ('DISB_APPROVER', 'DISB_REPORT_EXPORT'),
    -- Marketing refund / cash-advance requests (MKT 1.2-2.26)
    ('PRQ_PROCESSOR', 'PRQ_CREATE'), ('PRQ_PROCESSOR', 'PRQ_VIEW'),
    ('PRQ_REVIEWER', 'PRQ_CREATE'), ('PRQ_REVIEWER', 'PRQ_VIEW'), ('PRQ_REVIEWER', 'PRQ_ASSIGN'),
    ('PRQ_REVIEWER', 'PRQ_REVIEW'),
    ('PRQ_APPROVER', 'PRQ_APPROVE'), ('PRQ_APPROVER', 'PRQ_VIEW'),
    ('HR_APPROVER', 'PRQ_HR_APPROVE'), ('HR_APPROVER', 'PRQ_VIEW'),
    -- Marketing AO and Team Lead raise and review refund requests too (MKT 1.2, AQ28)
    ('MKT_AO', 'PRQ_CREATE'), ('MKT_AO', 'PRQ_VIEW'),
    ('MKT_TL', 'PRQ_VIEW'), ('MKT_TL', 'PRQ_ASSIGN'), ('MKT_TL', 'PRQ_REVIEW'),
    -- ACSL processor, team leader and head (ACSL 2.2-2.16)
    ('ACSL_PROCESSOR', 'ACSL_VIEW'), ('ACSL_PROCESSOR', 'ACSL_UPLOAD'), ('ACSL_PROCESSOR', 'ACSL_PROCESS'),
    ('ACSL_PROCESSOR', 'ACSL_APPLY'), ('ACSL_PROCESSOR', 'ACSL_REPORT_VIEW'),
    ('ACSL_PROCESSOR', 'ACSL_REPORT_EXPORT'), ('ACSL_PROCESSOR', 'JOURNAL_VIEW'), ('ACSL_PROCESSOR', 'BULK_PROCESS'),
    ('ACSL_TL', 'ACSL_VIEW'), ('ACSL_TL', 'ACSL_UPLOAD'), ('ACSL_TL', 'ACSL_PROCESS'), ('ACSL_TL', 'ACSL_APPLY'),
    ('ACSL_TL', 'ACSL_REPORT_VIEW'), ('ACSL_TL', 'ACSL_REPORT_EXPORT'), ('ACSL_TL', 'JOURNAL_VIEW'),
    ('ACSL_TL', 'BULK_PROCESS'), ('ACSL_TL', 'ACSL_ASSIGN'), ('ACSL_TL', 'ACSL_REVIEW'),
    ('ACSL_TL', 'REMIT_DEDUCTION_CONFIRM'),
    ('ACSL_HEAD', 'ACSL_VIEW'), ('ACSL_HEAD', 'ACSL_APPROVE'), ('ACSL_HEAD', 'ACSL_REPORT_VIEW'),
    ('ACSL_HEAD', 'JOURNAL_VIEW'),
    -- Comptrollership and administrators: read access; employee / cost-centre master (DIS 3.30.1)
    ('COMPTROLLERSHIP', 'DISB_VIEW'), ('COMPTROLLERSHIP', 'ACSL_VIEW'), ('COMPTROLLERSHIP', 'FRBS_REPORT_VIEW'),
    ('COMPTROLLERSHIP', 'DISB_REPORT_VIEW'), ('COMPTROLLERSHIP', 'ACSL_REPORT_VIEW'),
    ('FIN_ADMIN', 'EMPLOYEE_MAINTAIN'), ('BUSINESS_ADMIN', 'EMPLOYEE_MAINTAIN'),
    ('SYSADMIN', 'DISB_VIEW'), ('SYSADMIN', 'PRQ_VIEW'), ('SYSADMIN', 'ACSL_VIEW'), ('SYSADMIN', 'FRBS_REPORT_VIEW'),
    ('AUDITOR', 'DISB_VIEW'), ('AUDITOR', 'PRQ_VIEW'), ('AUDITOR', 'ACSL_VIEW'), ('AUDITOR', 'FRBS_REPORT_VIEW'),
    ('AUDITOR', 'DISB_REPORT_VIEW'), ('AUDITOR', 'ACSL_REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix, PMADD05) ---------------------------
-- The GL / FRBS permissions are finance permissions and stay unclassified, as the finance modules.
insert into sec_permission_action (permission, area, action)
values
    ('DISB_VIEW', 'DISBURSEMENT', 'VIEW'),
    ('DISB_PAYEE_VIEW_FULL', 'DISBURSEMENT', 'VIEW'),
    ('DISB_REPORT_VIEW', 'DISBURSEMENT', 'VIEW'),
    ('DISB_REPORT_EXPORT', 'DISBURSEMENT', 'VIEW'),
    ('DISB_UPLOAD', 'DISBURSEMENT', 'CREATE'),
    ('DISB_PAYEE_MAINTAIN', 'DISBURSEMENT', 'CREATE'),
    ('DISB_PAYEE_MAINTAIN', 'DISBURSEMENT', 'AMEND'),
    ('DISB_FUNDING_REQUEST', 'DISBURSEMENT', 'CREATE'),
    ('DISB_TAG', 'DISBURSEMENT', 'AMEND'),
    ('DISB_EOD', 'DISBURSEMENT', 'AMEND'),
    ('DISB_REVIEW', 'DISBURSEMENT', 'APPROVE'),
    ('DISB_APPROVE', 'DISBURSEMENT', 'APPROVE'),
    ('DISB_PAYEE_AUTHORIZE', 'DISBURSEMENT', 'APPROVE'),
    ('DISB_STATUS_APPROVE', 'DISBURSEMENT', 'APPROVE'),
    ('DISB_FUNDING_VERIFY', 'DISBURSEMENT', 'APPROVE'),
    ('DISB_FUNDING_APPROVE', 'DISBURSEMENT', 'APPROVE'),
    ('PRQ_VIEW', 'PAYMENT_REQUESTS', 'VIEW'),
    ('PRQ_CREATE', 'PAYMENT_REQUESTS', 'CREATE'),
    ('PRQ_CREATE', 'PAYMENT_REQUESTS', 'AMEND'),
    ('PRQ_ASSIGN', 'PAYMENT_REQUESTS', 'AMEND'),
    ('PRQ_REVIEW', 'PAYMENT_REQUESTS', 'APPROVE'),
    ('PRQ_APPROVE', 'PAYMENT_REQUESTS', 'APPROVE'),
    ('PRQ_HR_APPROVE', 'PAYMENT_REQUESTS', 'APPROVE'),
    ('ACSL_VIEW', 'ACSL', 'VIEW'),
    ('ACSL_REPORT_VIEW', 'ACSL', 'VIEW'),
    ('ACSL_REPORT_EXPORT', 'ACSL', 'VIEW'),
    ('ACSL_UPLOAD', 'ACSL', 'CREATE'),
    ('ACSL_PROCESS', 'ACSL', 'CREATE'),
    ('ACSL_PROCESS', 'ACSL', 'AMEND'),
    ('ACSL_APPLY', 'ACSL', 'AMEND'),
    ('ACSL_ASSIGN', 'ACSL', 'AMEND'),
    ('ACSL_REVIEW', 'ACSL', 'APPROVE'),
    ('ACSL_APPROVE', 'ACSL', 'APPROVE'),
    ('REMIT_DEDUCTION_CONFIRM', 'ACSL', 'APPROVE');

-- ---------- Lists of values (design section 9, spec section 6) --------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('PAYEE_CLASS', 'Payee class', 'Classification of a Disbursement payee (DIS 2.2.2)', true, now(), 'SYSTEM'),
       ('DISBURSEMENT_TYPE', 'Disbursement type', 'Type of a payment request or DV (DIS 2.6.1, 3.25.1)', true, now(), 'SYSTEM'),
       ('DISB_CANCEL_REASON', 'DV cancellation reason', 'Why a request or DV is cancelled (DIS 2.20.0-2.21.0, AQ15)', true, now(), 'SYSTEM'),
       ('DISB_RETURN_REASON', 'DV return / rejection reason', 'Why a DV is returned to the processor or rejected, or a request returned to its source (DIS 2.18.0-2.19.0, 3.25.0)', true, now(), 'SYSTEM'),
       ('BRANCH_EMAIL', 'Branch e-mail address', 'Branch mailboxes for ATD, MC / DD and credit ticket forms; label = e-mail address (DIS 2.7.7-2.7.9, AQ09)', true, now(), 'SYSTEM'),
       ('REFUND_REASON', 'Refund reason', 'Reason of a client refund on the Refund Request Form (MKT 1.10.0, Appendix D)', true, now(), 'SYSTEM'),
       ('RRF_CATEGORY_A', 'Refund category A', 'Category A of a refund line (Appendix D, AQ18)', true, now(), 'SYSTEM'),
       ('RRF_CATEGORY_B', 'Refund category B', 'Category B of a refund line (Appendix D, AQ18)', true, now(), 'SYSTEM'),
       ('ACSL_CASE_TYPE', 'ACSL case type', 'Type of an ACSL case (ACSL 2.5.x-2.9.x)', true, now(), 'SYSTEM'),
       ('ACSL_CORRECTION_KIND', 'ACSL correction kind', 'Kind of a correction entry (ACSL 2.7-2.15, 2.9.1)', true, now(), 'SYSTEM'),
       ('SERVICE_FEE_SEGMENT', 'Service fee segment', 'Segment of a service-fee rule (FRBS 2.10, AQ20)', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    -- Payee classes (DIS 2.2.2: Supplier, Insurer, Employee, Client, Others - government agencies)
    ('PAYEE_CLASS', 'SUPPLIER', 'Supplier', 10),
    ('PAYEE_CLASS', 'INSURER', 'Insurer', 20),
    ('PAYEE_CLASS', 'EMPLOYEE', 'Employee', 30),
    ('PAYEE_CLASS', 'CLIENT', 'Client', 40),
    ('PAYEE_CLASS', 'GOVERNMENT', 'Government agency', 50),
    ('PAYEE_CLASS', 'OTHER', 'Others', 90),
    -- Disbursement types (DIS 2.6.1 manual encoding, DIS 3.25.1 automatic classification); the
    -- codes are the gateway types of opsledger DisbursementRequest.Type
    ('DISBURSEMENT_TYPE', 'REMITTANCE', 'Remittance', 10),
    ('DISBURSEMENT_TYPE', 'REFUND', 'Refund', 20),
    ('DISBURSEMENT_TYPE', 'SUPPLIER', 'Payment to supplier (other service provider)', 30),
    ('DISBURSEMENT_TYPE', 'GOVERNMENT', 'Payment to government agencies', 40),
    ('DISBURSEMENT_TYPE', 'OTHER_BANK_UNIT', 'Payment to other bank units', 50),
    ('DISBURSEMENT_TYPE', 'EMPLOYEE', 'Employee-related request', 60),
    ('DISBURSEMENT_TYPE', 'CASH_ADVANCE', 'Cash advance', 70),
    ('DISBURSEMENT_TYPE', 'SERVICE_FEE', 'Service fee', 80),
    ('DISBURSEMENT_TYPE', 'PASS_ON', 'Incentive pass-on', 85),
    ('DISBURSEMENT_TYPE', 'CWT2307', 'BIR 2307 release', 87),
    ('DISBURSEMENT_TYPE', 'OTHER', 'Other disbursement requests', 90),
    -- Cancellation and return reasons (proposed lists, AQ15)
    ('DISB_CANCEL_REASON', 'REQUESTED_BY_SOURCE', 'Cancellation requested by the requesting unit', 10),
    ('DISB_CANCEL_REASON', 'WRONG_PAYEE_OR_AMOUNT', 'Wrong payee, account or amount', 20),
    ('DISB_CANCEL_REASON', 'DUPLICATE', 'Duplicate request or DV', 30),
    ('DISB_CANCEL_REASON', 'CHECK_SPOILED', 'Check spoiled or lost', 40),
    ('DISB_CANCEL_REASON', 'OTHERS', 'Others (see comment)', 90),
    ('DISB_RETURN_REASON', 'PAYEE_NOT_MAINTAINED', 'Payee not maintained (DIS 3.25.0)', 10),
    ('DISB_RETURN_REASON', 'INCOMPLETE_DOCUMENTS', 'Incomplete supporting documents', 20),
    ('DISB_RETURN_REASON', 'INCORRECT_ENTRY', 'Incorrect accounting entry', 30),
    ('DISB_RETURN_REASON', 'OTHERS', 'Others (see comment)', 90),
    -- Refund reasons and categories (Appendix D; lists to confirm, AQ18)
    ('REFUND_REASON', 'CANCELLED_POLICY', 'Cancelled policy', 10),
    ('REFUND_REASON', 'OVERPAYMENT', 'Overpayment', 20),
    ('REFUND_REASON', 'DOUBLE_PAYMENT', 'Double payment', 30),
    ('REFUND_REASON', 'PREMIUM_DECREASE', 'Premium decrease (endorsement)', 40),
    ('REFUND_REASON', 'OTHERS', 'Others (see comment)', 90),
    ('RRF_CATEGORY_A', 'OTHERS', 'Others (to confirm, AQ18)', 90),
    ('RRF_CATEGORY_B', 'OTHERS', 'Others (to confirm, AQ18)', 90),
    -- ACSL case types and correction kinds (design 5.3)
    ('ACSL_CASE_TYPE', 'INVESTIGATION', 'Investigation', 10),
    ('ACSL_CASE_TYPE', 'ANALYSIS_REQUEST', 'Account analysis request', 20),
    ('ACSL_CASE_TYPE', 'CORRECTION', 'Correction entry', 30),
    ('ACSL_CASE_TYPE', 'REFUND_APPLICATION', 'AR refund payment application', 40),
    ('ACSL_CASE_TYPE', 'PAYMENT_REVERSAL', 'Sub-ledger payment reversal', 50),
    ('ACSL_CORRECTION_KIND', 'WRONG_ACCOUNT', 'Posting to a wrong GL account', 10),
    ('ACSL_CORRECTION_KIND', 'AMOUNT', 'Wrong amount', 20),
    ('ACSL_CORRECTION_KIND', 'RECLASS', 'Reclassification', 30),
    ('ACSL_CORRECTION_KIND', 'OTHER', 'Other correction', 90),
    -- Service fee segments (FRBS 2.10: 2.5% / 1% by segment; AQ20)
    ('SERVICE_FEE_SEGMENT', 'CBG', 'Consumer Banking Group', 10),
    ('SERVICE_FEE_SEGMENT', 'IBG', 'Institutional Banking Group', 20),
    ('SERVICE_FEE_SEGMENT', 'OTHERS', 'Others (to confirm, AQ20)', 90)
) as v(type_code, code, label, sort_order);

-- ---------- Workflow DISB_VOUCHER (DIS 2.7-2.21, design 7.1) ---------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('DISB_VOUCHER', 'IN_PROCESS', 'In process', 'DISB_PROCESS', 24, true, false, 10),
       ('DISB_VOUCHER', 'FOR_REVIEW', 'For review', 'DISB_REVIEW', 24, false, false, 20),
       ('DISB_VOUCHER', 'FOR_APPROVAL', 'For approval', 'DISB_APPROVE', 24, false, false, 30),
       ('DISB_VOUCHER', 'APPROVED', 'Approved', 'DISB_PROCESS', null, false, false, 40),
       ('DISB_VOUCHER', 'REJECTED', 'Rejected', null, null, false, true, 80),
       ('DISB_VOUCHER', 'CANCELLED', 'Cancelled', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('DISB_VOUCHER', 'IN_PROCESS', 'submit', 'FOR_REVIEW', 'Submit for review', 'DISB_PROCESS', false, null, 10),
       -- Gateway refunds and remittances with a maintained payee go straight to the approver (DIS 3.25.0)
       ('DISB_VOUCHER', 'IN_PROCESS', 'route_to_approver', 'FOR_APPROVAL', 'Route to the approver', 'DISB_PROCESS', false, null, 20),
       ('DISB_VOUCHER', 'IN_PROCESS', 'cancel', 'CANCELLED', 'Cancel', 'DISB_PROCESS', false, 'DISB_CANCEL_REASON', 90),
       ('DISB_VOUCHER', 'FOR_REVIEW', 'submit_for_approval', 'FOR_APPROVAL', 'Submit for approval', 'DISB_REVIEW', false, null, 10),
       ('DISB_VOUCHER', 'FOR_REVIEW', 'return', 'IN_PROCESS', 'Return to processor', 'DISB_REVIEW', true, 'DISB_RETURN_REASON', 80),
       ('DISB_VOUCHER', 'FOR_REVIEW', 'cancel', 'CANCELLED', 'Cancel', 'DISB_REVIEW', false, 'DISB_CANCEL_REASON', 90),
       ('DISB_VOUCHER', 'FOR_APPROVAL', 'approve', 'APPROVED', 'Approve', 'DISB_APPROVE', false, null, 10),
       ('DISB_VOUCHER', 'FOR_APPROVAL', 'return', 'IN_PROCESS', 'Return to processor', 'DISB_APPROVE', true, 'DISB_RETURN_REASON', 80),
       ('DISB_VOUCHER', 'FOR_APPROVAL', 'reject', 'REJECTED', 'Reject', 'DISB_APPROVE', false, 'DISB_RETURN_REASON', 85),
       -- Cancelling an approved DV reverses its entry and regularises the source (DIS 2.20.0)
       ('DISB_VOUCHER', 'APPROVED', 'cancel', 'CANCELLED', 'Cancel approved DV', 'DISB_APPROVE', false, 'DISB_CANCEL_REASON', 90);

-- ---------- Workflow DISB_STATUS_EDIT (DIS 2.8.5) -------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('DISB_STATUS_EDIT', 'REQUESTED', 'Status change requested', 'DISB_STATUS_APPROVE', 24, true, false, 10),
       ('DISB_STATUS_EDIT', 'APPLIED', 'Applied', null, null, false, true, 20),
       ('DISB_STATUS_EDIT', 'REJECTED', 'Rejected', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('DISB_STATUS_EDIT', 'REQUESTED', 'approve', 'APPLIED', 'Approve status change', 'DISB_STATUS_APPROVE', false, null, 10),
       ('DISB_STATUS_EDIT', 'REQUESTED', 'reject', 'REJECTED', 'Reject status change', 'DISB_STATUS_APPROVE', true, 'RETURN_REASON', 90);

-- ---------- Workflow DISB_FUNDING (DIS 2.17: maker, verifier, two approvers) ----------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('DISB_FUNDING', 'CREATED', 'Created', 'DISB_FUNDING_REQUEST', 8, true, false, 10),
       ('DISB_FUNDING', 'FOR_VERIFICATION', 'For verification', 'DISB_FUNDING_VERIFY', 8, false, false, 20),
       ('DISB_FUNDING', 'FOR_APPROVAL_1', 'For first approval', 'DISB_FUNDING_APPROVE', 8, false, false, 30),
       ('DISB_FUNDING', 'FOR_APPROVAL_2', 'For second approval', 'DISB_FUNDING_APPROVE', 8, false, false, 40),
       ('DISB_FUNDING', 'APPROVED', 'Approved', null, null, false, true, 50),
       ('DISB_FUNDING', 'DECLINED', 'Declined', null, null, false, true, 80),
       ('DISB_FUNDING', 'CANCELLED', 'Cancelled', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('DISB_FUNDING', 'CREATED', 'submit', 'FOR_VERIFICATION', 'Submit for verification', 'DISB_FUNDING_REQUEST', false, null, 10),
       ('DISB_FUNDING', 'CREATED', 'cancel', 'CANCELLED', 'Cancel', 'DISB_FUNDING_REQUEST', true, 'VOID_REASON', 90),
       ('DISB_FUNDING', 'FOR_VERIFICATION', 'verify', 'FOR_APPROVAL_1', 'Verify', 'DISB_FUNDING_VERIFY', false, null, 10),
       ('DISB_FUNDING', 'FOR_VERIFICATION', 'return', 'CREATED', 'Return to maker', 'DISB_FUNDING_VERIFY', true, 'RETURN_REASON', 80),
       ('DISB_FUNDING', 'FOR_APPROVAL_1', 'approve', 'FOR_APPROVAL_2', 'Approve (first)', 'DISB_FUNDING_APPROVE', false, null, 10),
       ('DISB_FUNDING', 'FOR_APPROVAL_1', 'return', 'CREATED', 'Return to maker', 'DISB_FUNDING_APPROVE', true, 'RETURN_REASON', 80),
       ('DISB_FUNDING', 'FOR_APPROVAL_1', 'decline', 'DECLINED', 'Decline', 'DISB_FUNDING_APPROVE', true, 'RETURN_REASON', 85),
       ('DISB_FUNDING', 'FOR_APPROVAL_2', 'approve', 'APPROVED', 'Approve (second)', 'DISB_FUNDING_APPROVE', false, null, 10),
       ('DISB_FUNDING', 'FOR_APPROVAL_2', 'return', 'CREATED', 'Return to maker', 'DISB_FUNDING_APPROVE', true, 'RETURN_REASON', 80),
       ('DISB_FUNDING', 'FOR_APPROVAL_2', 'decline', 'DECLINED', 'Decline', 'DISB_FUNDING_APPROVE', true, 'RETURN_REASON', 85);

-- ---------- Workflow DISB_PAYEE (DIS 2.2: maintenance, deactivation, reactivation) ------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('DISB_PAYEE', 'DRAFT', 'Draft', 'DISB_PAYEE_MAINTAIN', 24, true, false, 10),
       ('DISB_PAYEE', 'FOR_AUTHORIZATION', 'For authorisation', 'DISB_PAYEE_AUTHORIZE', 24, false, false, 20),
       ('DISB_PAYEE', 'ACTIVE', 'Active', null, null, false, false, 30),
       ('DISB_PAYEE', 'FOR_DEACTIVATION', 'For deactivation', 'DISB_PAYEE_AUTHORIZE', 24, false, false, 40),
       ('DISB_PAYEE', 'INACTIVE', 'Inactive', null, null, false, false, 50),
       ('DISB_PAYEE', 'FOR_REACTIVATION', 'For reactivation', 'DISB_PAYEE_AUTHORIZE', 24, false, false, 60);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('DISB_PAYEE', 'DRAFT', 'submit', 'FOR_AUTHORIZATION', 'Submit for authorisation', 'DISB_PAYEE_MAINTAIN', false, null, 10),
       ('DISB_PAYEE', 'FOR_AUTHORIZATION', 'authorize', 'ACTIVE', 'Authorise', 'DISB_PAYEE_AUTHORIZE', false, null, 10),
       ('DISB_PAYEE', 'FOR_AUTHORIZATION', 'return', 'DRAFT', 'Return to maker', 'DISB_PAYEE_AUTHORIZE', true, 'RETURN_REASON', 80),
       ('DISB_PAYEE', 'ACTIVE', 'deactivate', 'FOR_DEACTIVATION', 'Request deactivation', 'DISB_PAYEE_MAINTAIN', false, null, 10),
       ('DISB_PAYEE', 'FOR_DEACTIVATION', 'authorize', 'INACTIVE', 'Authorise deactivation', 'DISB_PAYEE_AUTHORIZE', false, null, 10),
       ('DISB_PAYEE', 'FOR_DEACTIVATION', 'return', 'ACTIVE', 'Keep active', 'DISB_PAYEE_AUTHORIZE', true, 'RETURN_REASON', 80),
       ('DISB_PAYEE', 'INACTIVE', 'reactivate', 'FOR_REACTIVATION', 'Request reactivation', 'DISB_PAYEE_MAINTAIN', false, null, 10),
       ('DISB_PAYEE', 'FOR_REACTIVATION', 'authorize', 'ACTIVE', 'Authorise reactivation', 'DISB_PAYEE_AUTHORIZE', false, null, 10),
       ('DISB_PAYEE', 'FOR_REACTIVATION', 'return', 'INACTIVE', 'Keep inactive', 'DISB_PAYEE_AUTHORIZE', true, 'RETURN_REASON', 80);

-- ---------- Workflow PRQ_REFUND (MKT 1.2-2.26, 1.11.0 validation) ----------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('PRQ_REFUND', 'DRAFT', 'Draft', 'PRQ_CREATE', 24, true, false, 10),
       ('PRQ_REFUND', 'PREPARING', 'Preparing', 'PRQ_CREATE', 24, false, false, 20),
       ('PRQ_REFUND', 'FOR_VALIDATION', 'With ACSL and Cashiering for validation', 'PRQ_ASSIGN', 48, false, false, 30),
       ('PRQ_REFUND', 'FOR_REVIEW', 'For review', 'PRQ_REVIEW', 24, false, false, 40),
       ('PRQ_REFUND', 'FOR_APPROVAL', 'For approval', 'PRQ_APPROVE', 24, false, false, 50),
       ('PRQ_REFUND', 'SENT_TO_DISBURSEMENT', 'Sent to Disbursement', null, null, false, false, 60),
       ('PRQ_REFUND', 'DISBURSED', 'Disbursed', null, null, false, true, 70),
       ('PRQ_REFUND', 'CANCELLED', 'Cancelled', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('PRQ_REFUND', 'DRAFT', 'assign', 'PREPARING', 'Assign preparer', 'PRQ_ASSIGN', false, null, 10),
       ('PRQ_REFUND', 'DRAFT', 'submit', 'FOR_REVIEW', 'Submit for review', 'PRQ_CREATE', false, null, 20),
       ('PRQ_REFUND', 'DRAFT', 'submit_for_validation', 'FOR_VALIDATION', 'Send for validation', 'PRQ_CREATE', false, null, 30),
       ('PRQ_REFUND', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel', 'PRQ_CREATE', true, 'VOID_REASON', 90),
       ('PRQ_REFUND', 'PREPARING', 'submit', 'FOR_REVIEW', 'Submit for review', 'PRQ_CREATE', false, null, 10),
       ('PRQ_REFUND', 'PREPARING', 'submit_for_validation', 'FOR_VALIDATION', 'Send for validation', 'PRQ_CREATE', false, null, 20),
       ('PRQ_REFUND', 'PREPARING', 'cancel', 'CANCELLED', 'Cancel', 'PRQ_CREATE,PRQ_ASSIGN', true, 'VOID_REASON', 90),
       -- Both validations answered (system, on RefundValidationCompleted)
       ('PRQ_REFUND', 'FOR_VALIDATION', 'validated', 'FOR_REVIEW', 'Validation confirmed', 'PRQ_ASSIGN', false, null, 10),
       ('PRQ_REFUND', 'FOR_VALIDATION', 'validation_failed', 'PREPARING', 'Validation rejected', 'PRQ_ASSIGN', false, null, 80),
       ('PRQ_REFUND', 'FOR_REVIEW', 'endorse', 'FOR_APPROVAL', 'Endorse for approval', 'PRQ_REVIEW', false, null, 10),
       ('PRQ_REFUND', 'FOR_REVIEW', 'return', 'PREPARING', 'Return to preparer', 'PRQ_REVIEW', true, 'RETURN_REASON', 80),
       ('PRQ_REFUND', 'FOR_REVIEW', 'cancel', 'CANCELLED', 'Cancel', 'PRQ_REVIEW', true, 'VOID_REASON', 90),
       ('PRQ_REFUND', 'FOR_APPROVAL', 'approve', 'SENT_TO_DISBURSEMENT', 'Approve and send to Disbursement', 'PRQ_APPROVE', false, null, 10),
       ('PRQ_REFUND', 'FOR_APPROVAL', 'return', 'FOR_REVIEW', 'Return to reviewer', 'PRQ_APPROVE', true, 'RETURN_REASON', 80),
       -- Disbursement feedback (system, on DisbursementStatusChanged)
       ('PRQ_REFUND', 'SENT_TO_DISBURSEMENT', 'disbursed', 'DISBURSED', 'Disbursed', 'PRQ_APPROVE', false, null, 10),
       ('PRQ_REFUND', 'SENT_TO_DISBURSEMENT', 'disbursement_returned', 'PREPARING', 'Returned by Disbursement', 'PRQ_APPROVE', false, null, 80);

-- ---------- Workflow PRQ_CASH_ADVANCE (MKT, RFP with HR approval) ------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('PRQ_CASH_ADVANCE', 'DRAFT', 'Draft', 'PRQ_CREATE', 24, true, false, 10),
       ('PRQ_CASH_ADVANCE', 'FOR_REVIEW', 'For review', 'PRQ_REVIEW', 24, false, false, 20),
       ('PRQ_CASH_ADVANCE', 'FOR_APPROVAL', 'For Marketing approval', 'PRQ_APPROVE', 24, false, false, 30),
       ('PRQ_CASH_ADVANCE', 'HR_APPROVAL', 'For HR approval', 'PRQ_HR_APPROVE', 24, false, false, 40),
       ('PRQ_CASH_ADVANCE', 'SENT_TO_DISBURSEMENT', 'Sent to Disbursement', null, null, false, false, 50),
       ('PRQ_CASH_ADVANCE', 'DISBURSED', 'Disbursed', null, null, false, true, 60),
       ('PRQ_CASH_ADVANCE', 'CANCELLED', 'Cancelled', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('PRQ_CASH_ADVANCE', 'DRAFT', 'submit', 'FOR_REVIEW', 'Submit for review', 'PRQ_CREATE', false, null, 10),
       ('PRQ_CASH_ADVANCE', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel', 'PRQ_CREATE', true, 'VOID_REASON', 90),
       ('PRQ_CASH_ADVANCE', 'FOR_REVIEW', 'endorse', 'FOR_APPROVAL', 'Endorse for approval', 'PRQ_REVIEW', false, null, 10),
       ('PRQ_CASH_ADVANCE', 'FOR_REVIEW', 'return', 'DRAFT', 'Return to requester', 'PRQ_REVIEW', true, 'RETURN_REASON', 80),
       ('PRQ_CASH_ADVANCE', 'FOR_APPROVAL', 'approve', 'HR_APPROVAL', 'Approve and send to HR', 'PRQ_APPROVE', false, null, 10),
       ('PRQ_CASH_ADVANCE', 'FOR_APPROVAL', 'return', 'FOR_REVIEW', 'Return to reviewer', 'PRQ_APPROVE', true, 'RETURN_REASON', 80),
       ('PRQ_CASH_ADVANCE', 'HR_APPROVAL', 'approve', 'SENT_TO_DISBURSEMENT', 'Approve and send to Disbursement', 'PRQ_HR_APPROVE', false, null, 10),
       ('PRQ_CASH_ADVANCE', 'HR_APPROVAL', 'return', 'FOR_APPROVAL', 'Return to Marketing approver', 'PRQ_HR_APPROVE', true, 'RETURN_REASON', 80),
       ('PRQ_CASH_ADVANCE', 'SENT_TO_DISBURSEMENT', 'disbursed', 'DISBURSED', 'Disbursed', 'PRQ_APPROVE', false, null, 10),
       ('PRQ_CASH_ADVANCE', 'SENT_TO_DISBURSEMENT', 'disbursement_returned', 'DRAFT', 'Returned by Disbursement', 'PRQ_APPROVE', false, null, 80);

-- ---------- Workflow PRQ_CHECK_CANCEL (MKT 1.19.0, 1.16.3) ----------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('PRQ_CHECK_CANCEL', 'REQUESTED', 'Requested', 'PRQ_CREATE', 24, true, false, 10),
       ('PRQ_CHECK_CANCEL', 'FOR_REVIEW', 'For review', 'PRQ_REVIEW', 24, false, false, 20),
       ('PRQ_CHECK_CANCEL', 'FOR_APPROVAL', 'For approval', 'PRQ_APPROVE', 24, false, false, 30),
       ('PRQ_CHECK_CANCEL', 'SENT', 'Sent to Disbursement', null, null, false, true, 40),
       ('PRQ_CHECK_CANCEL', 'CANCELLED', 'Withdrawn', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('PRQ_CHECK_CANCEL', 'REQUESTED', 'submit', 'FOR_REVIEW', 'Submit for review', 'PRQ_CREATE', false, null, 10),
       ('PRQ_CHECK_CANCEL', 'REQUESTED', 'cancel', 'CANCELLED', 'Withdraw', 'PRQ_CREATE', true, 'VOID_REASON', 90),
       ('PRQ_CHECK_CANCEL', 'FOR_REVIEW', 'endorse', 'FOR_APPROVAL', 'Endorse for approval', 'PRQ_REVIEW', false, null, 10),
       ('PRQ_CHECK_CANCEL', 'FOR_REVIEW', 'return', 'REQUESTED', 'Return to requester', 'PRQ_REVIEW', true, 'RETURN_REASON', 80),
       ('PRQ_CHECK_CANCEL', 'FOR_APPROVAL', 'approve', 'SENT', 'Approve and send to Disbursement', 'PRQ_APPROVE', false, null, 10),
       ('PRQ_CHECK_CANCEL', 'FOR_APPROVAL', 'return', 'FOR_REVIEW', 'Return to reviewer', 'PRQ_APPROVE', true, 'RETURN_REASON', 80);

-- ---------- Workflow ACSL_CASE (ACSL 2.5.x-2.6.x) ------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('ACSL_CASE', 'RECEIVED', 'Received', 'ACSL_ASSIGN', 24, true, false, 10),
       ('ACSL_CASE', 'ASSIGNED', 'Assigned', 'ACSL_PROCESS', 24, false, false, 20),
       ('ACSL_CASE', 'INVESTIGATING', 'Investigating', 'ACSL_PROCESS', 72, false, false, 30),
       ('ACSL_CASE', 'RESULT_PROVIDED', 'Result provided', null, null, false, true, 40),
       ('ACSL_CASE', 'CORRECTION', 'Sent for correction', null, null, false, true, 50);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('ACSL_CASE', 'RECEIVED', 'assign', 'ASSIGNED', 'Assign', 'ACSL_ASSIGN', false, null, 10),
       ('ACSL_CASE', 'ASSIGNED', 'start', 'INVESTIGATING', 'Start investigation', 'ACSL_PROCESS', true, null, 10),
       ('ACSL_CASE', 'ASSIGNED', 'reassign', 'RECEIVED', 'Send back for reassignment', 'ACSL_PROCESS,ACSL_ASSIGN', true, 'RETURN_REASON', 80),
       ('ACSL_CASE', 'INVESTIGATING', 'provide_result', 'RESULT_PROVIDED', 'Provide result', 'ACSL_PROCESS', false, null, 10),
       ('ACSL_CASE', 'INVESTIGATING', 'raise_correction', 'CORRECTION', 'Raise a correction entry', 'ACSL_PROCESS', false, null, 20);

-- ---------- Workflow ACSL_CORRECTION (ACSL 2.7-2.15, 2.9.1) ----------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('ACSL_CORRECTION', 'ASSIGNED', 'To assign', 'ACSL_ASSIGN', 24, true, false, 10),
       ('ACSL_CORRECTION', 'DRAFT', 'Draft', 'ACSL_PROCESS', 48, false, false, 20),
       ('ACSL_CORRECTION', 'FOR_REVIEW', 'For review', 'ACSL_REVIEW', 24, false, false, 30),
       ('ACSL_CORRECTION', 'FOR_APPROVAL', 'For approval', 'ACSL_APPROVE', 24, false, false, 40),
       ('ACSL_CORRECTION', 'POSTED', 'Posted', null, null, false, true, 50),
       ('ACSL_CORRECTION', 'CANCELLED', 'Cancelled', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('ACSL_CORRECTION', 'ASSIGNED', 'assign', 'DRAFT', 'Assign preparer', 'ACSL_ASSIGN', false, null, 10),
       ('ACSL_CORRECTION', 'DRAFT', 'submit', 'FOR_REVIEW', 'Submit for review', 'ACSL_PROCESS', false, null, 10),
       ('ACSL_CORRECTION', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel', 'ACSL_PROCESS', true, 'VOID_REASON', 90),
       ('ACSL_CORRECTION', 'FOR_REVIEW', 'endorse', 'FOR_APPROVAL', 'Endorse for approval', 'ACSL_REVIEW', false, null, 10),
       ('ACSL_CORRECTION', 'FOR_REVIEW', 'return', 'DRAFT', 'Return to preparer', 'ACSL_REVIEW', true, 'RETURN_REASON', 80),
       ('ACSL_CORRECTION', 'FOR_APPROVAL', 'approve', 'POSTED', 'Approve and post', 'ACSL_APPROVE', false, null, 10),
       ('ACSL_CORRECTION', 'FOR_APPROVAL', 'return', 'DRAFT', 'Return to preparer', 'ACSL_APPROVE', true, 'RETURN_REASON', 80);

-- ---------- Workflow REM_DEDUCTION (ACSL 2.9.2, used by remittance) ---------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('REM_DEDUCTION', 'DRAFT', 'Draft', 'ACSL_PROCESS', 48, true, false, 10),
       ('REM_DEDUCTION', 'FOR_CONFIRMATION', 'For confirmation', 'REMIT_DEDUCTION_CONFIRM', 24, false, false, 20),
       ('REM_DEDUCTION', 'CONFIRMED', 'Confirmed - awaiting the next batch', null, null, false, false, 30),
       ('REM_DEDUCTION', 'APPLIED', 'Applied to a remittance batch', null, null, false, true, 40),
       ('REM_DEDUCTION', 'CANCELLED', 'Cancelled', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('REM_DEDUCTION', 'DRAFT', 'submit', 'FOR_CONFIRMATION', 'Submit with insurer confirmation', 'ACSL_PROCESS', false, null, 10),
       ('REM_DEDUCTION', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel', 'ACSL_PROCESS', true, 'VOID_REASON', 90),
       ('REM_DEDUCTION', 'FOR_CONFIRMATION', 'confirm', 'CONFIRMED', 'Confirm deduction', 'REMIT_DEDUCTION_CONFIRM', false, null, 10),
       ('REM_DEDUCTION', 'FOR_CONFIRMATION', 'return', 'DRAFT', 'Return to processor', 'REMIT_DEDUCTION_CONFIRM', true, 'RETURN_REASON', 80),
       -- Consumed by the next remittance batch of the insurer (system)
       ('REM_DEDUCTION', 'CONFIRMED', 'apply', 'APPLIED', 'Applied to a remittance batch', 'REMIT_PROCESS', false, null, 10);

-- ---------- Workflow FRBS_SERVICE_FEE (FRBS 2.10) ------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('FRBS_SERVICE_FEE', 'COMPUTED', 'Computed', 'SERVICE_FEE_MANAGE', 48, true, false, 10),
       ('FRBS_SERVICE_FEE', 'FOR_APPROVAL', 'For approval', 'SERVICE_FEE_APPROVE', 24, false, false, 20),
       ('FRBS_SERVICE_FEE', 'APPROVED', 'Approved - sent for payment', 'SERVICE_FEE_TAG', null, false, false, 30),
       ('FRBS_SERVICE_FEE', 'RELEASED', 'Released', 'SERVICE_FEE_TAG', null, false, false, 40),
       ('FRBS_SERVICE_FEE', 'LIQUIDATED', 'Liquidated', null, null, false, true, 50),
       ('FRBS_SERVICE_FEE', 'CANCELLED', 'Cancelled', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('FRBS_SERVICE_FEE', 'COMPUTED', 'submit', 'FOR_APPROVAL', 'Submit for approval', 'SERVICE_FEE_MANAGE', false, null, 10),
       ('FRBS_SERVICE_FEE', 'COMPUTED', 'cancel', 'CANCELLED', 'Cancel run', 'SERVICE_FEE_MANAGE', true, 'VOID_REASON', 90),
       ('FRBS_SERVICE_FEE', 'FOR_APPROVAL', 'approve', 'APPROVED', 'Approve and send for payment', 'SERVICE_FEE_APPROVE', false, null, 10),
       ('FRBS_SERVICE_FEE', 'FOR_APPROVAL', 'return', 'COMPUTED', 'Return to processor', 'SERVICE_FEE_APPROVE', true, 'RETURN_REASON', 80),
       ('FRBS_SERVICE_FEE', 'APPROVED', 'release', 'RELEASED', 'Tag released', 'SERVICE_FEE_TAG', false, null, 10),
       ('FRBS_SERVICE_FEE', 'RELEASED', 'liquidate', 'LIQUIDATED', 'Tag liquidated', 'SERVICE_FEE_TAG', false, null, 10);

-- ---------- Accounting event types (design section 6; seed rules in V999, real rules AQ02) --------
insert into acc_event_type (code, name, category, journal_type, description, amount_components) values
 ('DISB_VOUCHER', 'Disbursement voucher approved', 'PAYMENT', 'PAYMENT',
  'Approved DV (DIS 2.7.6, 2.18.0): the gross is supplied in the component of the DV type (the payable settled), '
  || 'PAID is credited to @PAY_ACCOUNT (paying bank, or the checks-outstanding clearing for checks) and EWT to '
  || 'withholding tax payable. An approved DV that is cancelled re-posts with negative amounts (DV:<no>:CANCEL, '
  || 'DIS 2.20.0). Rows 1-7, 10 and 11 of the design.',
  'REMITTANCE,REFUND,REFUND_FROM_INSURER,SUPPLIER,GOVERNMENT,OTHER_BANK_UNIT,EMPLOYEE,CASH_ADVANCE,SERVICE_FEE,PASS_ON,STALE_REISSUE,OTHER,PAID,EWT'),
 ('DISB_CHECK_NEGOTIATED', 'Check negotiated', 'PAYMENT', 'PAYMENT',
  'A released check was negotiated (deposited-checks file, DIS 3.26.1): checks outstanding cleared against the '
  || 'bank when DISB_CHECK_CLEARING = ON.',
  'AMOUNT'),
 ('DISB_CHECK_STALE', 'Check staled', 'PAYMENT', 'PAYMENT',
  'A printed or released check reached DISB_STALE_DAYS (DIS 3.26.2, 3.27.1): checks outstanding moved to '
  || 'Miscellaneous Liability - stale checks of the payee.',
  'AMOUNT'),
 ('DISB_FUND_TRANSFER', 'Account funding transfer', 'TREASURY', 'PAYMENT',
  'Funding of the main BDOIR account through BDO Business Online Banking (DIS 2.17): target bank (@TARGET_BANK) '
  || 'against source bank (@SOURCE_BANK).',
  'AMOUNT'),
 ('TAX_CWT_CERT_RECEIVED', 'Insurer 2307 certificate received', 'RECEIPT', 'RECEIPT',
  'An insurer creditable withholding tax certificate was tagged as received (DIS 2.11): AR-BIR on commission or on '
  || 'incentives moved to AR-BIR on hand.',
  'COMMISSION_CWT,INCENTIVE_CWT'),
 ('OPS_REMIT_CPC2', 'CPC2 incentive per remittance batch', 'COMMISSION', 'COMMISSION',
  'CPC2 incentive earned on packaged Fire and Motor products, computed per remittance batch line from the incentive '
  || 'criteria (DIS 3.29.2): deducted from the amount due to the insurer as Other Income with output VAT.',
  'GROSS,CPC2_INCOME,OUTPUT_VAT'),
 ('OPS_REMIT_DEDUCTION', 'Remittance deduction on insurer confirmation', 'PAYMENT', 'PAYMENT',
  'An AR insurer''s refund confirmed by the insurer is deducted from the next remittance batch of that insurer (ACSL '
  || '2.9.2): due to insurer for disbursement against AR insurer''s refund.',
  'AMOUNT'),
 ('FRBS_SERVICE_FEE_ACCRUE', 'Service fee accrual', 'PAYMENT', 'PAYMENT',
  'Service fee of the units and referrers on fully paid commission (FRBS 2.10): expense (cost centre) against service '
  || 'fee payable.',
  'AMOUNT'),
 ('PRQ_CA_LIQUIDATION', 'Cash advance liquidation', 'PAYMENT', 'PAYMENT',
  'Liquidation of an employee cash advance (Appendix D, AQ18): expenses by category against advances to employees; '
  || 'excess returned (CASH_RETURNED) or shortage payable to the employee (SHORTAGE).',
  'PER_DIEM,REPRESENTATION,TRANSPORT,LODGING,OTHER,CASH_RETURNED,SHORTAGE,ADVANCE');

-- ---------- Business parameters (design section 9) --------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CLOSE_ONLY_PREVIOUS_MONTH', 'true', 'BOOLEAN', 'ACCOUNTING',
     'Month-end close only for the previous month and year (FRBS 2.6.1, AQ04)', null, null, now(), 'SYSTEM'),
    ('YEAR_END_CLOSE_DEADLINE', '04-15', 'STRING', 'ACCOUNTING',
     'Month and day (MM-DD) by which the previous year must be closed; YEAR_END_CLOSE_DUE is raised before (FRBS 2.7.x)', null, null, now(), 'SYSTEM'),
    ('BROKING_CLOSE_TIME', '23:00', 'STRING', 'ACCOUNTING',
     'Time (PHT, HH:MM) at which the broking books are closed on the last day of the month (FRBS 3.4.0/3.4.1, AQ04)', null, null, now(), 'SYSTEM'),
    ('OPS_BOOK_RATE_SOURCE', 'CLOSING_PREV_MONTH', 'STRING', 'ACCOUNTING',
     'Source of the Operations BOOK rate: CLOSING_PREV_MONTH copies the revaluation rate of the previous month end, MANUAL keeps it by hand (FRBS 2.2.0, OQ08, AQ03)', null, null, now(), 'SYSTEM'),
    ('DISB_STALE_DAYS', '180', 'INTEGER', 'DISBURSEMENT',
     'Days after the print date at which a printed or released check becomes stale (DIS 3.26.2)', 30, 720, now(), 'SYSTEM'),
    ('DISB_CHECK_CLEARING', 'ON', 'STRING', 'DISBURSEMENT',
     'ON: issued checks are credited to checks outstanding until negotiated; OFF: to the bank at approval (DIS 3.26.1, 3.27.1)', null, null, now(), 'SYSTEM'),
    ('DISB_AUTO_APPROVER_ROUTING', 'REFUND,REMITTANCE', 'CODE_LIST', 'DISBURSEMENT',
     'Gateway request types whose DV is built automatically and routed straight to the approver (DIS 3.25.0)', null, null, now(), 'SYSTEM'),
    ('EARLY_INCENTIVE_WTAX_RATE', '2', 'DECIMAL', 'DISBURSEMENT',
     'Withholding tax rate (percent) of the early-incentive service invoice (DIS 3.29.1, AQ25)', 0, 100, now(), 'SYSTEM');

-- ---------- Exception codes (design section 9) ------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('COST_CENTER_MISSING', 'Cost centre missing',
     'A posting to an account that requires a cost centre found no cost-centre rule (FRBS 3.1.1, DIS 3.30.0).',
     'ACCOUNTING', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('YEAR_END_CLOSE_DUE', 'Year-end close due',
     'The previous year is not closed and YEAR_END_CLOSE_DEADLINE is near (FRBS 2.7.x).',
     'CLOSING', 'HIGH', null, 15, now(), 'SYSTEM'),
    ('GL_CLOSE_FAILED', 'Scheduled close failed',
     'A scheduled month-end or broking books close did not complete; the checklist lists the blocking items (FRBS 2.6.0, 3.4.0).',
     'CLOSING', 'HIGH', null, null, now(), 'SYSTEM'),
    ('DISB_PAYEE_NO_MATCH', 'Payment request without payee',
     'A system-triggered payment request names a payee that is not maintained; it was returned to its source (DIS 3.25.0).',
     'DISBURSEMENT', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('DISB_UNREGULARIZED', 'Cancelled DV not regularised',
     'A source module has not restored its records after the cancellation of an approved DV (DIS 2.20.0).',
     'DISBURSEMENT', 'HIGH', null, 2, now(), 'SYSTEM'),
    ('DISB_CHECK_STALE', 'Check staled',
     'A printed or released check reached DISB_STALE_DAYS and was moved to Miscellaneous Liability (DIS 3.26.2).',
     'DISBURSEMENT', 'LOW', null, 180, now(), 'SYSTEM'),
    ('ACSL_GLSL_DIFFERENCE', 'GL and sub-ledger differ',
     'The daily GL-SL reconciliation found a difference between a control account and its sub-ledger (ACSL 2.13.2).',
     'ACSL', 'HIGH', 1.00, null, now(), 'SYSTEM'),
    ('CHECK_SERIES_LOW', 'Check series running low',
     'A cheque book of a paying bank account reached its warning threshold of remaining checks (DIS 2.24.2).',
     'DISBURSEMENT', 'MEDIUM', null, null, now(), 'SYSTEM');
