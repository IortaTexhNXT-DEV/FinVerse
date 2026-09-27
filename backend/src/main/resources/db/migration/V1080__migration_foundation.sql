-- =====================================================================================
-- iNXT BrokerVerse - V1080 Data Migration (BRD-13) foundation: roles and grants of the MIG_*
-- and LEGACY_* permissions, permission action classes, lists of values, business parameters,
-- the workflows MIG_OBJECT_DECISION, MIG_MAP_VERSION, MIG_BATCH_ROLLBACK, MIG_OPENING_TRUEUP and
-- MIG_RESUBMISSION, exception codes and the accounting event types of the opening entries.
--   Requirements: docs/requirements/BDOI_DM_BRD_SPEC.md (BRID 1.1a-12.1)
--   Design: docs/architecture/DATA_MIGRATION_DESIGN.md sections 13, 18, 19 and 20. The permissions
--   are the enum security.domain.Permission; the role proposal waits for BDOI (DMQ16).
--   CASH_UPP_INCOME_APPROVE is granted here to top management (role created here); the other
--   CASH_UPP_INCOME_* and LEGACY_REVERSAL_* grants are in the owners' migrations (V768
--   cashiering, V787 commission).
--   Runs after V750 (LOV), V751 (workflow), V755 (sec_permission_action), V760 (Operations roles)
--   and V762 (notification events); references platform tables only.
-- =====================================================================================

-- ---------- Roles (design 18.2) ---------------------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('DATA_MIGRATION_LEAD', 'Data Migration Lead', now(), 'SYSTEM'),
       ('DATA_STEWARD', 'Data Steward (migration)', now(), 'SYSTEM'),
       ('DATA_OWNER', 'Data Owner (migration object)', now(), 'SYSTEM'),
       ('MIGRATION_OPERATOR', 'Migration Operator', now(), 'SYSTEM'),
       ('MIGRATION_RECON_APPROVER', 'Migration Reconciliation Approver', now(), 'SYSTEM'),
       ('MIGRATION_GONOGO', 'Go / No-Go Board', now(), 'SYSTEM'),
       ('LEGACY_INQUIRY', 'Legacy Inquiry (Audit / Compliance)', now(), 'SYSTEM'),
       ('LEGACY_ACCESS_REVIEWER', 'Legacy Access Log Reviewer', now(), 'SYSTEM'),
       ('TOP_MANAGEMENT_APPROVER', 'Top Management Approver', now(), 'SYSTEM')
on conflict (code) do nothing;

