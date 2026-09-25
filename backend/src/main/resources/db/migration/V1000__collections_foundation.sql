-- =====================================================================================
-- iNXT BrokerVerse - V1000 Collections (BRD-4) foundation: roles and grants, permission action
-- classes, lists of values and their attributes, workflow CLX_ESCALATION, business parameters,
-- exception codes, notification events, the in-app transport of the Collection feeds and the
-- availability time of scheduled report files.
--   Requirements: docs/requirements/BDOI_CLXN_BRD_SPEC.md
--     BRCLXN.005-007  minimal balance threshold, aging basis and brackets (CQ03)
--     BRCLXN.016-018, 037-038  PR and unapplied-payment disposition LOVs with attributes (CQ08)
--     BRCLXN.024-029, 041, 045  scheduled files available from a given time (report_run)
--     BRCLXN.047      invoice number pattern of "For application to invoice" (CQ13)
--     BRCLXN.049/050/055  escalation rules, manual escalation, promises (workflow CLX_ESCALATION)
--     BRCLXN.051/052/053/058  bulk update, reassignment, installments, billing frequencies
--     NFR 1-2         lockout after 3 failed logins (LOGIN_MAX_FAILED_ATTEMPTS, CQ23)
--     NFR (UI/UX)     record lock "<user> is editing" (CLX_EDIT_LOCK_MINUTES)
--   Design: docs/architecture/COLLECTIONS_DESIGN.md sections 2.2 (feeds), 3 (Flyway: V890 of the
--   design is this V1000), 6 (security), 7 (workflow) and 8 (parameters, LOVs, alerts,
--   notification events). The permissions are the enum security.domain.Permission; the grants are
--   a proposal until OQ48 / CQ23 are answered. The Collections tables come with C1-A / B / C
--   (V1001-V1005); demo users and data with V1900-V1901.
--   The catalog "sales unit head" contract (design section 9) is deferred to wave C1-A.
--   Runs after V890 (BRD-5 roles ACSL_*) and every V7xx / V8xx migration; references only
--   platform tables by key and ops_flow_in_feed / report_run by value.
-- =====================================================================================

-- ---------- Collections role profiles (design 6.2) -------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('MKT_HANDLER', 'Marketing Handler', now(), 'SYSTEM'),
       ('CLX_TL', 'Collection Team Lead', now(), 'SYSTEM'),
       ('MKT_SECTION_HEAD', 'Marketing Section Head (Corporate / Retail)', now(), 'SYSTEM'),
       ('UNAPPLIED_HANDLER', 'Unapplied Payment Handler', now(), 'SYSTEM'),
       ('APP_SUPPORT', 'Application Support', now(), 'SYSTEM'),
       ('DCO', 'Data Control Officer', now(), 'SYSTEM');

-- Every new role: My Work, documents, report catalogue, client and account look-ups, Operations
-- home and Invoice 360 (the collection account links to it).
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('WORK_VIEW'), ('ATTACHMENT_VIEW'), ('REPORT_VIEW'), ('CLIENT_VIEW'),
                   ('ACCOUNT_VIEW'), ('OPS_VIEW')) as p(permission)
