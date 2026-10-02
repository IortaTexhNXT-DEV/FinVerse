-- =====================================================================================
-- iNXT BrokerVerse - V755 Product Maintenance (BRD-3) foundation: roles and grants, permission
-- action classes, lists of values, workflow PM_PACKAGE_REQUEST and business parameters.
--   Requirements: docs/requirements/BDOI_PM_BRD_SPEC.md
--     BRPM.002       access restricted to authorised users; Product Maintenance roles (PQ17)
--     PMADD05        role-to-action matrix: every broking permission classified VIEW / CREATE /
--                    AMEND / APPROVE by functional area (sec_permission_action)
--     BRPM.008-017, BRPM.021, PMADD04  package request workflow with multi-level approvals,
--                    negotiation rounds, ManCom sign-off, MBS set-up and validation
--     PMADD01/02/04/07/08, BRPM.011/016  coded fields as lists of values
--   Design: docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md sections 3, 6 and 7. The
--   permissions are the enum security.domain.Permission.
--   Runs before the catalog tables (V810) on a fresh database: no foreign key to catalog tables.
--   Lists the BRD leaves open hold a proposed list or only 'Others' until BDOI supplies them
--   (PQ04, PQ05, PQ12, PQ13).
-- =====================================================================================

-- ---------- Product Maintenance role profiles (design 6.2) ------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('TSU_TL', 'TSU Team Lead', now(), 'SYSTEM'),
       ('TSU_HEAD', 'TSU Team Head / Head', now(), 'SYSTEM'),
       ('MBS', 'Marketing Business Services and System Support', now(), 'SYSTEM'),
       ('MANCOM', 'Management Committee', now(), 'SYSTEM');

-- Every new role: My Work, documents, reports catalogue, client look-up and product read access.
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('WORK_VIEW'), ('ATTACHMENT_VIEW'), ('REPORT_VIEW'), ('CLIENT_VIEW'),
                   ('PRODUCT_VIEW')) as p(permission)
