-- =====================================================================================
-- iNXT BrokerVerse - V1101 Document storage reference data (build step ST0).
--   Decision: docs/architecture/DOCUMENT_STORAGE_DECISION.md sections 3 and 5.
--     Record classes      bucket class, retention (mapped to nba_retention_rule), legal hold and
--                         "archive to ECM" per class (decisions 3 and 5)
--     Roles / grants      legal hold officer and approver (DOA, question DSQ03: BDOI names the
--                         roles; administrators re-grant FILE_LEGAL_HOLD_* when it answers);
--                         information security officer (quarantine alerts, decision 2)
--     Parameter           FILE_LINK_TTL_SECONDS (presigned link validity, default 300)
--     Exception code      FILE_QUARANTINED; notification events FILE_QUARANTINED
--   Runs after V790 (retention rules), V755 (sec_permission_action), V23 (exception codes) and
--   V1100.
-- =====================================================================================

-- ---------- Record classes --------------------------------------------------------------------
-- retention_period is the fallback when the mapped record type has no active retention rule:
-- 5 years online + 15 in archive by default, 10 + 5 for claims, 5 + 5 for screening (AMLA).
insert into sto_record_class (code, name, bucket_class, retention_record_type, retention_period,
                              legal_hold, archive_to_ecm, description, created_at, created_by)
values
    ('GENERAL_DOCUMENT', 'General document', 'DOCUMENTS', null, 'P20Y', false, false,
     'Attachments and supporting documents of any record', now(), 'SYSTEM'),
    ('WORKING_FILE', 'Working file or draft', 'DOCUMENTS', null, 'P5Y', false, false,
     'Drafts and working files; never sent to ECM', now(), 'SYSTEM'),
    ('POLICY_DOCUMENT', 'Issued policy document', 'DOCUMENTS', 'ACCOUNT', 'P20Y', false, true,
     'Signed or issued policy documents, endorsements and slips', now(), 'SYSTEM'),
    ('OFFICIAL_RECEIPT', 'Official receipt', 'DOCUMENTS', null, 'P20Y', true, true,
     'Official and acknowledgement receipts (BIR audit hold)', now(), 'SYSTEM'),
    ('BIR_FORM', 'Filed BIR form', 'DOCUMENTS', null, 'P20Y', true, true,
     'BIR forms and returns as filed (BIR audit hold)', now(), 'SYSTEM'),
    ('STATEMENT_OF_ACCOUNT', 'Statement of account', 'DOCUMENTS', null, 'P20Y', false, true,
     'Statements of account as issued', now(), 'SYSTEM'),
    ('CLAIM_SETTLEMENT', 'Claim settlement letter', 'DOCUMENTS', 'BROKER_CLAIM', 'P15Y', true, true,
     'Claim settlement letters and releases', now(), 'SYSTEM'),
    ('STR', 'Suspicious transaction report', 'DOCUMENTS', 'SCREENING_CASE', 'P10Y', true, true,
     'STRs as filed with the AMLC (AMLA hold)', now(), 'SYSTEM'),
    ('REPORT_OUTPUT', 'Report output', 'REPORTS', null, 'P400D', false, false,
     'Report runs, scheduled files and batch ZIPs (report archive setting)', now(), 'SYSTEM'),
    ('INBOUND_FILE', 'Inbound file', 'INBOUND', null, 'P90D', false, false,
     'Bulk upload files, bank and insurer files, watchlist feeds', now(), 'SYSTEM'),
    ('MIGRATION_EXTRACT', 'Migration extract', 'MIGRATION', null, 'P5D', false, false,
     'Migration extracts and staging files (hosting appendix: expire after 5 days)', now(), 'SYSTEM');

-- ---------- Roles and grants ------------------------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('RECORDS_HOLD_OFFICER', 'Records Legal Hold Officer (DOA)', now(), 'SYSTEM'),
       ('RECORDS_HOLD_APPROVER', 'Records Legal Hold Approver (DOA)', now(), 'SYSTEM'),
       ('INFOSEC_OFFICER', 'Information Security Officer', now(), 'SYSTEM');

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    ('RECORDS_HOLD_OFFICER', 'FILE_LEGAL_HOLD_REQUEST'),
    ('RECORDS_HOLD_APPROVER', 'FILE_LEGAL_HOLD_APPROVE'),
    ('INFOSEC_OFFICER', 'FILE_QUARANTINE_VIEW'),
    ('INFOSEC_OFFICER', 'AUDIT_VIEW'),
    ('AUDITOR', 'FILE_QUARANTINE_VIEW')) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

insert into sec_permission_action (permission, area, action)
values ('FILE_LEGAL_HOLD_REQUEST', 'ADMINISTRATION', 'CREATE'),
       ('FILE_LEGAL_HOLD_APPROVE', 'ADMINISTRATION', 'APPROVE'),
       ('FILE_QUARANTINE_VIEW', 'ADMINISTRATION', 'VIEW');

-- ---------- Parameter -------------------------------------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('FILE_LINK_TTL_SECONDS', '300', 'INTEGER', 'SECURITY',
     'Seconds a presigned file download or upload link stays valid (document storage decision, section 3)',
     30, 3600, now(), 'SYSTEM');

-- ---------- Exception code and notification event ---------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('FILE_QUARANTINED', 'File quarantined by the malware scan',
     'The malware scan of a stored file found a threat or did not complete; the file was moved to quarantine and cannot be downloaded.',
     'STORAGE', 'HIGH', null, null, now(), 'SYSTEM');

insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('FILE_QUARANTINED', 'File quarantined', 'STORAGE',
     'A file you uploaded, or one you review as security officer, was quarantined by the malware scan',
     true, true, 800);