-- Migration roles: console, reports, documents and approvals (design 18.2; FRS permissions matrix).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    ('DATA_MIGRATION_LEAD', 'MIG_VIEW'), ('DATA_MIGRATION_LEAD', 'MIG_OBJECT_MANAGE'),
    ('DATA_MIGRATION_LEAD', 'MIG_LOAD_APPROVE'), ('DATA_MIGRATION_LEAD', 'MIG_SIGNOFF'),
    ('DATA_MIGRATION_LEAD', 'MIG_CUTOVER_MANAGE'), ('DATA_MIGRATION_LEAD', 'MIG_ROLLBACK_REQUEST'),
    ('DATA_MIGRATION_LEAD', 'REPORT_VIEW'), ('DATA_MIGRATION_LEAD', 'ATTACHMENT_VIEW'),
    ('DATA_STEWARD', 'MIG_VIEW'), ('DATA_STEWARD', 'MIG_MAPPING_EDIT'),
    ('DATA_STEWARD', 'MIG_DQ_RESOLVE'), ('DATA_STEWARD', 'MIG_MATCH_DECIDE'),
    ('DATA_STEWARD', 'REPORT_VIEW'),
    ('DATA_OWNER', 'MIG_VIEW'), ('DATA_OWNER', 'MIG_DECISION_APPROVE'),
    ('DATA_OWNER', 'MIG_MAPPING_APPROVE'), ('DATA_OWNER', 'MIG_DQ_WAIVE'),
    ('DATA_OWNER', 'MIG_SIGNOFF'), ('DATA_OWNER', 'REPORT_VIEW'), ('DATA_OWNER', 'ATTACHMENT_VIEW'),
    ('MIGRATION_OPERATOR', 'MIG_VIEW'), ('MIGRATION_OPERATOR', 'MIG_INTAKE'),
    ('MIGRATION_OPERATOR', 'MIG_LOAD_RUN'), ('MIGRATION_OPERATOR', 'REPORT_VIEW'),
    ('MIGRATION_RECON_APPROVER', 'MIG_VIEW'), ('MIGRATION_RECON_APPROVER', 'MIG_RECON_SIGNOFF'),
    ('MIGRATION_RECON_APPROVER', 'MIG_ROLLBACK_APPROVE'),
    ('MIGRATION_RECON_APPROVER', 'MIG_TRUEUP_APPROVE'), ('MIGRATION_RECON_APPROVER', 'REPORT_VIEW'),
    ('MIGRATION_RECON_APPROVER', 'ATTACHMENT_VIEW'),
    ('MIGRATION_GONOGO', 'MIG_VIEW'), ('MIGRATION_GONOGO', 'MIG_GONOGO_DECIDE'),
    ('MIGRATION_GONOGO', 'REPORT_VIEW'),
    ('LEGACY_INQUIRY', 'LEGACY_INQUIRY_VIEW'), ('LEGACY_INQUIRY', 'LEGACY_INQUIRY_EXPORT'),
    ('LEGACY_INQUIRY', 'ATTACHMENT_VIEW'),
    ('LEGACY_ACCESS_REVIEWER', 'LEGACY_ACCESS_LOG_VIEW'), ('LEGACY_ACCESS_REVIEWER', 'REPORT_VIEW'),
    -- Comptrollership GL lead prepares the FY2027 true-ups (design 17.7)
    ('COMPTROLLERSHIP', 'MIG_VIEW'), ('COMPTROLLERSHIP', 'MIG_TRUEUP_PREPARE'),
    -- Top management approves the reclassification of old unapplied payments to income (BRID 5.5)
    ('TOP_MANAGEMENT_APPROVER', 'CASH_UPP_INCOME_APPROVE'),
    -- Platform administrator and auditor: read access to the console
    ('SYSADMIN', 'MIG_VIEW'), ('AUDITOR', 'MIG_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix) --------------------------------------
insert into sec_permission_action (permission, area, action)
values
    ('MIG_VIEW', 'DATA_MIGRATION', 'VIEW'),
    ('MIG_OBJECT_MANAGE', 'DATA_MIGRATION', 'CREATE'),
    ('MIG_OBJECT_MANAGE', 'DATA_MIGRATION', 'AMEND'),
    ('MIG_DECISION_APPROVE', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_MAPPING_EDIT', 'DATA_MIGRATION', 'AMEND'),
    ('MIG_MAPPING_APPROVE', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_INTAKE', 'DATA_MIGRATION', 'CREATE'),
    ('MIG_DQ_RESOLVE', 'DATA_MIGRATION', 'AMEND'),
    ('MIG_DQ_WAIVE', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_MATCH_DECIDE', 'DATA_MIGRATION', 'AMEND'),
    ('MIG_LOAD_RUN', 'DATA_MIGRATION', 'CREATE'),
    ('MIG_LOAD_APPROVE', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_ROLLBACK_REQUEST', 'DATA_MIGRATION', 'CREATE'),
    ('MIG_ROLLBACK_APPROVE', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_RECON_SIGNOFF', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_SIGNOFF', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_CUTOVER_MANAGE', 'DATA_MIGRATION', 'AMEND'),
    ('MIG_GONOGO_DECIDE', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_TRUEUP_PREPARE', 'DATA_MIGRATION', 'CREATE'),
    ('MIG_TRUEUP_APPROVE', 'DATA_MIGRATION', 'APPROVE'),
    ('MIG_RESUBMIT_APPROVE', 'DATA_MIGRATION', 'APPROVE'),
    ('LEGACY_INQUIRY_VIEW', 'DATA_MIGRATION', 'VIEW'),
    ('LEGACY_INQUIRY_EXPORT', 'DATA_MIGRATION', 'VIEW'),
    ('LEGACY_ACCESS_LOG_VIEW', 'DATA_MIGRATION', 'VIEW');

-- ---------- Lists of values (design 20; owner MIG_MAPPING_EDIT) ---------------------------------
insert into lov_type (code, name, description, maintainable, owner_permission, created_at, created_by)
values ('MIG_SOURCE_SYSTEM', 'Legacy source system', 'Legacy system an extract comes from', true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM'),
       ('MIG_OBJECT_CATEGORY', 'Migration object category', 'Category of a data object of the migration register', true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM'),
       ('MIG_TRUST_LEVEL', 'Data trust level', 'How far the business trusts the legacy data of an object', true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM'),
       ('MIG_BREAK_REASON', 'Reconciliation break reason', 'Why a reconciliation difference is explained', true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM'),
       ('MIG_WAIVER_REASON', 'Row waiver reason', 'Why a data owner waives or excludes a failing row', true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM'),
       ('MIG_ACCESS_REASON', 'Legacy access reason', 'Why a user searches the legacy archive', true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM'),
       ('LEGACY_RECORD_TYPE', 'Legacy record type', 'Kind of a legacy archive record', true, 'MIG_MAPPING_EDIT', now(), 'SYSTEM')
on conflict (code) do nothing;

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('MIG_SOURCE_SYSTEM', 'EBIX', 'EBIX', 10),
    ('MIG_SOURCE_SYSTEM', 'QPS', 'QPS', 20),
    ('MIG_SOURCE_SYSTEM', 'ISYS', 'ISYS', 30),
    ('MIG_SOURCE_SYSTEM', 'CMS', 'Collection Management System', 40),
    ('MIG_SOURCE_SYSTEM', 'EXCEL', 'Excel trackers', 50),
    ('MIG_OBJECT_CATEGORY', 'REFERENCE', 'Reference data', 10),
    ('MIG_OBJECT_CATEGORY', 'CLIENT', 'Client', 20),
    ('MIG_OBJECT_CATEGORY', 'POLICY', 'Policy', 30),
    ('MIG_OBJECT_CATEGORY', 'OPEN_ITEM', 'Open item', 40),
    ('MIG_OBJECT_CATEGORY', 'GL', 'General ledger', 50),
    ('MIG_OBJECT_CATEGORY', 'HISTORY', 'History', 60),
    ('MIG_TRUST_LEVEL', 'HIGH', 'High', 10),
    ('MIG_TRUST_LEVEL', 'MEDIUM', 'Medium', 20),
    ('MIG_TRUST_LEVEL', 'LOW', 'Low', 30),
    ('MIG_BREAK_REASON', 'TIMING', 'Timing difference between extract and ledger', 10),
    ('MIG_BREAK_REASON', 'ROUNDING', 'Rounding in the legacy extract', 20),
    ('MIG_BREAK_REASON', 'EXCLUDED_ROWS', 'Rows excluded by the data owner', 30),
    ('MIG_BREAK_REASON', 'SOURCE_ERROR', 'Error in the legacy source, corrected by manual entry', 40),
    ('MIG_BREAK_REASON', 'MAPPING', 'Code map difference', 50),
    ('MIG_BREAK_REASON', 'OTHER', 'Other (see explanation)', 90),
    ('MIG_WAIVER_REASON', 'NOT_NEEDED', 'Record not needed in BIBS', 10),
    ('MIG_WAIVER_REASON', 'MANUAL_ENTRY', 'Entered manually in BIBS after go-live', 20),
    ('MIG_WAIVER_REASON', 'DUPLICATE', 'Duplicate of another legacy record', 30),
    ('MIG_WAIVER_REASON', 'SOURCE_FIX_LATER', 'Corrected in the next extract', 40),
    ('MIG_WAIVER_REASON', 'OTHER', 'Other (see remarks)', 90),
    ('MIG_ACCESS_REASON', 'AUDIT', 'Internal or external audit', 10),
    ('MIG_ACCESS_REASON', 'CLIENT_INQUIRY', 'Client inquiry', 20),
    ('MIG_ACCESS_REASON', 'CLAIM', 'Claim support', 30),
    ('MIG_ACCESS_REASON', 'REGULATOR', 'Regulator request', 40),
    ('MIG_ACCESS_REASON', 'LEGAL', 'Legal case', 50),
    ('MIG_ACCESS_REASON', 'OTHER', 'Other (see remarks)', 90),
    ('LEGACY_RECORD_TYPE', 'CLIENT', 'Client', 10),
    ('LEGACY_RECORD_TYPE', 'POLICY', 'Policy', 20),
    ('LEGACY_RECORD_TYPE', 'INVOICE', 'Invoice', 30),
    ('LEGACY_RECORD_TYPE', 'RECEIPT', 'Receipt', 40),
    ('LEGACY_RECORD_TYPE', 'REMITTANCE', 'Remittance', 50),
    ('LEGACY_RECORD_TYPE', 'ENDORSEMENT', 'Endorsement', 60),
    ('LEGACY_RECORD_TYPE', 'CLAIM', 'Claim', 70),
    ('LEGACY_RECORD_TYPE', 'GL_JOURNAL', 'GL journal', 80),
    ('LEGACY_RECORD_TYPE', 'RENEWAL_ADVICE', 'Renewal advice', 90),
    ('LEGACY_RECORD_TYPE', 'LETTER', 'Letter', 100),
    ('LEGACY_RECORD_TYPE', 'OTHER', 'Other', 110),
    ('DOCUMENT_TYPE', 'LEGACY_DOCUMENT', 'Legacy document (archive)', 130),
    ('DOCUMENT_TYPE', 'MIG_EVIDENCE', 'Migration sign-off evidence', 131)
) as v(type_code, code, label, sort_order)
where not exists (select 1 from lov_value x where x.type_code = v.type_code and x.code = v.code);

-- ---------- Business parameters (design 20; category DATA_MIGRATION) ---------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('MIG_ENVIRONMENT_CLASS', 'NON_PRODUCTION', 'STRING', 'DATA_MIGRATION',
     'PRODUCTION or NON_PRODUCTION; outside production personal data of the extracts is masked before staging', null, null, now(), 'SYSTEM'),
    ('MIG_CUTOVER_DATE', '2028-01-03', 'STRING', 'DATA_MIGRATION',
     'Go-live date (yyyy-MM-dd); the first business day on BIBS', null, null, now(), 'SYSTEM'),
    ('MIG_STAGING_RETENTION_DAYS', '5', 'INTEGER', 'DATA_MIGRATION',
     'Days staging data and extract files are kept after the batch is signed off or rolled back', 1, 5, now(), 'SYSTEM'),
    ('MIG_CHUNK_SIZE', '500', 'INTEGER', 'DATA_MIGRATION',
     'Rows loaded per transaction', 10, 5000, now(), 'SYSTEM'),
    ('MIG_PARTITIONS', '4', 'INTEGER', 'DATA_MIGRATION',
     'Partitions loaded in parallel', 1, 16, now(), 'SYSTEM'),
    ('MIG_AMOUNT_TOLERANCE', '0.00', 'DECIMAL', 'DATA_MIGRATION',
     'Largest amount difference accepted by the amount reconciliation', null, null, now(), 'SYSTEM'),
    ('MIG_MAX_ERROR_RATE_MASTER', '0.5', 'DECIMAL', 'DATA_MIGRATION',
     'Largest percentage of failing or waived rows with which master data may be loaded', null, null, now(), 'SYSTEM'),
    ('MIG_MAX_ERROR_RATE_FINANCIAL', '0', 'DECIMAL', 'DATA_MIGRATION',
     'Largest percentage of failing or waived rows with which open items, unapplied payments and the trial balance may be loaded', null, null, now(), 'SYSTEM'),
    ('MIG_CLIENT_MATCH_AUTO', '90', 'INTEGER', 'DATA_MIGRATION',
     'Client match score from which two legacy clients are merged automatically', 60, 100, now(), 'SYSTEM'),
    ('MIG_CLIENT_MATCH_REVIEW', '60', 'INTEGER', 'DATA_MIGRATION',
     'Client match score from which a pair goes to the Data Steward review queue', 1, 100, now(), 'SYSTEM'),
    ('MIG_INVOICE_NO_COLLISION_PREFIX', 'true', 'BOOLEAN', 'DATA_MIGRATION',
     'Prefix the source system to a legacy invoice number found in two source systems', null, null, now(), 'SYSTEM'),
    ('MIG_ARCHIVE_EXPORT_MAX_ROWS', '1000', 'INTEGER', 'DATA_MIGRATION',
     'Largest number of archive records exported to Excel at a time', 1, 100000, now(), 'SYSTEM'),
    ('MIG_ACCESS_EXPORT_ALERT_ROWS', '5000', 'INTEGER', 'DATA_MIGRATION',
     'Archive records exported by one user in a day above which the unusual-access alert is raised', 1, 1000000, now(), 'SYSTEM'),
    ('MIG_LEGACY_ACCESS_REASON_REQUIRED', 'true', 'BOOLEAN', 'DATA_MIGRATION',
     'A reason is required before the legacy archive is searched', null, null, now(), 'SYSTEM'),
    ('MIG_LEGACY_LINK_EBIX', '', 'STRING', 'DATA_MIGRATION',
     'Address of the read-only EBIX application for users who still need it', null, null, now(), 'SYSTEM'),
    ('MIG_LEGACY_LINK_QPS', '', 'STRING', 'DATA_MIGRATION',
     'Address of the read-only QPS application for users who still need it', null, null, now(), 'SYSTEM'),
    ('MIG_UPP_ISSUE_AR', 'false', 'BOOLEAN', 'DATA_MIGRATION',
     'Issue a BIBS acknowledgement receipt for each migrated unapplied payment (no cash posting)', null, null, now(), 'SYSTEM'),
    ('MIG_LEGACY_INVOICE_NO_PATTERN', '^I\d{8}$', 'STRING', 'DATA_MIGRATION',
     'Patterns of legacy invoice numbers recognised by the payment matcher (comma separated regular expressions)', null, null, now(), 'SYSTEM'),
    ('MIG_OPENING_VALUE_DATE', '2028-01-01', 'STRING', 'DATA_MIGRATION',
     'Value date of the opening entries and of the opening-balance adjustments (yyyy-MM-dd)', null, null, now(), 'SYSTEM'),
    ('MIG_GOLIVE_RENEWAL_TO', '2028-05-31', 'STRING', 'DATA_MIGRATION',
     'Last expiry date of the migrated policies taken over by Renewal at go-live (yyyy-MM-dd)', null, null, now(), 'SYSTEM'),
    ('MIG_RENEWAL_URGENT_TO', '2028-01-31', 'STRING', 'DATA_MIGRATION',
     'Migrated policies expiring up to this date are flagged urgent in the go-live renewal queue (yyyy-MM-dd)', null, null, now(), 'SYSTEM'),
    ('MIG_RESUBMIT_DEADLINE', '2028-01-02 12:00', 'STRING', 'DATA_MIGRATION',
     'Last time a corrected resubmission of the advices already sent may be approved (yyyy-MM-dd HH:mm, Philippine time)', null, null, now(), 'SYSTEM'),
    ('MIG_LAST_LEGACY_BUSINESS_DAY', '2027-12-29', 'STRING', 'DATA_MIGRATION',
     'Last business day processed in the legacy systems (yyyy-MM-dd)', null, null, now(), 'SYSTEM'),
    ('MIG_FREEZE_AT', '2027-12-31 22:00', 'STRING', 'DATA_MIGRATION',
     'Start of the legacy business freeze (yyyy-MM-dd HH:mm, Philippine time)', null, null, now(), 'SYSTEM'),
    ('MIG_YEAR_END_OPTION', 'A', 'STRING', 'DATA_MIGRATION',
     'Year-end cut-over option: A = provisional opening trial balance at the year-end boundary with controlled true-ups', null, null, now(), 'SYSTEM'),
    ('MIG_RA_MAX_LEAD_DAYS', '140', 'INTEGER', 'DATA_MIGRATION',
     'An advice already sent more than this number of days before expiry is loaded with a warning', 1, 365, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- ---------- Exception codes (design 20) ----------------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('MIG_EXTRACT_REJECTED', 'Migration extract rejected',
     'An extract did not agree with its control file and nothing was staged; ask for a corrected extract.',
     'DATA_MIGRATION', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('MIG_LOAD_FAILED', 'Migration load failed',
     'A migration batch stopped or loaded with rejected rows; review the batch run log.',
     'DATA_MIGRATION', 'HIGH', null, null, now(), 'SYSTEM'),
    ('MIG_RECON_BREAK', 'Migration reconciliation break',
     'A reconciliation line does not match; explain or correct it before sign-off.',
     'DATA_MIGRATION', 'HIGH', null, null, now(), 'SYSTEM'),
    ('MIG_UNMAPPED', 'Unmapped legacy codes',
     'Validation found legacy codes without an approved code map entry; add them to a new map version.',
     'DATA_MIGRATION', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('MIG_CLEARING_NOT_ZERO', 'Migration clearing not zero',
     'The migration clearing account is not zero for a branch and currency; the trial balance and the open-item detail disagree.',
     'DATA_MIGRATION', 'CRITICAL', 0.00, null, now(), 'SYSTEM'),
    ('MIG_STAGING_PURGE_OVERDUE', 'Migration staging not purged',
     'A signed-off batch still holds staging data after the retention days.',
     'DATA_MIGRATION', 'HIGH', null, 5, now(), 'SYSTEM'),
    ('MIG_LEGACY_ACCESS_UNUSUAL', 'Unusual legacy archive access',
     'A user exported more legacy archive records in a day than the alert limit.',
     'DATA_MIGRATION', 'HIGH', null, null, now(), 'SYSTEM'),
    ('MIG_TRUEUP_BREAK', 'Opening-balance adjustment break',
     'A check of the opening-balance adjustment reconciliation failed; it cannot be signed until the break is resolved.',
     'DATA_MIGRATION', 'CRITICAL', null, null, now(), 'SYSTEM')
on conflict (code) do nothing;

-- ---------- Accounting event types (design 19; rules come from Comptrollership) -----------------
insert into acc_event_type (code, name, category, journal_type, description, amount_components) values
 ('MIG_LEGACY_INVOICE_OPENING', 'Legacy invoice opening', 'PREMIUM', 'OPENING',
  'Opening position of an open legacy invoice at cut-over: premium receivable by component, receivable of the client''s '
  || 'withholding tax certificate, due to insurer, commission and VAT receivable, unrealised commission and deferred VAT, '
  || 'balanced against the migration clearing account. A negative event of the same reference with :RB reverses it.',
  'LG_PR_BASIC,LG_PR_DST,LG_PR_PTX_VAT,LG_PR_LGT,LG_PR_FST,LG_PR_OTHER,LG_PR2307,LG_DTIP,LG_COMMISSION,LG_COMMISSION_VAT,LG_UNREALIZED,LG_DEFERRED_VAT,CLEARING'),
 ('MIG_UPP_OPENING', 'Legacy unapplied payment opening', 'RECEIPT', 'OPENING',
  'Opening balance of a legacy unapplied payment: migration clearing against unapplied collections (legacy).',
  'LG_AMOUNT,CLEARING'),
 ('MIG_LEGACY_POSITION_TRUEUP', 'Legacy open-item adjustment', 'PREMIUM', 'OPENING',
  'Year-end adjustment of an open legacy item after go-live: the legacy control account of the component against the '
  || 'migration clearing account.',
  'LG_PR_BASIC,LG_PR_DST,LG_PR_PTX_VAT,LG_PR_LGT,LG_PR_FST,LG_PR_OTHER,LG_PR2307,LG_DTIP,LG_COMMISSION,LG_COMMISSION_VAT,LG_UNREALIZED,LG_DEFERRED_VAT,LG_AMOUNT,CLEARING')
on conflict (code) do nothing;

-- ---------- Workflow MIG_OBJECT_DECISION (gate G1, design 13) -----------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('MIG_OBJECT_DECISION', 'FOR_DECISION', 'For decision', 'MIG_DECISION_APPROVE', 48, true, false, 10),
       ('MIG_OBJECT_DECISION', 'DECIDED', 'Decided', null, null, false, true, 20),
       ('MIG_OBJECT_DECISION', 'RETURNED', 'Returned', null, null, false, true, 30);
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('MIG_OBJECT_DECISION', 'FOR_DECISION', 'approve', 'DECIDED', 'Approve', 'MIG_DECISION_APPROVE', false, null, 10),
       ('MIG_OBJECT_DECISION', 'FOR_DECISION', 'return', 'RETURNED', 'Return', 'MIG_DECISION_APPROVE', false, null, 20);

-- ---------- Workflow MIG_MAP_VERSION (gate G2, design 7) ----------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('MIG_MAP_VERSION', 'DRAFT', 'Draft', 'MIG_MAPPING_EDIT', null, true, false, 10),
       ('MIG_MAP_VERSION', 'SUBMITTED', 'Submitted', 'MIG_MAPPING_APPROVE', 48, false, false, 20),
       ('MIG_MAP_VERSION', 'APPROVED', 'Approved', null, null, false, false, 30),
       ('MIG_MAP_VERSION', 'SUPERSEDED', 'Superseded', null, null, false, true, 40);
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('MIG_MAP_VERSION', 'DRAFT', 'submit', 'SUBMITTED', 'Submit', 'MIG_MAPPING_EDIT', false, null, 10),
       ('MIG_MAP_VERSION', 'SUBMITTED', 'approve', 'APPROVED', 'Approve', 'MIG_MAPPING_APPROVE', false, null, 10),
       ('MIG_MAP_VERSION', 'SUBMITTED', 'return', 'DRAFT', 'Return', 'MIG_MAPPING_APPROVE', false, null, 20),
       ('MIG_MAP_VERSION', 'APPROVED', 'supersede', 'SUPERSEDED', 'Supersede', 'MIG_MAPPING_APPROVE', false, null, 10);

-- ---------- Workflow MIG_BATCH_ROLLBACK (design 11) ---------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('MIG_BATCH_ROLLBACK', 'REQUESTED', 'Rollback requested', 'MIG_ROLLBACK_APPROVE', 24, true, false, 10),
       ('MIG_BATCH_ROLLBACK', 'ROLLED_BACK', 'Rolled back', null, null, false, true, 20),
       ('MIG_BATCH_ROLLBACK', 'REJECTED', 'Rejected', null, null, false, true, 30);
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('MIG_BATCH_ROLLBACK', 'REQUESTED', 'approve', 'ROLLED_BACK', 'Approve Rollback', 'MIG_ROLLBACK_APPROVE', false, null, 10),
       ('MIG_BATCH_ROLLBACK', 'REQUESTED', 'reject', 'REJECTED', 'Reject', 'MIG_ROLLBACK_APPROVE', false, null, 20);

-- ---------- Workflow MIG_OPENING_TRUEUP (design 17.7) -------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('MIG_OPENING_TRUEUP', 'PREPARED', 'Prepared', 'MIG_TRUEUP_PREPARE', null, true, false, 10),
       ('MIG_OPENING_TRUEUP', 'FOR_APPROVAL', 'For approval', 'MIG_TRUEUP_APPROVE', 48, false, false, 20),
       ('MIG_OPENING_TRUEUP', 'APPROVED', 'Approved', 'MIG_TRUEUP_PREPARE', null, false, false, 30),
       ('MIG_OPENING_TRUEUP', 'POSTED', 'Posted', 'MIG_TRUEUP_APPROVE', null, false, false, 40),
       ('MIG_OPENING_TRUEUP', 'SIGNED', 'Signed', null, null, false, true, 50);
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('MIG_OPENING_TRUEUP', 'PREPARED', 'submit', 'FOR_APPROVAL', 'Submit for Approval', 'MIG_TRUEUP_PREPARE', false, null, 10),
       ('MIG_OPENING_TRUEUP', 'FOR_APPROVAL', 'approve', 'APPROVED', 'Approve', 'MIG_TRUEUP_APPROVE', false, null, 10),
       ('MIG_OPENING_TRUEUP', 'FOR_APPROVAL', 'return', 'PREPARED', 'Return', 'MIG_TRUEUP_APPROVE', false, null, 20),
       ('MIG_OPENING_TRUEUP', 'APPROVED', 'post', 'POSTED', 'Post', 'MIG_TRUEUP_PREPARE,MIG_TRUEUP_APPROVE', false, null, 10),
       ('MIG_OPENING_TRUEUP', 'POSTED', 'sign', 'SIGNED', 'Sign', 'MIG_TRUEUP_APPROVE', false, null, 10);

-- ---------- Workflow MIG_RESUBMISSION (design 15.1) ---------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('MIG_RESUBMISSION', 'PREPARED', 'Prepared', 'MIG_RESUBMIT_APPROVE', 24, true, false, 10),
       ('MIG_RESUBMISSION', 'APPROVED', 'Approved', null, null, false, true, 20),
       ('MIG_RESUBMISSION', 'RETURNED', 'Returned', null, null, false, true, 30);
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('MIG_RESUBMISSION', 'PREPARED', 'approve', 'APPROVED', 'Approve Resubmission', 'MIG_RESUBMIT_APPROVE', false, null, 10),
       ('MIG_RESUBMISSION', 'PREPARED', 'return', 'RETURNED', 'Return', 'MIG_RESUBMIT_APPROVE', false, null, 20);
