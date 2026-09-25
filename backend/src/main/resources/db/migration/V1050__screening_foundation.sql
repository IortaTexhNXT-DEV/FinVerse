-- =====================================================================================
-- iNXT BrokerVerse - V1050 Sanction Screening and Risk Profiling (BRD-10) foundation: roles and
-- grants, permission action classes, lists of values, business parameters, exception codes,
-- notification events, workflow SCR_CASE and the retention rules of screening records.
--   Requirements: docs/requirements/BDOI_SANC_BRD_SPEC.md
--     SNSRP-101-109  versioned configuration, maker-checker (SCR_CONFIG_MAINTAIN / _APPROVE)
--     SNSRP-107      dispositions as a controlled list (SCR_DISPOSITION, parent = case stage; SQ06)
--     SNSRP-201-204  list ingestion, errors, maintenance and approval (SCR_LIST_*)
--     SNSRP-303      no-active-policy notice (SCR_NO_POLICY_HIT)
--     SNSRP-401-405  case stages, queues, re-assignment reasons, SLA reminders and escalation
--     SNSRP-601      document form types and screening document types
--     SNSRP-701-706  unit head, compliance review, AML committee (SCR_COMMITTEE_RULE, SQ15), STR
--     SNSRP-901-903  compliance reports and the screening audit log (SCR_REPORT_VIEW / _AUDIT_VIEW)
--     p.25           retention 5 years online and 5 years archive (Q39)
--   Design: docs/architecture/SANCTION_SCREENING_DESIGN.md sections 3 (Flyway), 6 (security),
--   7 (workflow SCR_CASE) and 8 (parameters, LOVs, alerts, notification events). The permissions
--   are the enum security.domain.Permission; the grants are a proposal until SQ19 is answered (the
--   Operations Lead role gets its grants when BDOI names it). The screening tables come with S1-A /
--   S1-B / S1-C (V1051-V1055); demo users and data with V1950-V1952.
--   Runs after V801 (crm LOVs, KYC_RISK_RATING / CLIENT_TAG), V790 (retention rules) and V755
--   (sec_permission_action) on a fresh database; nothing here depends on V1000+.
-- =====================================================================================