where r.code in ('TSU_TL', 'TSU_HEAD', 'MBS', 'MANCOM')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Role-specific grants (design 6.2; a proposal until PQ17 / OQ48 are answered).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Marketing AO: create and submit package requests; Marketing review of negotiated terms
    ('MKT_AO', 'PKG_REQUEST'), ('MKT_AO', 'PRODUCT_VIEW'),
    -- Marketing TL / TH / UH: approve package requests (never their own)
    ('MKT_TL', 'PKG_REQUEST'), ('MKT_TL', 'PKG_REQUEST_APPROVE'), ('MKT_TL', 'PRODUCT_VIEW'),
    ('MKT_TL', 'PKG_REPORT_VIEW'),
    -- TSU Officer
    ('TSU', 'PKG_REQUEST'), ('TSU', 'PKG_NEGOTIATE'), ('TSU', 'PKG_ADVISORY'), ('TSU', 'PRODUCT_VIEW'),
    -- TSU Team Lead: review and recommend, QS approval (four eyes), negotiation, advisories
    ('TSU_TL', 'PKG_TSU_RECOMMEND'), ('TSU_TL', 'PKG_QS_APPROVE'), ('TSU_TL', 'PKG_NEGOTIATE'),
    ('TSU_TL', 'PKG_ADVISORY'), ('TSU_TL', 'PKG_REPORT_VIEW'), ('TSU_TL', 'ATTACHMENT_MANAGE'),
    ('TSU_TL', 'WORK_ASSIGN'),
    -- TSU Team Head / Head: TSU approval, release of terms, post-set-up validation (PMADD06)
    ('TSU_HEAD', 'PKG_TSU_APPROVE'), ('TSU_HEAD', 'PRODUCT_VALIDATE'), ('TSU_HEAD', 'PKG_REPORT_VIEW'),
    ('TSU_HEAD', 'PRODUCT_ARCHIVE_VIEW'), ('TSU_HEAD', 'WORK_ASSIGN'),
    -- MBS: package set-up (draft versions), catalog product areas, incentive criteria
    ('MBS', 'PRODUCT_MAINTAIN'), ('MBS', 'INCENTIVE_CRITERIA_MAINTAIN'), ('MBS', 'PKG_REPORT_VIEW'),
    ('MBS', 'PRODUCT_ARCHIVE_VIEW'), ('MBS', 'MASTER_VIEW'), ('MBS', 'ATTACHMENT_MANAGE'),
    -- ManCom: sign-off of the product requirements (BRPM.015)
    ('MANCOM', 'PKG_MANCOM_SIGNOFF'),
    -- Business Administrator: validation checkpoint, authorisation of product masters, archive
    ('BUSINESS_ADMIN', 'PRODUCT_VIEW'), ('BUSINESS_ADMIN', 'PRODUCT_VALIDATE'),
    ('BUSINESS_ADMIN', 'PRODUCT_AUTHORIZE'), ('BUSINESS_ADMIN', 'PRODUCT_ARCHIVE_VIEW'),
    ('BUSINESS_ADMIN', 'PKG_REPORT_VIEW'),
    -- Approver: keeps authorising catalog product records as with MASTER_AUTHORIZE today
    ('NB_APPROVER', 'PRODUCT_VIEW'), ('NB_APPROVER', 'PRODUCT_AUTHORIZE'),
    -- Existing platform roles: read access
    ('SYSADMIN', 'PRODUCT_VIEW'),
    ('AUDITOR', 'PRODUCT_VIEW'), ('AUDITOR', 'PRODUCT_ARCHIVE_VIEW'), ('AUDITOR', 'PKG_REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (PMADD05, design 6.3) ------------------------------------
-- Each permission of the broking areas is classified by functional area and action class so the
-- User Access Matrix can show roles x areas x actions. A permission may have two rows (CREATE and
-- AMEND). Permissions without a row (finance modules) appear only in the permission view.
create table sec_permission_action (
    id         bigint generated by default as identity primary key,
    permission varchar(50) not null,
    area       varchar(40) not null,
    action     varchar(10) not null,
    constraint uq_sec_permission_action unique (permission, action),
    constraint ck_sec_permission_action check (action in ('VIEW', 'CREATE', 'AMEND', 'APPROVE'))
);
create index ix_sec_permission_action_area on sec_permission_action (area, action);

insert into sec_permission_action (permission, area, action)
values
    -- Product Maintenance: catalog product areas and versions
    ('PRODUCT_VIEW', 'PRODUCT_MAINTENANCE', 'VIEW'),
    ('PRODUCT_ARCHIVE_VIEW', 'PRODUCT_MAINTENANCE', 'VIEW'),
    ('PRODUCT_MAINTAIN', 'PRODUCT_MAINTENANCE', 'CREATE'),
    ('PRODUCT_MAINTAIN', 'PRODUCT_MAINTENANCE', 'AMEND'),
    ('PRODUCT_AUTHORIZE', 'PRODUCT_MAINTENANCE', 'APPROVE'),
    ('PRODUCT_VALIDATE', 'PRODUCT_MAINTENANCE', 'APPROVE'),
    -- Incentive criteria
    ('INCENTIVE_CRITERIA_MAINTAIN', 'INCENTIVES', 'CREATE'),
    ('INCENTIVE_CRITERIA_MAINTAIN', 'INCENTIVES', 'AMEND'),
    -- Package requests
    ('PKG_REQUEST', 'PACKAGE_REQUEST', 'CREATE'),
    ('PKG_REQUEST', 'PACKAGE_REQUEST', 'AMEND'),
    ('PKG_REQUEST_APPROVE', 'PACKAGE_REQUEST', 'APPROVE'),
    ('PKG_TSU_RECOMMEND', 'PACKAGE_REQUEST', 'APPROVE'),
    ('PKG_TSU_APPROVE', 'PACKAGE_REQUEST', 'APPROVE'),
    ('PKG_NEGOTIATE', 'PACKAGE_REQUEST', 'CREATE'),
    ('PKG_NEGOTIATE', 'PACKAGE_REQUEST', 'AMEND'),
    ('PKG_QS_APPROVE', 'PACKAGE_REQUEST', 'APPROVE'),
    ('PKG_MANCOM_SIGNOFF', 'PACKAGE_REQUEST', 'APPROVE'),
    ('PKG_ADVISORY', 'PACKAGE_REQUEST', 'CREATE'),
    ('PKG_REPORT_VIEW', 'PACKAGE_REQUEST', 'VIEW'),
    -- Catalog and other shared masters (maker-checker)
    ('MASTER_VIEW', 'MASTER_DATA', 'VIEW'),
    ('MASTER_MAINTAIN', 'MASTER_DATA', 'CREATE'),
    ('MASTER_MAINTAIN', 'MASTER_DATA', 'AMEND'),
    ('MASTER_AUTHORIZE', 'MASTER_DATA', 'APPROVE'),
    -- Clients (crm)
    ('CLIENT_VIEW', 'CLIENTS', 'VIEW'),
    ('CLIENT_MAINTAIN', 'CLIENTS', 'CREATE'),
    ('CLIENT_MAINTAIN', 'CLIENTS', 'AMEND'),
    ('CLIENT_APPROVE', 'CLIENTS', 'APPROVE'),
    -- Package quotations
    ('QUOTE_VIEW', 'QUOTATIONS', 'VIEW'),
    ('QUOTE_MAINTAIN', 'QUOTATIONS', 'CREATE'),
    ('QUOTE_MAINTAIN', 'QUOTATIONS', 'AMEND'),
    ('QUOTE_APPROVE', 'QUOTATIONS', 'APPROVE'),
    -- Non-package proposal requests and TSU
    ('PROPOSAL_REQUEST', 'NON_PACKAGE', 'CREATE'),
    ('PROPOSAL_REQUEST', 'NON_PACKAGE', 'AMEND'),
    ('PROPOSAL_APPROVE', 'NON_PACKAGE', 'APPROVE'),
    ('TSU_PROCESS', 'NON_PACKAGE', 'AMEND'),
    ('TSU_APPROVE', 'NON_PACKAGE', 'APPROVE'),
    -- Accounts
    ('ACCOUNT_VIEW', 'ACCOUNTS', 'VIEW'),
    ('ACCOUNT_MAINTAIN', 'ACCOUNTS', 'CREATE'),
    ('ACCOUNT_MAINTAIN', 'ACCOUNTS', 'AMEND'),
    ('ACCOUNT_PROCESS', 'ACCOUNTS', 'APPROVE'),
    -- Placement, billing and e-policies
    ('PLACEMENT_MANAGE', 'PLACEMENT', 'AMEND'),
    ('BILLING_MANAGE', 'PLACEMENT', 'AMEND'),
    ('EPOLICY_MANAGE', 'PLACEMENT', 'AMEND'),
    ('EPOLICY_SEND', 'PLACEMENT', 'CREATE'),
    -- Booking
    ('BOOKING_PROCESS', 'BOOKING', 'CREATE'),
    ('BOOKING_ADJUST', 'BOOKING', 'AMEND'),
    -- Work queues, bulk processing, documents, reports
    ('WORK_VIEW', 'WORK_QUEUES', 'VIEW'),
    ('WORK_ASSIGN', 'WORK_QUEUES', 'AMEND'),
    ('BULK_PROCESS', 'BULK_PROCESSING', 'CREATE'),
    ('ATTACHMENT_VIEW', 'DOCUMENTS', 'VIEW'),
    ('ATTACHMENT_MANAGE', 'DOCUMENTS', 'CREATE'),
    ('REPORT_VIEW', 'REPORTS', 'VIEW'),
    ('DASHBOARD_VIEW', 'REPORTS', 'VIEW'),
    -- Broking administration
    ('LOV_MANAGE', 'BROKING_ADMIN', 'CREATE'),
    ('LOV_MANAGE', 'BROKING_ADMIN', 'AMEND'),
    ('ACCESS_REQUEST', 'BROKING_ADMIN', 'CREATE'),
    ('ACCESS_APPROVE', 'BROKING_ADMIN', 'APPROVE'),
    ('MESSAGE_VIEW', 'BROKING_ADMIN', 'VIEW'),
    -- Operations (BRD-2): home, reports and interfaces
    ('OPS_VIEW', 'OPERATIONS', 'VIEW'),
    ('OPS_REPORT_VIEW', 'OPERATIONS', 'VIEW'),
    ('OPS_REPORT_EXPORT', 'OPERATIONS', 'VIEW'),
    ('FLOWIN_MANAGE', 'OPERATIONS', 'AMEND'),
    ('DISB_PROCESS', 'OPERATIONS', 'AMEND'),
    -- Cashiering
    ('CASH_RECEIPT', 'CASHIERING', 'CREATE'),
    ('CASH_UPLOAD', 'CASHIERING', 'CREATE'),
    ('CASH_CANCEL', 'CASHIERING', 'AMEND'),
    ('CASH_REINSTATE', 'CASHIERING', 'AMEND'),
    ('CASH_APPLY', 'CASHIERING', 'AMEND'),
    ('CASH_DISPOSITION', 'CASHIERING', 'CREATE'),
    ('CASH_PRINT', 'CASHIERING', 'VIEW'),
    ('CASH_SERIES_MANAGE', 'CASHIERING', 'CREATE'),
    ('CASH_SERIES_MANAGE', 'CASHIERING', 'AMEND'),
    ('CASH_APPROVE', 'CASHIERING', 'APPROVE'),
    ('CASH_DISPOSITION_APPROVE', 'CASHIERING', 'APPROVE'),
    ('CWT_TAG', 'CASHIERING', 'AMEND'),
    ('CWT_PROCESS', 'CASHIERING', 'AMEND'),
    -- Remittance and holds
    ('REMIT_EXTRACT', 'REMITTANCE', 'CREATE'),
    ('REMIT_OR_UPLOAD', 'REMITTANCE', 'CREATE'),
    ('REMIT_PROCESS', 'REMITTANCE', 'AMEND'),
    ('REMIT_EXCLUDE', 'REMITTANCE', 'AMEND'),
    ('REMIT_APPROVE', 'REMITTANCE', 'APPROVE'),
    ('HOLD_REQUEST', 'REMITTANCE', 'CREATE'),
    ('HOLD_APPROVE', 'REMITTANCE', 'APPROVE'),
    ('SPECIAL_REMIT_REQUEST', 'REMITTANCE', 'CREATE'),
    ('SPECIAL_REMIT_APPROVE', 'REMITTANCE', 'APPROVE'),
    -- Production reconciliation
    ('RECON_PROCESS', 'PRODUCTION_RECON', 'AMEND'),
    ('RECON_SEND', 'PRODUCTION_RECON', 'CREATE'),
    -- Adjustment
    ('ADJ_REQUEST', 'ADJUSTMENT', 'CREATE'),
    ('ADJ_PROCESS', 'ADJUSTMENT', 'AMEND'),
    ('ADJ_APPROVE', 'ADJUSTMENT', 'APPROVE'),
    ('ADJ_POST', 'ADJUSTMENT', 'APPROVE'),
    -- Commission receivables and incentives
    ('COMMREC_PROCESS', 'COMMISSION', 'CREATE'),
    ('COMMREC_PROCESS', 'COMMISSION', 'AMEND'),
    ('COMMREC_APPROVE', 'COMMISSION', 'APPROVE'),
    ('INCENTIVE_MANAGE', 'COMMISSION', 'AMEND'),
    ('BIR_CERT_SUBMIT', 'COMMISSION', 'CREATE'),
    ('BIR_CERT_ACK', 'COMMISSION', 'APPROVE');

-- ---------- Lists of values (design sections 4 and 8) ----------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('PKG_REQUEST_TYPE', 'Package request type', 'What a package request asks for (BRPM.011, PQ13)', true, now(), 'SYSTEM'),
       ('PKG_REQUEST_REASON', 'Package request reason', 'Why a package is created, changed, renewed or retired (BRPM.008/011)', true, now(), 'SYSTEM'),
       ('PKG_RESPONSE_OUTCOME', 'Insurer response outcome', 'Outcome of an insurer response in a negotiation round, including exception states (PMADD04, PQ05)', true, now(), 'SYSTEM'),
       ('PKG_ADVISORY_GROUP', 'Advisory recipient group', 'Units that receive package advisories (BRPM.016, PQ12)', true, now(), 'SYSTEM'),
       ('PKG_NOT_PROCEEDED_REASON', 'Package not proceeded reason', 'Why a package request is closed without a package (BRPM.010/013)', true, now(), 'SYSTEM'),
       ('INCENTIVE_TYPE', 'Incentive type', 'Type of an incentive criterion on the products matrix (PMADD07/08, PQ04)', true, now(), 'SYSTEM'),
       ('COVERAGE_KIND', 'Coverage kind', 'Level of a coverage in the product hierarchy (PMADD01)', true, now(), 'SYSTEM'),
       ('CLAUSE_KIND', 'Clause kind', 'Kind of a clause library entry (PMADD02)', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    -- Request types (BRPM.011; "deletion" is RETIRE, BRPM.006)
    ('PKG_REQUEST_TYPE', 'NEW', 'New package', 10),
    ('PKG_REQUEST_TYPE', 'AMEND', 'Amend package terms', 20),
    ('PKG_REQUEST_TYPE', 'UPDATE', 'Update package details', 30),
    ('PKG_REQUEST_TYPE', 'RENEW', 'Renew package', 40),
    ('PKG_REQUEST_TYPE', 'RETIRE', 'Retire package', 50),
    ('PKG_REQUEST_TYPE', 'REACTIVATE', 'Reactivate expired package', 60),
    -- Request reasons (proposed list, PQ13)
    ('PKG_REQUEST_REASON', 'CLIENT_REQUIREMENT', 'Client requirement', 10),
    ('PKG_REQUEST_REASON', 'NEW_PROGRAMME', 'New programme for a market segment', 20),
    ('PKG_REQUEST_REASON', 'MARKET_COMPETITIVENESS', 'Market competitiveness', 30),
    ('PKG_REQUEST_REASON', 'INSURER_TERMS_CHANGE', 'Change in insurer terms or rates', 40),
    ('PKG_REQUEST_REASON', 'LOSS_EXPERIENCE', 'Loss experience', 50),
    ('PKG_REQUEST_REASON', 'REGULATORY', 'Regulatory change', 60),
    ('PKG_REQUEST_REASON', 'PACKAGE_EXPIRY', 'Package expiry or anniversary', 70),
    ('PKG_REQUEST_REASON', 'OTHERS', 'Others (see comment)', 90),
    -- Insurer response outcomes (PMADD04; list to confirm, PQ05)
    ('PKG_RESPONSE_OUTCOME', 'PENDING', 'Pending', 10),
    ('PKG_RESPONSE_OUTCOME', 'ACCEPTED_AS_REQUESTED', 'Accepted as requested', 20),
    ('PKG_RESPONSE_OUTCOME', 'APPROVED_WITH_CHANGES', 'Approved with changes', 30),
    ('PKG_RESPONSE_OUTCOME', 'COUNTER_PROPOSAL', 'Counter-proposal', 40),
    ('PKG_RESPONSE_OUTCOME', 'DECLINED', 'Declined', 50),
    ('PKG_RESPONSE_OUTCOME', 'NO_RESPONSE', 'No response', 60),
    -- Advisory recipient groups (BRPM.016 "relevant units"; PQ12)
    ('PKG_ADVISORY_GROUP', 'MARKETING', 'Marketing', 10),
    ('PKG_ADVISORY_GROUP', 'TSU', 'Technical Support Unit', 20),
    ('PKG_ADVISORY_GROUP', 'MBS', 'Marketing Business Services', 30),
    ('PKG_ADVISORY_GROUP', 'PROCESSING', 'Processing', 40),
    ('PKG_ADVISORY_GROUP', 'OPERATIONS', 'Operations', 50),
    -- Not proceeded reasons
    ('PKG_NOT_PROCEEDED_REASON', 'INSURERS_DECLINED', 'Declined by the insurers', 10),
    ('PKG_NOT_PROCEEDED_REASON', 'TERMS_NOT_ACCEPTED', 'Negotiated terms not accepted', 20),
    ('PKG_NOT_PROCEEDED_REASON', 'CLIENT_WITHDREW', 'Client withdrew the request', 30),
    ('PKG_NOT_PROCEEDED_REASON', 'BUSINESS_DECISION', 'Business decision', 40),
    ('PKG_NOT_PROCEEDED_REASON', 'OTHERS', 'Others (see comment)', 90),
    -- Incentive types (definitions to be supplied, PQ04)
    ('INCENTIVE_TYPE', 'OTHERS', 'Others (see description)', 90),
    -- Coverage kinds (PMADD01)
    ('COVERAGE_KIND', 'SECTION', 'Section', 10),
    ('COVERAGE_KIND', 'COVERAGE', 'Coverage', 20),
    ('COVERAGE_KIND', 'PERIL', 'Peril', 30),
    ('COVERAGE_KIND', 'EXTENSION', 'Extension', 40),
    -- Clause kinds (PMADD02)
    ('CLAUSE_KIND', 'WARRANTY', 'Warranty', 10),
    ('CLAUSE_KIND', 'CLAUSE', 'Clause', 20),
    ('CLAUSE_KIND', 'EXCLUSION', 'Exclusion', 30),
    ('CLAUSE_KIND', 'DEDUCTIBLE_WORDING', 'Deductible wording', 40)
) as v(type_code, code, label, sort_order);

-- ---------- Workflow PM_PACKAGE_REQUEST (design section 7) -----------------------------------
-- SLA hours are the defaults of the PKG_SLA_* parameters below (placeholders until PQ08).
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('PM_PACKAGE_REQUEST', 'DRAFT', 'Draft request', 'PKG_REQUEST', null, true, false, 10),
       ('PM_PACKAGE_REQUEST', 'FOR_MKT_APPROVAL', 'For Marketing approval', 'PKG_REQUEST_APPROVE', 24, false, false, 20),
       ('PM_PACKAGE_REQUEST', 'FOR_TSU_REVIEW', 'For TSU review', 'PKG_TSU_RECOMMEND', 24, false, false, 30),
       ('PM_PACKAGE_REQUEST', 'FOR_TSU_APPROVAL', 'For TSU Head approval', 'PKG_TSU_APPROVE', 24, false, false, 40),
       ('PM_PACKAGE_REQUEST', 'NEGOTIATION', 'Insurer negotiation', 'PKG_NEGOTIATE', 120, false, false, 50),
       ('PM_PACKAGE_REQUEST', 'TERMS_REVIEW', 'Negotiated terms review', 'PKG_TSU_APPROVE', 24, false, false, 60),
       ('PM_PACKAGE_REQUEST', 'FOR_MKT_REVIEW', 'Marketing review of terms', 'PKG_REQUEST', 48, false, false, 70),
       ('PM_PACKAGE_REQUEST', 'REQUIREMENTS_PREP', 'Requirements preparation', 'PKG_NEGOTIATE', 48, false, false, 80),
       ('PM_PACKAGE_REQUEST', 'FOR_MANCOM', 'For ManCom sign-off', 'PKG_MANCOM_SIGNOFF', 72, false, false, 90),
       ('PM_PACKAGE_REQUEST', 'WITH_MBS', 'With MBS for set-up', 'PRODUCT_MAINTAIN', 48, false, false, 100),
       ('PM_PACKAGE_REQUEST', 'FOR_VALIDATION', 'Package version for validation', 'PRODUCT_VALIDATE', 24, false, false, 110),
       ('PM_PACKAGE_REQUEST', 'RELEASED', 'Released', null, null, false, true, 120),
       ('PM_PACKAGE_REQUEST', 'RETIRED', 'Package retired', null, null, false, true, 125),
       ('PM_PACKAGE_REQUEST', 'NOT_PROCEEDED', 'Not proceeded', null, null, false, true, 130),
       ('PM_PACKAGE_REQUEST', 'VOIDED', 'Voided', null, null, false, true, 140);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values
    -- Request and approvals (BRPM.008/009/021)
    ('PM_PACKAGE_REQUEST', 'DRAFT', 'submit', 'FOR_MKT_APPROVAL', 'Submit for approval', 'PKG_REQUEST', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'DRAFT', 'void', 'VOIDED', 'Void', 'PKG_REQUEST', true, 'VOID_REASON', 90),
    ('PM_PACKAGE_REQUEST', 'FOR_MKT_APPROVAL', 'approve', 'FOR_TSU_REVIEW', 'Approve and send to TSU', 'PKG_REQUEST_APPROVE', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'FOR_MKT_APPROVAL', 'return', 'DRAFT', 'Return to requester', 'PKG_REQUEST_APPROVE', true, 'RETURN_REASON', 80),
    ('PM_PACKAGE_REQUEST', 'FOR_MKT_APPROVAL', 'void', 'VOIDED', 'Void', 'PKG_REQUEST,PKG_REQUEST_APPROVE', true, 'VOID_REASON', 90),
    ('PM_PACKAGE_REQUEST', 'FOR_TSU_REVIEW', 'recommend', 'FOR_TSU_APPROVAL', 'Recommend for approval', 'PKG_TSU_RECOMMEND', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'FOR_TSU_REVIEW', 'return', 'DRAFT', 'Return to requester', 'PKG_TSU_RECOMMEND', true, 'RETURN_REASON', 80),
    ('PM_PACKAGE_REQUEST', 'FOR_TSU_REVIEW', 'void', 'VOIDED', 'Void', 'PKG_TSU_RECOMMEND', true, 'VOID_REASON', 90),
    ('PM_PACKAGE_REQUEST', 'FOR_TSU_APPROVAL', 'approve', 'NEGOTIATION', 'Approve for negotiation', 'PKG_TSU_APPROVE', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'FOR_TSU_APPROVAL', 'approve_no_negotiation', 'FOR_MANCOM', 'Approve without negotiation', 'PKG_TSU_APPROVE', false, null, 20),
    ('PM_PACKAGE_REQUEST', 'FOR_TSU_APPROVAL', 'return', 'DRAFT', 'Return to requester', 'PKG_TSU_APPROVE', true, 'RETURN_REASON', 80),
    ('PM_PACKAGE_REQUEST', 'FOR_TSU_APPROVAL', 'void', 'VOIDED', 'Void', 'PKG_TSU_APPROVE', true, 'VOID_REASON', 90),
    -- Negotiation rounds and terms (BRPM.010/012/013, PMADD04)
    ('PM_PACKAGE_REQUEST', 'NEGOTIATION', 'terms_final', 'TERMS_REVIEW', 'Terms final', 'PKG_NEGOTIATE', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'NEGOTIATION', 'revise_qs', 'NEGOTIATION', 'Revise quotation slip (new round)', 'PKG_NEGOTIATE', false, null, 20),
    ('PM_PACKAGE_REQUEST', 'NEGOTIATION', 'not_proceeded', 'NOT_PROCEEDED', 'Not proceeded', 'PKG_NEGOTIATE,PKG_TSU_APPROVE', true, 'PKG_NOT_PROCEEDED_REASON', 90),
    ('PM_PACKAGE_REQUEST', 'TERMS_REVIEW', 'release_to_marketing', 'FOR_MKT_REVIEW', 'Release terms to Marketing', 'PKG_TSU_APPROVE', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'TERMS_REVIEW', 'skip_marketing_review', 'REQUIREMENTS_PREP', 'Proceed to requirements (generic package)', 'PKG_TSU_APPROVE', false, null, 20),
    ('PM_PACKAGE_REQUEST', 'FOR_MKT_REVIEW', 'accept_terms', 'REQUIREMENTS_PREP', 'Accept terms', 'PKG_REQUEST', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'FOR_MKT_REVIEW', 'request_changes', 'NEGOTIATION', 'Request changes', 'PKG_REQUEST', true, 'RETURN_REASON', 20),
    ('PM_PACKAGE_REQUEST', 'FOR_MKT_REVIEW', 'not_proceeded', 'NOT_PROCEEDED', 'Not proceeded', 'PKG_REQUEST', true, 'PKG_NOT_PROCEEDED_REASON', 90),
    -- Requirements, ManCom sign-off and MBS set-up (BRPM.015)
    ('PM_PACKAGE_REQUEST', 'REQUIREMENTS_PREP', 'submit_requirements', 'FOR_MANCOM', 'Submit requirements for ManCom sign-off', 'PKG_NEGOTIATE', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'FOR_MANCOM', 'signoff', 'WITH_MBS', 'Sign off and send to MBS', 'PKG_MANCOM_SIGNOFF', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'FOR_MANCOM', 'return', 'REQUIREMENTS_PREP', 'Return to TSU', 'PKG_MANCOM_SIGNOFF', true, 'RETURN_REASON', 80),
    ('PM_PACKAGE_REQUEST', 'WITH_MBS', 'setup', 'FOR_VALIDATION', 'Set up package version', 'PRODUCT_MAINTAIN', false, null, 10),
    -- A RETIRE request ends at the MBS step: the product is retired, no version is released
    -- (BRPM.011 "deletion", BRPM.006; PackageSetupService.retireProduct).
    ('PM_PACKAGE_REQUEST', 'WITH_MBS', 'retire', 'RETIRED', 'Retire package', 'PRODUCT_MAINTAIN', false, null, 20),
    ('PM_PACKAGE_REQUEST', 'WITH_MBS', 'return_incomplete', 'REQUIREMENTS_PREP', 'Return incomplete requirements to TSU', 'PRODUCT_MAINTAIN', false, 'RETURN_REASON', 80),
    -- Validation checkpoint on the catalog version (PMADD06): run as system actions by the
    -- listener of the catalog events ProductVersionReleased / ProductVersionReturned.
    ('PM_PACKAGE_REQUEST', 'FOR_VALIDATION', 'version_released', 'RELEASED', 'Package version released', 'PRODUCT_VALIDATE', false, null, 10),
    ('PM_PACKAGE_REQUEST', 'FOR_VALIDATION', 'version_returned', 'WITH_MBS', 'Package version returned to MBS', 'PRODUCT_VALIDATE', false, null, 80);

-- ---------- Business parameters (design section 8) --------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('PKG_REQUEST_PREFIX', 'PKR-', 'STRING', 'PRODUCT_MAINTENANCE',
     'Prefix of package request numbers; the year is appended (PKR-<yyyy>-n, BRPM.008)', null, null, now(), 'SYSTEM'),
    ('PKG_QS_PREFIX', 'PQS-', 'STRING', 'PRODUCT_MAINTENANCE',
     'Prefix of package quotation slip numbers; the year is appended (PQS-<yyyy>-n, BRPM.012)', null, null, now(), 'SYSTEM'),
    ('PKG_QS_REPLY_DAYS', '5', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'Days insurers have to reply to a package quotation slip (reply due date, BRPM.012)', 1, 60, now(), 'SYSTEM'),
    ('PACKAGE_EXPIRY_NOTICE_DAYS', '60', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'Days before the package end date at which PACKAGE_EXPIRING is first raised; again at 30 and 7 days (BRPM.017, PQ11)', 1, 365, now(), 'SYSTEM'),
    ('PACKAGE_RENEWAL_AUTODRAFT', 'false', 'BOOLEAN', 'PRODUCT_MAINTENANCE',
     'The expiry monitor drafts a RENEW package request for packages reaching the notice period (BRPM.017, PQ11)', null, null, now(), 'SYSTEM'),
    ('PKG_SLA_MKT_APPROVAL', '24', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'SLA hours of the Marketing approval of a package request (BRPM.008/021, PQ08)', 1, 720, now(), 'SYSTEM'),
    ('PKG_SLA_TSU_REVIEW', '24', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'SLA hours of the TSU Team Lead review and recommendation (BRPM.009, PQ08)', 1, 720, now(), 'SYSTEM'),
    ('PKG_SLA_TSU_APPROVAL', '24', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'SLA hours of the TSU Team Head approval (BRPM.009, PQ08)', 1, 720, now(), 'SYSTEM'),
    ('PKG_SLA_NEGOTIATION', '120', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'SLA hours of the insurer negotiation stage (BRPM.010, PQ08)', 1, 2160, now(), 'SYSTEM'),
    ('PKG_SLA_MANCOM', '72', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'SLA hours of the ManCom sign-off (BRPM.015, PQ07/PQ08)', 1, 720, now(), 'SYSTEM'),
    ('PKG_SLA_MBS_SETUP', '48', 'INTEGER', 'PRODUCT_MAINTENANCE',
     'SLA hours of the MBS package set-up (BRPM.015, PQ08)', 1, 720, now(), 'SYSTEM');