where r.code in ('MKT_HANDLER', 'CLX_TL', 'MKT_SECTION_HEAD', 'UNAPPLIED_HANDLER', 'APP_SUPPORT', 'DCO')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Role-specific grants (design 6.2).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Marketing AO: own accounts
    ('MKT_AO', 'CLX_VIEW'), ('MKT_AO', 'CLX_WORK'), ('MKT_AO', 'CLX_ESCALATE'), ('MKT_AO', 'CLX_UNAPPLIED_WORK'),
    ('MKT_AO', 'CLX_REPORT_VIEW'), ('MKT_AO', 'CLX_EXPORT'), ('MKT_AO', 'OPS_VIEW'),
    -- Marketing Team Lead: + escalations, assignment, bulk update, billing
    ('MKT_TL', 'CLX_VIEW'), ('MKT_TL', 'CLX_WORK'), ('MKT_TL', 'CLX_ESCALATE'), ('MKT_TL', 'CLX_UNAPPLIED_WORK'),
    ('MKT_TL', 'CLX_REPORT_VIEW'), ('MKT_TL', 'CLX_EXPORT'), ('MKT_TL', 'CLX_ESCALATION_HANDLE'),
    ('MKT_TL', 'CLX_ASSIGN'), ('MKT_TL', 'CLX_BULK_UPDATE'), ('MKT_TL', 'CLX_BILLING'),
    -- Marketing Handler: as the AO + bulk update
    ('MKT_HANDLER', 'CLX_VIEW'), ('MKT_HANDLER', 'CLX_WORK'), ('MKT_HANDLER', 'CLX_ESCALATE'),
    ('MKT_HANDLER', 'CLX_UNAPPLIED_WORK'), ('MKT_HANDLER', 'CLX_REPORT_VIEW'), ('MKT_HANDLER', 'CLX_EXPORT'),
    ('MKT_HANDLER', 'CLX_BULK_UPDATE'),
    -- Collection Handler (existing Operations role MKT_COLLECTION)
    ('MKT_COLLECTION', 'CLX_VIEW'), ('MKT_COLLECTION', 'CLX_WORK'), ('MKT_COLLECTION', 'CLX_BULK_UPDATE'),
    ('MKT_COLLECTION', 'CLX_ESCALATE'), ('MKT_COLLECTION', 'CLX_UNAPPLIED_WORK'), ('MKT_COLLECTION', 'CLX_BILLING'),
    ('MKT_COLLECTION', 'CLX_REPORT_VIEW'), ('MKT_COLLECTION', 'CLX_EXPORT'),
    -- Collection Team Lead: + escalations and assignment
    ('CLX_TL', 'CLX_VIEW'), ('CLX_TL', 'CLX_WORK'), ('CLX_TL', 'CLX_BULK_UPDATE'), ('CLX_TL', 'CLX_ESCALATE'),
    ('CLX_TL', 'CLX_UNAPPLIED_WORK'), ('CLX_TL', 'CLX_BILLING'), ('CLX_TL', 'CLX_REPORT_VIEW'),
    ('CLX_TL', 'CLX_EXPORT'), ('CLX_TL', 'CLX_ESCALATION_HANDLE'), ('CLX_TL', 'CLX_ASSIGN'),
    -- Section Head: + Collections set-up and audit log
    ('MKT_SECTION_HEAD', 'CLX_VIEW'), ('MKT_SECTION_HEAD', 'CLX_WORK'), ('MKT_SECTION_HEAD', 'CLX_BULK_UPDATE'),
    ('MKT_SECTION_HEAD', 'CLX_ESCALATE'), ('MKT_SECTION_HEAD', 'CLX_UNAPPLIED_WORK'),
    ('MKT_SECTION_HEAD', 'CLX_BILLING'), ('MKT_SECTION_HEAD', 'CLX_REPORT_VIEW'), ('MKT_SECTION_HEAD', 'CLX_EXPORT'),
    ('MKT_SECTION_HEAD', 'CLX_ESCALATION_HANDLE'), ('MKT_SECTION_HEAD', 'CLX_ASSIGN'),
    ('MKT_SECTION_HEAD', 'CLX_SETUP'), ('MKT_SECTION_HEAD', 'CLX_AUDIT_VIEW'),
    -- Unapplied Payment Handler (CQ10)
    ('UNAPPLIED_HANDLER', 'CLX_VIEW'), ('UNAPPLIED_HANDLER', 'CLX_UNAPPLIED_WORK'),
    ('UNAPPLIED_HANDLER', 'CLX_REPORT_VIEW'),
    -- Processing Unit: dispositions limited to "no / missing policy number" (LOV attribute allowed_roles)
    ('PROCESSOR', 'CLX_VIEW'), ('PROCESSOR', 'CLX_WORK'),
    -- Operations Cashiering, Disbursement, Comptrollership, ACSL: viewers
    ('CASHIER', 'CLX_VIEW'), ('CASHIER', 'CLX_REPORT_VIEW'), ('CASHIER', 'CLX_EXPORT'),
    ('CASHIER_TL', 'CLX_VIEW'), ('CASHIER_TL', 'CLX_REPORT_VIEW'), ('CASHIER_TL', 'CLX_EXPORT'),
    ('DISBURSEMENT', 'CLX_VIEW'), ('DISBURSEMENT', 'CLX_REPORT_VIEW'), ('DISBURSEMENT', 'CLX_EXPORT'),
    ('COMPTROLLERSHIP', 'CLX_VIEW'), ('COMPTROLLERSHIP', 'CLX_REPORT_VIEW'),
    ('ACSL_PROCESSOR', 'CLX_VIEW'), ('ACSL_PROCESSOR', 'CLX_REPORT_VIEW'),
    ('ACSL_TL', 'CLX_VIEW'), ('ACSL_TL', 'CLX_REPORT_VIEW'),
    ('ACSL_HEAD', 'CLX_VIEW'), ('ACSL_HEAD', 'CLX_REPORT_VIEW'),
    -- Application support, business administrator and DCO: set-up, audit, sync view, job runs
    ('APP_SUPPORT', 'CLX_VIEW'), ('APP_SUPPORT', 'CLX_SETUP'), ('APP_SUPPORT', 'CLX_AUDIT_VIEW'),
    ('APP_SUPPORT', 'FLOWIN_MANAGE'), ('APP_SUPPORT', 'SYSTEM_MONITOR'), ('APP_SUPPORT', 'LOV_MANAGE'),
    ('BUSINESS_ADMIN', 'CLX_VIEW'), ('BUSINESS_ADMIN', 'CLX_SETUP'), ('BUSINESS_ADMIN', 'CLX_AUDIT_VIEW'),
    ('BUSINESS_ADMIN', 'FLOWIN_MANAGE'), ('BUSINESS_ADMIN', 'SYSTEM_MONITOR'),
    ('DCO', 'CLX_VIEW'), ('DCO', 'CLX_SETUP'), ('DCO', 'CLX_AUDIT_VIEW'), ('DCO', 'FLOWIN_MANAGE'),
    ('DCO', 'SYSTEM_MONITOR'),
    -- Platform administrator and auditor: read access, set-up and audit
    ('SYSADMIN', 'CLX_VIEW'), ('SYSADMIN', 'CLX_SETUP'), ('SYSADMIN', 'CLX_AUDIT_VIEW'),
    ('SYSADMIN', 'CLX_REPORT_VIEW'),
    ('AUDITOR', 'CLX_VIEW'), ('AUDITOR', 'CLX_REPORT_VIEW'), ('AUDITOR', 'CLX_AUDIT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix, PMADD05) ---------------------------
insert into sec_permission_action (permission, area, action)
values
    ('CLX_VIEW', 'COLLECTIONS', 'VIEW'),
    ('CLX_REPORT_VIEW', 'COLLECTIONS', 'VIEW'),
    ('CLX_EXPORT', 'COLLECTIONS', 'VIEW'),
    ('CLX_AUDIT_VIEW', 'COLLECTIONS', 'VIEW'),
    ('CLX_WORK', 'COLLECTIONS', 'CREATE'),
    ('CLX_WORK', 'COLLECTIONS', 'AMEND'),
    ('CLX_UNAPPLIED_WORK', 'COLLECTIONS', 'CREATE'),
    ('CLX_ESCALATE', 'COLLECTIONS', 'CREATE'),
    ('CLX_BILLING', 'COLLECTIONS', 'CREATE'),
    ('CLX_BULK_UPDATE', 'COLLECTIONS', 'AMEND'),
    ('CLX_ASSIGN', 'COLLECTIONS', 'AMEND'),
    ('CLX_SETUP', 'COLLECTIONS', 'AMEND'),
    ('CLX_ESCALATION_HANDLE', 'COLLECTIONS', 'APPROVE');

-- ---------- Lists of values (design section 8) ------------------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('CLX_PR_DISPOSITION', 'PR collector disposition', 'Collector disposition of an outstanding premium receivable; attributes in clx_lov_attribute (BRCLXN.016-023, CQ08)', true, now(), 'SYSTEM'),
       ('CLX_UPP_DISPOSITION', 'Unapplied payment disposition (collector)', 'Collector disposition of an unapplied payment; attributes in clx_lov_attribute (BRCLXN.031-033, 037-038, 047/048, CQ08)', true, now(), 'SYSTEM'),
       ('CLX_EFFORT_CODE', 'Collection effort code', 'Kind of a collection effort (p.43-46, p.59; CQ08)', true, now(), 'SYSTEM'),
       ('CLX_BILLING_FREQUENCY', 'Billing frequency', 'Billing frequency of an installment plan or a billing statement cycle (BRCLXN.053/058, CQ15)', true, now(), 'SYSTEM'),
       ('CLX_ESCALATION_REASON', 'Escalation reason', 'Why an account is escalated (BRCLXN.049/050)', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    -- PR collector dispositions implied by the process (p.40-42); the full list comes from BDOI (CQ08)
    ('CLX_PR_DISPOSITION', 'DP_PR_FOR_REVERSAL', 'DP PR for reversal (to confirm)', 10),
    ('CLX_PR_DISPOSITION', 'PR2307_FOR_REVERSAL', 'PR 2307 for reversal (to confirm)', 20),
    ('CLX_PR_DISPOSITION', 'FOR_CHECK_PICKUP', 'For check pick-up (to confirm)', 30),
    ('CLX_PR_DISPOSITION', 'CANCEL_ACCOUNT', 'Cancel account (to confirm)', 40),
    ('CLX_PR_DISPOSITION', 'COORDINATE_FURTHER', 'Coordinate further (to confirm)', 50),
    ('CLX_PR_DISPOSITION', 'BANK_AO_ASSISTANCE', 'Request bank AO assistance (to confirm)', 60),
    ('CLX_PR_DISPOSITION', 'NO_POLICY_NUMBER', 'No / missing policy number (to confirm)', 70),
    ('CLX_PR_DISPOSITION', 'DP_RETURNED', 'DP returned by insurer (to confirm)', 80),
    -- Unapplied payment dispositions (BRCLXN.030-033); the full list comes from BDOI (CQ08)
    ('CLX_UPP_DISPOSITION', 'FOR_APPLICATION_TO_INVOICE', 'For application to invoice (to confirm)', 10),
    ('CLX_UPP_DISPOSITION', 'FOR_REFUND', 'For refund (to confirm)', 20),
    ('CLX_UPP_DISPOSITION', 'FOR_RECLASS', 'For reclass (to confirm)', 30),
    ('CLX_UPP_DISPOSITION', 'FOR_TRANSFER', 'For transfer to other unit (to confirm)', 40),
    ('CLX_UPP_DISPOSITION', 'COORDINATE_FURTHER', 'Coordinate further (to confirm)', 50),
    -- Collection effort codes (p.59 "last collection effort code"); values from BDOI (CQ08)
    ('CLX_EFFORT_CODE', 'CALL', 'Phone call (to confirm)', 10),
    ('CLX_EFFORT_CODE', 'EMAIL', 'E-mail (to confirm)', 20),
    ('CLX_EFFORT_CODE', 'VISIT', 'Client visit (to confirm)', 30),
    ('CLX_EFFORT_CODE', 'SOA_SENT', 'Statement of account sent (to confirm)', 40),
    ('CLX_EFFORT_CODE', 'OTHERS', 'Others (see remarks)', 90),
    -- Billing frequencies (BRCLXN.058)
    ('CLX_BILLING_FREQUENCY', 'ANNUAL', 'Annual', 10),
    ('CLX_BILLING_FREQUENCY', 'SEMI_ANNUAL', 'Semi-annual', 20),
    ('CLX_BILLING_FREQUENCY', 'QUARTERLY', 'Quarterly', 30),
    ('CLX_BILLING_FREQUENCY', 'MONTHLY', 'Monthly', 40),
    -- Escalation reasons (BRCLXN.049/050)
    ('CLX_ESCALATION_REASON', 'NO_COMMITMENT', 'No payment commitment', 10),
    ('CLX_ESCALATION_REASON', 'BROKEN_PROMISE', 'Broken promise to pay', 20),
    ('CLX_ESCALATION_REASON', 'AGING', 'Aging beyond the threshold', 30),
    ('CLX_ESCALATION_REASON', 'INSTALLMENT_OVERDUE', 'Installment overdue', 40),
    ('CLX_ESCALATION_REASON', 'CTE_REFUSED', 'Credit term extension refused by the insurer', 50),
    ('CLX_ESCALATION_REASON', 'OTHERS', 'Others (see remarks)', 90)
) as v(type_code, code, label, sort_order);

-- Attributes of the Collections LOV values (BRCLXN.016, 037, 047/048). lov_value has no attribute
-- columns, so Collections keeps them by (type, code, attribute). Owned by collections: C1-A reads
-- the CLX_PR_DISPOSITION rows, C1-C the CLX_UPP_DISPOSITION rows; Collections Setup maintains them.
--   CLX_PR_DISPOSITION: category (A / B / C, CQ08), tagging_owner (MARKETING / OPERATIONS),
--     ops_action (NONE / CWT2307_REVERSAL / DP_REVERSAL / CHECK_PICKUP / CANCEL_REQUEST),
--     allowed_roles (comma-separated role codes; empty = every role with CLX_WORK)
--   CLX_UPP_DISPOSITION: requires_invoice (true / false), cashiering_action (APPLY_TO_INVOICE /
--     REFUND / RECLASS / TRANSFER / NONE = UnappliedDispositionRequests.Action)
create table clx_lov_attribute (
    id          bigint generated by default as identity primary key,
    version     bigint        not null default 0,
    type_code   varchar(40)   not null references lov_type (code),
    code        varchar(40)   not null,
    attribute   varchar(40)   not null,
    value       varchar(200)  not null,
    created_at  timestamptz   not null,
    created_by  varchar(50)   not null,
    updated_at  timestamptz,
    updated_by  varchar(50),
    constraint uq_clx_lov_attribute unique (type_code, code, attribute),
    constraint fk_clx_lov_attribute_value foreign key (type_code, code) references lov_value (type_code, code),
    constraint ck_clx_lov_attribute_name check (attribute in ('category', 'tagging_owner', 'ops_action',
        'allowed_roles', 'requires_invoice', 'cashiering_action'))
);

insert into clx_lov_attribute (type_code, code, attribute, value, created_at, created_by)
select v.type_code, v.code, v.attribute, v.value, now(), 'SYSTEM'
from (values
    ('CLX_PR_DISPOSITION', 'DP_PR_FOR_REVERSAL', 'tagging_owner', 'OPERATIONS'),
    ('CLX_PR_DISPOSITION', 'DP_PR_FOR_REVERSAL', 'ops_action', 'DP_REVERSAL'),
    ('CLX_PR_DISPOSITION', 'PR2307_FOR_REVERSAL', 'tagging_owner', 'OPERATIONS'),
    ('CLX_PR_DISPOSITION', 'PR2307_FOR_REVERSAL', 'ops_action', 'CWT2307_REVERSAL'),
    ('CLX_PR_DISPOSITION', 'FOR_CHECK_PICKUP', 'tagging_owner', 'MARKETING'),
    ('CLX_PR_DISPOSITION', 'FOR_CHECK_PICKUP', 'ops_action', 'CHECK_PICKUP'),
    ('CLX_PR_DISPOSITION', 'CANCEL_ACCOUNT', 'tagging_owner', 'OPERATIONS'),
    ('CLX_PR_DISPOSITION', 'CANCEL_ACCOUNT', 'ops_action', 'CANCEL_REQUEST'),
    ('CLX_PR_DISPOSITION', 'COORDINATE_FURTHER', 'tagging_owner', 'MARKETING'),
    ('CLX_PR_DISPOSITION', 'COORDINATE_FURTHER', 'ops_action', 'NONE'),
    ('CLX_PR_DISPOSITION', 'BANK_AO_ASSISTANCE', 'tagging_owner', 'MARKETING'),
    ('CLX_PR_DISPOSITION', 'BANK_AO_ASSISTANCE', 'ops_action', 'NONE'),
    ('CLX_PR_DISPOSITION', 'NO_POLICY_NUMBER', 'tagging_owner', 'MARKETING'),
    ('CLX_PR_DISPOSITION', 'NO_POLICY_NUMBER', 'ops_action', 'NONE'),
    ('CLX_PR_DISPOSITION', 'NO_POLICY_NUMBER', 'allowed_roles', 'PROCESSOR,MKT_COLLECTION,CLX_TL,MKT_SECTION_HEAD'),
    ('CLX_PR_DISPOSITION', 'DP_RETURNED', 'tagging_owner', 'MARKETING'),
    ('CLX_PR_DISPOSITION', 'DP_RETURNED', 'ops_action', 'NONE'),
    ('CLX_UPP_DISPOSITION', 'FOR_APPLICATION_TO_INVOICE', 'requires_invoice', 'true'),
    ('CLX_UPP_DISPOSITION', 'FOR_APPLICATION_TO_INVOICE', 'cashiering_action', 'APPLY_TO_INVOICE'),
    ('CLX_UPP_DISPOSITION', 'FOR_REFUND', 'requires_invoice', 'false'),
    ('CLX_UPP_DISPOSITION', 'FOR_REFUND', 'cashiering_action', 'REFUND'),
    ('CLX_UPP_DISPOSITION', 'FOR_RECLASS', 'requires_invoice', 'false'),
    ('CLX_UPP_DISPOSITION', 'FOR_RECLASS', 'cashiering_action', 'RECLASS'),
    ('CLX_UPP_DISPOSITION', 'FOR_TRANSFER', 'requires_invoice', 'false'),
    ('CLX_UPP_DISPOSITION', 'FOR_TRANSFER', 'cashiering_action', 'TRANSFER'),
    ('CLX_UPP_DISPOSITION', 'COORDINATE_FURTHER', 'requires_invoice', 'false'),
    ('CLX_UPP_DISPOSITION', 'COORDINATE_FURTHER', 'cashiering_action', 'NONE')
) as v(type_code, code, attribute, value);

-- ---------- Workflow CLX_ESCALATION (BRCLXN.049/050/055, design section 7) ------------------
-- SLA hours are defaults; each escalation rule gives its own (CLX_ESCALATION_OVERDUE).
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('CLX_ESCALATION', 'RAISED', 'Raised', null, null, true, false, 10),
       ('CLX_ESCALATION', 'WITH_TL', 'With the team lead', 'CLX_ESCALATION_HANDLE', 24, false, false, 20),
       ('CLX_ESCALATION', 'WITH_UH', 'With the unit / section head', 'CLX_ESCALATION_HANDLE', 24, false, false, 30),
       ('CLX_ESCALATION', 'IN_ACTION', 'In action', 'CLX_ESCALATION_HANDLE', 72, false, false, 40),
       ('CLX_ESCALATION', 'RETURNED', 'Returned to the handler', 'CLX_WORK', 48, false, false, 50),
       ('CLX_ESCALATION', 'RESOLVED', 'Resolved', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('CLX_ESCALATION', 'RAISED', 'route', 'WITH_TL', 'Route to the team lead', 'CLX_ESCALATE', false, null, 10),
       ('CLX_ESCALATION', 'RAISED', 'route_to_head', 'WITH_UH', 'Route to the unit / section head', 'CLX_ESCALATE', false, null, 20),
       ('CLX_ESCALATION', 'WITH_TL', 'acknowledge', 'IN_ACTION', 'Acknowledge', 'CLX_ESCALATION_HANDLE', true, null, 10),
       ('CLX_ESCALATION', 'WITH_TL', 'escalate_further', 'WITH_UH', 'Escalate to the unit / section head', 'CLX_ESCALATION_HANDLE', false, 'CLX_ESCALATION_REASON', 20),
       ('CLX_ESCALATION', 'WITH_TL', 'return_to_handler', 'RETURNED', 'Return to the handler', 'CLX_ESCALATION_HANDLE', true, 'RETURN_REASON', 80),
       ('CLX_ESCALATION', 'WITH_TL', 'auto_close', 'RESOLVED', 'Closed: account collected', 'CLX_ESCALATION_HANDLE', false, null, 95),
       ('CLX_ESCALATION', 'WITH_UH', 'acknowledge', 'IN_ACTION', 'Acknowledge', 'CLX_ESCALATION_HANDLE', true, null, 10),
       ('CLX_ESCALATION', 'WITH_UH', 'return_to_handler', 'RETURNED', 'Return to the handler', 'CLX_ESCALATION_HANDLE', true, 'RETURN_REASON', 80),
       ('CLX_ESCALATION', 'WITH_UH', 'auto_close', 'RESOLVED', 'Closed: account collected', 'CLX_ESCALATION_HANDLE', false, null, 95),
       ('CLX_ESCALATION', 'IN_ACTION', 'resolve', 'RESOLVED', 'Resolve', 'CLX_ESCALATION_HANDLE', false, null, 10),
       ('CLX_ESCALATION', 'IN_ACTION', 'escalate_further', 'WITH_UH', 'Escalate to the unit / section head', 'CLX_ESCALATION_HANDLE', false, 'CLX_ESCALATION_REASON', 20),
       ('CLX_ESCALATION', 'IN_ACTION', 'auto_close', 'RESOLVED', 'Closed: account collected', 'CLX_ESCALATION_HANDLE', false, null, 95),
       ('CLX_ESCALATION', 'RETURNED', 'resubmit', 'WITH_TL', 'Resubmit to the team lead', 'CLX_WORK,CLX_ESCALATE', false, null, 10),
       ('CLX_ESCALATION', 'RETURNED', 'auto_close', 'RESOLVED', 'Closed: account collected', 'CLX_ESCALATION_HANDLE', false, null, 95);

-- ---------- Business parameters (design section 8) --------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CLX_MIN_BALANCE_THRESHOLD', '10.00', 'DECIMAL', 'COLLECTIONS',
     'Net outstanding PR above which an invoice enters the collection worklist (BRCLXN.005-007; value to confirm, CQ03)', 0, 100000, now(), 'SYSTEM'),
    ('CLX_AGING_BASIS', 'BOOKING', 'STRING', 'COLLECTIONS',
     'Date the aging of a collection item counts from: BOOKING or INCEPTION (BRCLXN.049)', null, null, now(), 'SYSTEM'),
    ('CLX_AGING_BRACKETS', '0-30,31-45,46-60,61-90,91-120,121+', 'CODE_LIST', 'COLLECTIONS',
     'Aging brackets of the worklist, the home chart and the reports, in days (BRCLXN.001-012)', null, null, now(), 'SYSTEM'),
    ('CLX_INVOICE_NO_PATTERN', '^(I\d{8}|BI-.+)$', 'STRING', 'COLLECTIONS',
     'Regular expression of the invoice number of "For application to invoice": EBIX I######## or BrokerVerse BI-... (BRCLXN.047, CQ13)', null, null, now(), 'SYSTEM'),
    ('CLX_PROMISE_GRACE_DAYS', '0', 'INTEGER', 'COLLECTIONS',
     'Days after the promised date before an unpaid promise to pay is broken (BRCLXN.055)', 0, 30, now(), 'SYSTEM'),
    ('CLX_EXPORT_MAX_ROWS', '50000', 'INTEGER', 'COLLECTIONS',
     'Maximum rows of an asynchronous Collections export (caveat p.93)', 100, 1000000, now(), 'SYSTEM'),
    ('CLX_EDIT_LOCK_MINUTES', '15', 'INTEGER', 'COLLECTIONS',
     'Minutes after which the "<user> is editing" lock of a collection account expires (NFR record lock)', 1, 240, now(), 'SYSTEM'),
    ('LOGIN_MAX_FAILED_ATTEMPTS', '3', 'INTEGER', 'SECURITY',
     'Consecutive failed logins that lock a user account (BDOI NFR 1-2; scope to confirm, CQ23)', 1, 10, now(), 'SYSTEM');

-- ---------- Exception codes (design section 8) ------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('CLX_REFRESH_FAILED', 'Collections refresh failed',
     'The daily refresh of the collection worklist from the invoice ledger failed (BRCLXN.001-015).',
     'COLLECTIONS', 'HIGH', null, null, now(), 'SYSTEM'),
    ('CLX_ESCALATION_OVERDUE', 'Collections escalation overdue',
     'An escalation stayed in a stage beyond the SLA of its rule (BRCLXN.049).',
     'COLLECTIONS', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('CLX_FILE_NOT_PUBLISHED', 'Collections file not published',
     'A scheduled Collections file (daily, weekly, monthly or application file) was not generated on time (BRCLXN.024-029, 041, 045).',
     'COLLECTIONS', 'HIGH', null, null, now(), 'SYSTEM'),
    ('CLX_OUTBOX_STALE', 'Collections item not taken',
     'An item Collections sent to Cashiering or Commission (check pick-up, 2307, DP list) is still pending after the threshold (COLLECTIONS_DESIGN 2.2).',
     'COLLECTIONS', 'MEDIUM', null, 1, now(), 'SYSTEM');

-- ---------- Notification events (design section 8) --------------------------------------------
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('CLX_ESCALATED', 'Account escalated', 'COLLECTIONS',
     'A collection account was escalated to you or by your team (BRCLXN.049/050)', true, true, 200),
    ('CLX_REASSIGNED', 'Account reassigned', 'COLLECTIONS',
     'Collection accounts were assigned to you or taken from you (BRCLXN.052)', true, false, 210),
    ('CLX_PROMISE_BROKEN', 'Promise to pay broken', 'COLLECTIONS',
     'A promise to pay of one of your accounts was not kept (BRCLXN.055)', true, false, 220),
    ('CLX_DP_RETURNED', 'Direct payment returned by insurer', 'COLLECTIONS',
     'An insurer returned a direct payment account; it is back in your worklist (CMRID.009)', true, false, 230),
    ('CLX_FILE_READY', 'Collections file ready', 'COLLECTIONS',
     'A scheduled Collections file is available for download (BRCLXN.024-029, 045)', true, false, 240);

-- ---------- Collection feeds become in-app (design sections 2.2 and 9) ------------------------
-- The Collections module serves the inbound feeds from its outbox and receives the outbound ones
-- in its inbox (CollectionFeed adapter IN_APP); the upload handlers stay as a fallback.
update ops_flow_in_feed
set transport = 'IN_APP', updated_at = now(), updated_by = 'SYSTEM'
where code in ('COLLECTION_CWT2307', 'COLLECTION_DP_LIST', 'COLLECTION_CHECK_PICKUP',
               'COLLECTION_DP_RETURNED', 'COLLECTION_REFUND');

-- ---------- Scheduled report files (BRCLXN.024-029, 041, 045) ---------------------------------
-- A file generated by a job is archived as a GENERATE run with the time from which it may be
-- downloaded (ReportArchiveService.archiveGenerated).
alter table report_run drop constraint report_run_action_check;
alter table report_run add constraint ck_report_run_action check (action in ('VIEW', 'EXPORT', 'GENERATE'));
alter table report_run add column available_from timestamptz;
create index ix_report_run_available on report_run (report_code, available_from)
    where action = 'GENERATE';