-- ---------- Screening role profiles (design 6.2) ---------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('COMPLIANCE_OFFICER', 'Compliance Officer', now(), 'SYSTEM'),
       ('COMPLIANCE_CHECKER', 'Compliance Officer (Checker)', now(), 'SYSTEM'),
       ('UNIT_COMPLIANCE_COORD', 'Unit Compliance Coordinator', now(), 'SYSTEM'),
       ('SCR_INVESTIGATOR', 'Screening Investigator', now(), 'SYSTEM'),
       ('SCR_APPROVER', 'Screening Approver / Unit Head', now(), 'SYSTEM'),
       ('AML_COMMITTEE', 'AML Committee Member', now(), 'SYSTEM');

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Compliance Officer: maker of configuration and list changes, BU escalation, STR (SNSRP-703-706)
    ('COMPLIANCE_OFFICER', 'SCR_VIEW'), ('COMPLIANCE_OFFICER', 'SCR_CONFIG_MAINTAIN'),
    ('COMPLIANCE_OFFICER', 'SCR_LIST_MAINTAIN'), ('COMPLIANCE_OFFICER', 'SCR_COMPLIANCE_REVIEW'),
    ('COMPLIANCE_OFFICER', 'SCR_STR_EXTRACT'), ('COMPLIANCE_OFFICER', 'SCR_CASE_ASSIGN'),
    ('COMPLIANCE_OFFICER', 'SCR_REPORT_VIEW'), ('COMPLIANCE_OFFICER', 'SCR_AUDIT_VIEW'),
    ('COMPLIANCE_OFFICER', 'CLIENT_VIEW'), ('COMPLIANCE_OFFICER', 'ATTACHMENT_VIEW'),
    ('COMPLIANCE_OFFICER', 'REPORT_VIEW'), ('COMPLIANCE_OFFICER', 'WORK_VIEW'),
    -- Compliance Checker: approves configuration versions and list changes (SNSRP-109, 204)
    ('COMPLIANCE_CHECKER', 'SCR_VIEW'), ('COMPLIANCE_CHECKER', 'SCR_CONFIG_APPROVE'),
    ('COMPLIANCE_CHECKER', 'SCR_LIST_APPROVE'), ('COMPLIANCE_CHECKER', 'SCR_REPORT_VIEW'),
    ('COMPLIANCE_CHECKER', 'CLIENT_VIEW'), ('COMPLIANCE_CHECKER', 'REPORT_VIEW'),
    -- Unit Compliance Coordinator: queues and re-assignment (SNSRP-402-404)
    ('UNIT_COMPLIANCE_COORD', 'SCR_VIEW'), ('UNIT_COMPLIANCE_COORD', 'SCR_CASE_ASSIGN'),
    ('UNIT_COMPLIANCE_COORD', 'SCR_REPORT_VIEW'), ('UNIT_COMPLIANCE_COORD', 'CLIENT_VIEW'),
    ('UNIT_COMPLIANCE_COORD', 'WORK_VIEW'), ('UNIT_COMPLIANCE_COORD', 'REPORT_VIEW'),
    -- Investigator: reviews, documents, dispositions, manual risk tag (SNSRP-304, 501, 502, 601)
    ('SCR_INVESTIGATOR', 'SCR_VIEW'), ('SCR_INVESTIGATOR', 'SCR_INVESTIGATE'),
    ('SCR_INVESTIGATOR', 'SCR_RISK_TAG'), ('SCR_INVESTIGATOR', 'CLIENT_VIEW'),
    ('SCR_INVESTIGATOR', 'ACCOUNT_VIEW'), ('SCR_INVESTIGATOR', 'ATTACHMENT_VIEW'),
    ('SCR_INVESTIGATOR', 'ATTACHMENT_MANAGE'), ('SCR_INVESTIGATOR', 'WORK_VIEW'),
    -- Approver / Unit Head (SNSRP-702)
    ('SCR_APPROVER', 'SCR_VIEW'), ('SCR_APPROVER', 'SCR_CASE_APPROVE'),
    ('SCR_APPROVER', 'SCR_CASE_ASSIGN'), ('SCR_APPROVER', 'CLIENT_VIEW'), ('SCR_APPROVER', 'WORK_VIEW'),
    -- AML Committee member (SNSRP-704)
    ('AML_COMMITTEE', 'SCR_VIEW'), ('AML_COMMITTEE', 'SCR_COMMITTEE'), ('AML_COMMITTEE', 'CLIENT_VIEW'),
    ('AML_COMMITTEE', 'ATTACHMENT_VIEW'),
    -- Auditor: read, reports and the screening audit log (SNSRP-903)
    ('AUDITOR', 'SCR_VIEW'), ('AUDITOR', 'SCR_REPORT_VIEW'), ('AUDITOR', 'SCR_AUDIT_VIEW'),
    -- System administrator: read access (jobs are run from the job monitor)
    ('SYSADMIN', 'SCR_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix, PMADD05) ---------------------------
insert into sec_permission_action (permission, area, action)
values
    ('SCR_VIEW', 'SCREENING', 'VIEW'),
    ('SCR_REPORT_VIEW', 'SCREENING', 'VIEW'),
    ('SCR_AUDIT_VIEW', 'SCREENING', 'VIEW'),
    ('SCR_CONFIG_MAINTAIN', 'SCREENING', 'CREATE'),
    ('SCR_CONFIG_MAINTAIN', 'SCREENING', 'AMEND'),
    ('SCR_LIST_MAINTAIN', 'SCREENING', 'CREATE'),
    ('SCR_LIST_MAINTAIN', 'SCREENING', 'AMEND'),
    ('SCR_STR_EXTRACT', 'SCREENING', 'CREATE'),
    ('SCR_INVESTIGATE', 'SCREENING', 'AMEND'),
    ('SCR_RISK_TAG', 'SCREENING', 'AMEND'),
    ('SCR_CASE_ASSIGN', 'SCREENING', 'AMEND'),
    ('SCR_CONFIG_APPROVE', 'SCREENING', 'APPROVE'),
    ('SCR_LIST_APPROVE', 'SCREENING', 'APPROVE'),
    ('SCR_CASE_APPROVE', 'SCREENING', 'APPROVE'),
    ('SCR_COMPLIANCE_REVIEW', 'SCREENING', 'APPROVE'),
    ('SCR_COMMITTEE', 'SCREENING', 'APPROVE');

-- ---------- Lists of values (design section 8; seeds "to confirm", SQ06) ---------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('SCR_DISPOSITION', 'Screening disposition', 'Disposition of a screening case; parent = case stage (SNSRP-107, SQ06)', true, now(), 'SYSTEM'),
       ('SCR_CASE_TYPE', 'Screening case type', 'Kind of a screening case (SNSRP-102, 303)', true, now(), 'SYSTEM'),
       ('SCR_LIST_TYPE', 'Watchlist type', 'Kind of a watchlist source (SNSRP-201, SQ01, SQ20)', true, now(), 'SYSTEM'),
       ('SCR_REASSIGN_REASON', 'Case re-assignment reason', 'Why a screening case is re-assigned (SNSRP-404)', true, now(), 'SYSTEM'),
       ('SCR_STR_REASON', 'STR reason', 'Reason code of a suspicious transaction report; values from the AMLC layout (SNSRP-705, SQ09)', true, now(), 'SYSTEM'),
       ('SCR_FORM_TYPE', 'Screening form type', 'Form of a screening review or document (SNSRP-501, 601)', true, now(), 'SYSTEM'),
       ('SCR_DOCUMENT_TYPE', 'Screening document type', 'Document type of a screening case; parent = DOCUMENT_TYPE code (SNSRP-601)', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, v.parent_code, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    -- Dispositions per stage (SNSRP-107; the final list comes from BDOI, SQ06)
    ('SCR_DISPOSITION', 'FALSE_POSITIVE', 'False positive (to confirm)', 10, 'INVESTIGATION'),
    ('SCR_DISPOSITION', 'TRUE_MATCH_REVIEW', 'True match - for review (to confirm)', 20, 'INVESTIGATION'),
    ('SCR_DISPOSITION', 'POSSIBLE_MATCH_EDD', 'Possible match - enhanced due diligence (to confirm)', 30, 'INVESTIGATION'),
    ('SCR_DISPOSITION', 'NEED_MORE_INFO', 'Need more information (to confirm)', 40, 'INVESTIGATION'),
    ('SCR_DISPOSITION', 'CONCUR', 'Concur (to confirm)', 110, 'UNIT_HEAD_APPROVAL'),
    ('SCR_DISPOSITION', 'NOT_CONCUR', 'Do not concur (to confirm)', 120, 'UNIT_HEAD_APPROVAL'),
    ('SCR_DISPOSITION', 'CLOSE_NO_ACTION', 'Close - no further action (to confirm)', 210, 'COMPLIANCE_REVIEW'),
    ('SCR_DISPOSITION', 'ESCALATE_COMMITTEE', 'Escalate to the AML Committee (to confirm)', 220, 'COMPLIANCE_REVIEW'),
    ('SCR_DISPOSITION', 'FOR_STR', 'For STR filing (to confirm)', 230, 'COMPLIANCE_REVIEW'),
    ('SCR_DISPOSITION', 'RETURN', 'Return for rework (to confirm)', 240, 'COMPLIANCE_REVIEW'),
    ('SCR_DISPOSITION', 'APPROVE_STR', 'Approve STR filing (to confirm)', 310, 'AML_COMMITTEE'),
    ('SCR_DISPOSITION', 'NO_STR', 'No STR (to confirm)', 320, 'AML_COMMITTEE'),
    ('SCR_DISPOSITION', 'COMMITTEE_RETURN', 'Return to Compliance (to confirm)', 330, 'AML_COMMITTEE'),
    -- Case types (design 4.4)
    ('SCR_CASE_TYPE', 'NAME_MATCH', 'Name match', 10, null),
    ('SCR_CASE_TYPE', 'PEP', 'Politically exposed person', 20, null),
    ('SCR_CASE_TYPE', 'HIGH_RISK', 'High-risk client', 30, null),
    ('SCR_CASE_TYPE', 'EDD', 'Enhanced due diligence', 40, null),
    ('SCR_CASE_TYPE', 'MONITOR', 'Monitoring', 50, null),
    ('SCR_CASE_TYPE', 'ACCOUNT_APPLICATION', 'Account application', 60, null),
    -- List types (design 4.2)
    ('SCR_LIST_TYPE', 'SANCTION', 'Sanctions list', 10, null),
    ('SCR_LIST_TYPE', 'PEP', 'Politically exposed persons', 20, null),
    ('SCR_LIST_TYPE', 'INTERNAL', 'Internal watchlist', 30, null),
    ('SCR_LIST_TYPE', 'ADVERSE_MEDIA', 'Adverse media (manual)', 40, null),
    -- Re-assignment reasons (SNSRP-404)
    ('SCR_REASSIGN_REASON', 'WORKLOAD', 'Workload balancing', 10, null),
    ('SCR_REASSIGN_REASON', 'ABSENCE', 'Absence of the assignee', 20, null),
    ('SCR_REASSIGN_REASON', 'CONFLICT_OF_INTEREST', 'Conflict of interest', 30, null),
    ('SCR_REASSIGN_REASON', 'OTHERS', 'Others (see remarks)', 90, null),
    -- Form types (SNSRP-501, 601)
    ('SCR_FORM_TYPE', 'KYC_REVIEW', 'KYC review', 10, null),
    ('SCR_FORM_TYPE', 'TRANSACTION_REVIEW', 'Transaction review', 20, null),
    ('SCR_FORM_TYPE', 'EDD', 'Enhanced due diligence', 30, null),
    ('SCR_FORM_TYPE', 'STR', 'Suspicious transaction report', 40, null),
    -- Screening document types, each mapped to its attachment document type (SNSRP-601)
    ('SCR_DOCUMENT_TYPE', 'KYC_FORM', 'Client information sheet / KYC form', 10, 'KYC_FORM'),
    ('SCR_DOCUMENT_TYPE', 'VALID_ID', 'Valid ID', 20, 'VALID_ID'),
    ('SCR_DOCUMENT_TYPE', 'PROOF_OF_ADDRESS', 'Proof of address', 30, 'PROOF_OF_ADDRESS'),
    ('SCR_DOCUMENT_TYPE', 'SOURCE_OF_FUNDS', 'Proof of source of funds (to confirm)', 40, 'OTHERS'),
    ('SCR_DOCUMENT_TYPE', 'SCREENING_EVIDENCE', 'Screening evidence / list extract (to confirm)', 50, 'OTHERS'),
    ('SCR_DOCUMENT_TYPE', 'OTHERS', 'Others', 90, 'OTHERS')
) as v(type_code, code, label, sort_order, parent_code);

-- ---------- Business parameters (design section 8) --------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('SCR_SCREENING_SCOPE', 'PROSPECT,CONFIRMED', 'CODE_LIST', 'SCREENING',
     'Client statuses screened by the periodic run (SNSRP-301-303; to confirm, SQ11)', null, null, now(), 'SYSTEM'),
    ('SCR_FULL_RESCREEN_DAY', '1', 'INTEGER', 'SCREENING',
     'Day of the month of the full rescreen of the in-scope clients; other days screen the list changes only (SNSRP-301)', 1, 28, now(), 'SYSTEM'),
    ('SCR_BLOCK_ON_OPEN_MATCH', 'false', 'BOOLEAN', 'SCREENING',
     'When true, account submission and placement are refused while the client has an open true or potential match (optional gate, SQ07; off: screening informs, it does not block)', null, null, now(), 'SYSTEM'),
    ('SCR_COMMITTEE_RULE', 'MAJORITY', 'STRING', 'SCREENING',
     'How the AML Committee decides: ANY, MAJORITY or ALL of SCR_COMMITTEE_SIZE members (SNSRP-704; to confirm, SQ15)', null, null, now(), 'SYSTEM'),
    ('SCR_COMMITTEE_SIZE', '5', 'INTEGER', 'SCREENING',
     'Members of the AML Committee counted by the MAJORITY / ALL rule (SNSRP-704)', 1, 20, now(), 'SYSTEM'),
    ('SCR_INGEST_ALERT_RECIPIENTS', '', 'STRING', 'SCREENING',
     'Comma separated e-mail addresses that receive the digest of failed watchlist records (SNSRP-202; to confirm, SQ01)', null, null, now(), 'SYSTEM'),
    ('SCR_CASE_SEQUENCE_PREFIX', 'SCR', 'STRING', 'SCREENING',
     'Prefix of the screening case number <prefix>-yyyy-nnnnnn (SNSRP-401)', null, null, now(), 'SYSTEM');

-- ---------- Exception codes (design section 8) ------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('SCR_INGEST_FAILED', 'Watchlist ingestion failed',
     'A watchlist ingestion run failed or loaded only part of the file (SNSRP-201, 202).',
     'SCREENING', 'HIGH', null, null, now(), 'SYSTEM'),
    ('SCR_SLA_BREACH', 'Screening case SLA breached',
     'A screening case stayed in a stage beyond the SLA of its matrix (SNSRP-108, 405).',
     'SCREENING', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('SCR_NO_ACTIVE_CONFIG', 'No active screening configuration',
     'A screening trigger fired while no matching configuration version was active (SNSRP-101, 109).',
     'SCREENING', 'HIGH', null, null, now(), 'SYSTEM'),
    ('SCR_OPEN_TRUE_MATCH_AGEING', 'True match open too long',
     'A true or potential match has had no closed case for more than the threshold days (SNSRP-302, 405).',
     'SCREENING', 'MEDIUM', null, 30, now(), 'SYSTEM');

-- ---------- Notification events (design section 8) --------------------------------------------
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('SCR_CASE_ASSIGNED', 'Screening case assigned', 'SCREENING',
     'A screening case was assigned or re-assigned to you (SNSRP-106, 404)', true, true, 500),
    ('SCR_CASE_FOR_APPROVAL', 'Screening case for approval', 'SCREENING',
     'A screening case waits for your approval (SNSRP-702, 703)', true, true, 510),
    ('SCR_CASE_RETURNED', 'Screening case returned', 'SCREENING',
     'A screening case was returned to you with a rationale (SNSRP-702, 703)', true, true, 520),
    ('SCR_COMMITTEE_REVIEW', 'AML Committee review', 'SCREENING',
     'A screening case waits for the vote of the AML Committee (SNSRP-704)', true, true, 530),
    ('SCR_SLA_REMINDER', 'Screening SLA reminder', 'SCREENING',
     'A screening case of yours is about to pass its SLA (SNSRP-405)', true, true, 540),
    ('SCR_SLA_ESCALATION', 'Screening SLA escalation', 'SCREENING',
     'A screening case passed its SLA and is escalated to you (SNSRP-108, 405)', true, true, 550),
    ('SCR_DOCUMENT_REMINDER', 'Screening document reminder', 'SCREENING',
     'Documents of a screening case are still missing (SNSRP-802)', true, false, 560),
    ('SCR_NO_POLICY_HIT', 'Screening hit without active policy', 'SCREENING',
     'A high-risk or PEP client without an active policy was found (SNSRP-303)', true, false, 570),
    ('SCR_CONFIG_TO_APPROVE', 'Screening configuration to approve', 'SCREENING',
     'A screening configuration version waits for your approval (SNSRP-109)', true, false, 580),
    ('SCR_LIST_CHANGE_TO_APPROVE', 'Watchlist change to approve', 'SCREENING',
     'A watchlist change waits for your approval (SNSRP-204)', true, false, 590);

-- ---------- Workflow SCR_CASE (SNSRP-401-405, 701-706; design section 7) ----------------------
-- SLA hours are defaults; the dated SLA matrix (scr_sla_rule) overrides them per case through
-- WorkflowService.overrideDue (SNSRP-108).
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('SCR_CASE', 'NEW', 'New', null, null, true, false, 10),
       ('SCR_CASE', 'INVESTIGATION', 'Investigation', 'SCR_INVESTIGATE', 72, false, false, 20),
       ('SCR_CASE', 'RETURNED', 'Returned to the investigator', 'SCR_INVESTIGATE', 24, false, false, 30),
       ('SCR_CASE', 'UNIT_HEAD_APPROVAL', 'Unit head approval', 'SCR_CASE_APPROVE', 24, false, false, 40),
       ('SCR_CASE', 'COMPLIANCE_REVIEW', 'Compliance review', 'SCR_COMPLIANCE_REVIEW', 24, false, false, 50),
       ('SCR_CASE', 'AML_COMMITTEE', 'AML Committee', 'SCR_COMMITTEE', 72, false, false, 60),
       ('SCR_CASE', 'STR_PREPARATION', 'STR preparation', 'SCR_COMPLIANCE_REVIEW', 48, false, false, 70),
       ('SCR_CASE', 'STR_EXTRACTION', 'STR extraction and filing', 'SCR_STR_EXTRACT', 120, false, false, 80),
       ('SCR_CASE', 'CLOSED', 'Closed', null, null, false, true, 90);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('SCR_CASE', 'NEW', 'route', 'INVESTIGATION', 'Route to investigation', 'SCR_CASE_ASSIGN,SCR_COMPLIANCE_REVIEW', false, null, 10),
       ('SCR_CASE', 'INVESTIGATION', 'submit', 'UNIT_HEAD_APPROVAL', 'Submit for approval', 'SCR_INVESTIGATE', false, null, 10),
       ('SCR_CASE', 'INVESTIGATION', 'close_no_approval', 'CLOSED', 'Close: false positive', 'SCR_INVESTIGATE', false, null, 20),
       ('SCR_CASE', 'INVESTIGATION', 'request_info', 'INVESTIGATION', 'Request more information', 'SCR_INVESTIGATE', false, null, 30),
       ('SCR_CASE', 'RETURNED', 'resubmit', 'UNIT_HEAD_APPROVAL', 'Resubmit to the unit head', 'SCR_INVESTIGATE', false, null, 10),
       ('SCR_CASE', 'RETURNED', 'resubmit_to_compliance', 'COMPLIANCE_REVIEW', 'Resubmit to Compliance', 'SCR_INVESTIGATE', false, null, 20),
       ('SCR_CASE', 'UNIT_HEAD_APPROVAL', 'approve', 'COMPLIANCE_REVIEW', 'Approve and escalate to Compliance', 'SCR_CASE_APPROVE', false, null, 10),
       ('SCR_CASE', 'UNIT_HEAD_APPROVAL', 'approve_close', 'CLOSED', 'Approve and close', 'SCR_CASE_APPROVE', false, null, 20),
       ('SCR_CASE', 'UNIT_HEAD_APPROVAL', 'disapprove', 'RETURNED', 'Disapprove and return', 'SCR_CASE_APPROVE', false, 'RETURN_REASON', 80),
       ('SCR_CASE', 'COMPLIANCE_REVIEW', 'escalate_committee', 'AML_COMMITTEE', 'Escalate to the AML Committee', 'SCR_COMPLIANCE_REVIEW', false, null, 10),
       ('SCR_CASE', 'COMPLIANCE_REVIEW', 'prepare_str', 'STR_PREPARATION', 'Prepare STR', 'SCR_COMPLIANCE_REVIEW', false, null, 20),
       ('SCR_CASE', 'COMPLIANCE_REVIEW', 'close', 'CLOSED', 'Close the case', 'SCR_COMPLIANCE_REVIEW', false, null, 30),
       ('SCR_CASE', 'COMPLIANCE_REVIEW', 'return_for_rework', 'RETURNED', 'Return for rework', 'SCR_COMPLIANCE_REVIEW', false, 'RETURN_REASON', 80),
       ('SCR_CASE', 'AML_COMMITTEE', 'finalise_str', 'STR_PREPARATION', 'Committee: file an STR', 'SCR_COMMITTEE', false, null, 10),
       ('SCR_CASE', 'AML_COMMITTEE', 'finalise_no_str', 'CLOSED', 'Committee: no STR', 'SCR_COMMITTEE', false, null, 20),
       ('SCR_CASE', 'AML_COMMITTEE', 'finalise_return', 'COMPLIANCE_REVIEW', 'Committee: return to Compliance', 'SCR_COMMITTEE', false, null, 30),
       ('SCR_CASE', 'STR_PREPARATION', 'str_ready', 'STR_EXTRACTION', 'STR ready for extraction', 'SCR_COMPLIANCE_REVIEW', false, null, 10),
       ('SCR_CASE', 'STR_EXTRACTION', 'filed', 'CLOSED', 'Record AMLC filing', 'SCR_STR_EXTRACT', false, null, 10),
       ('SCR_CASE', 'CLOSED', 'reopen', 'INVESTIGATION', 'Reopen the case', 'SCR_COMPLIANCE_REVIEW', false, 'RETURN_REASON', 10);

-- ---------- Retention (BRD-10 p.25: 5 years online, 5 years archive; Q39) -------------------
-- Screening implements RetentionCandidateProvider for both record types in wave S1-C; until then
-- the review run records "no provider" for these rules. Archive / purge stays parked.
insert into nba_retention_rule (record_type, statuses, years_online, years_archive, action, active,
                                description, created_at, created_by)
values ('SCREENING_CASE', 'CLOSED', 5, 5, 'REVIEW', true,
        'Closed screening cases with their reviews, documents and STRs (BRD-10 p.25)', now(), 'SYSTEM'),
       ('WATCHLIST_ENTRY', 'INACTIVE', 5, 5, 'REVIEW', true,
        'Inactive (delisted) watchlist entries (BRD-10 p.25)', now(), 'SYSTEM');
