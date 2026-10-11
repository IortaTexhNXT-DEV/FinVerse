-- =====================================================================================
-- iNXT BrokerVerse - V1010 Renewal (BRD-6) foundation: the roles LAMD and CONTACT_CENTER, the
-- grants of the RNW_* permissions, permission action classes, lists of values, the renewal
-- document types, business parameters, exception codes, notification events, the workflow
-- RNW_CASE and the renewal templates.
--   Requirements: docs/requirements/BDOI_RN_BRD_SPEC.md; FRS BRD-6 v1.1 sections 3, 5 and 9.
--     BRD 6.002.2      the 25 functions granted to the profiles (permissions of section 6.1)
--     BRRN.024/026     LAMD (validation only) and Contact Center roles
--     BRRN.021/023/034 workflow RNW_CASE: extraction, initiation, buckets, matrix, disposition
--     BRRN.030/037     lead days, NRNS and non-acceptance parameters
--     DMQ37            go-live window parameters (MIG_GOLIVE_RENEWAL_TO, MIG_RENEWAL_URGENT_TO)
--   Design: docs/architecture/RENEWAL_DESIGN.md sections 6, 7.1 and 9. The grants follow the
--   matrix of section 6.2 until OQ48, RQ20 and RQ21 are answered; every default value is a
--   placeholder until RQ04, RQ09, RQ11, RQ18 and RQ25 are answered.
--   The document type RENEWAL_ADVICE is shared with Employee Benefits (V1031): inserted with
--   "on conflict do nothing", so the order of the two migrations does not matter.
--   Runs after V750 (LOV), V751 (workflow), V754 (templates), V755 (sec_permission_action), V762
--   (notification events); references platform tables only.
-- =====================================================================================

-- ---------- Roles (design 6.2) -----------------------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('LAMD', 'LAMD (loan status validation)', now(), 'SYSTEM'),
       ('CONTACT_CENTER', 'Contact Center (renewal follow-up)', now(), 'SYSTEM');

-- Grants of the renewal permissions (design 6.2; FRS BRD-6 permissions matrix).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Marketing Team Leader: extraction, assignment, review, overrides, letters, reports
    ('MKT_TL', 'RNW_VIEW'), ('MKT_TL', 'RNW_EXTRACT'), ('MKT_TL', 'RNW_ASSIGN'),
    ('MKT_TL', 'RNW_REVIEW'), ('MKT_TL', 'RNW_OVERRIDE'), ('MKT_TL', 'RNW_DISPOSE'),
    ('MKT_TL', 'RNW_RA_GENERATE'), ('MKT_TL', 'RNW_RA_SEND'), ('MKT_TL', 'RNW_ACCEPT'),
    ('MKT_TL', 'RNW_REPORT_VIEW'), ('MKT_TL', 'RNW_EXPORT'),
    -- Marketing AO / Admin, Account Broker: disposition, transfer requests, letters, reports
    ('MKT_AO', 'RNW_VIEW'), ('MKT_AO', 'RNW_DISPOSE'), ('MKT_AO', 'RNW_ASSIGN'),
    ('MKT_AO', 'RNW_RA_GENERATE'), ('MKT_AO', 'RNW_RA_SEND'), ('MKT_AO', 'RNW_ACCEPT'),
    ('MKT_AO', 'RNW_REPORT_VIEW'), ('MKT_AO', 'RNW_EXPORT'),
    -- Processing Team Leader: extraction, PO assignment, processing, uploads, insurer, letters
    ('PROCESSING_TL', 'RNW_VIEW'), ('PROCESSING_TL', 'RNW_EXTRACT'),
    ('PROCESSING_TL', 'RNW_PROCESS_ASSIGN'), ('PROCESSING_TL', 'RNW_PROCESS'),
    ('PROCESSING_TL', 'RNW_UPLOAD'), ('PROCESSING_TL', 'RNW_INSURER'),
    ('PROCESSING_TL', 'RNW_RA_GENERATE'), ('PROCESSING_TL', 'RNW_RA_SEND'),
    ('PROCESSING_TL', 'RNW_ACCEPT'), ('PROCESSING_TL', 'RNW_REPORT_VIEW'),
    ('PROCESSING_TL', 'RNW_EXPORT'), ('PROCESSING_TL', 'RNW_PACKAGE_REMAP'),
    -- Processing Officer / Broker
    ('PROCESSOR', 'RNW_VIEW'), ('PROCESSOR', 'RNW_PROCESS'), ('PROCESSOR', 'RNW_UPLOAD'),
    ('PROCESSOR', 'RNW_INSURER'), ('PROCESSOR', 'RNW_RA_GENERATE'), ('PROCESSOR', 'RNW_RA_SEND'),
    ('PROCESSOR', 'RNW_ACCEPT'), ('PROCESSOR', 'RNW_REPORT_VIEW'),
    ('PROCESSOR', 'RNW_PACKAGE_REMAP'),
    -- Business Administrator: rules, package map, templates
    ('BUSINESS_ADMIN', 'RNW_VIEW'), ('BUSINESS_ADMIN', 'RNW_SETUP'),
    ('BUSINESS_ADMIN', 'RNW_TEMPLATE_MAINTAIN'), ('BUSINESS_ADMIN', 'RNW_REPORT_VIEW'),
    -- LAMD: validation only (BRRN.024); the upload runs through the bulk upload screens
    ('LAMD', 'RNW_VIEW'), ('LAMD', 'RNW_LAMD_UPLOAD'), ('LAMD', 'RNW_VALIDATE'),
    ('LAMD', 'BULK_PROCESS'),
    -- Contact Center: read-only lists, follow-ups and client documents (BRRN.026)
    ('CONTACT_CENTER', 'RNW_VIEW'), ('CONTACT_CENTER', 'RNW_FOLLOWUP'),
    ('CONTACT_CENTER', 'ATTACHMENT_VIEW'), ('CONTACT_CENTER', 'ATTACHMENT_MANAGE'),
    -- Auditor: read access and reports
    ('AUDITOR', 'RNW_VIEW'), ('AUDITOR', 'RNW_REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- Marketing and Processing upload their files through the bulk upload screens.
insert into sec_role_permission (role_id, permission)
select r.id, 'BULK_PROCESS'
from sec_role r
where r.code in ('MKT_TL', 'PROCESSING_TL', 'PROCESSOR')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = 'BULK_PROCESS');

-- ---------- Permission action classes (User Access Matrix) -------------------------------------
insert into sec_permission_action (permission, area, action)
values
    ('RNW_VIEW', 'RENEWAL', 'VIEW'),
    ('RNW_REPORT_VIEW', 'RENEWAL', 'VIEW'),
    ('RNW_EXPORT', 'RENEWAL', 'VIEW'),
    ('RNW_EXTRACT', 'RENEWAL', 'CREATE'),
    ('RNW_ASSIGN', 'RENEWAL', 'AMEND'),
    ('RNW_DISPOSE', 'RENEWAL', 'AMEND'),
    ('RNW_REVIEW', 'RENEWAL', 'APPROVE'),
    ('RNW_OVERRIDE', 'RENEWAL', 'APPROVE'),
    ('RNW_PROCESS_ASSIGN', 'RENEWAL', 'AMEND'),
    ('RNW_PROCESS', 'RENEWAL', 'AMEND'),
    ('RNW_UPLOAD', 'RENEWAL', 'CREATE'),
    ('RNW_INSURER', 'RENEWAL', 'AMEND'),
    ('RNW_RA_GENERATE', 'RENEWAL', 'CREATE'),
    ('RNW_RA_SEND', 'RENEWAL', 'AMEND'),
    ('RNW_ACCEPT', 'RENEWAL', 'AMEND'),
    ('RNW_FOLLOWUP', 'RENEWAL', 'CREATE'),
    ('RNW_LAMD_UPLOAD', 'RENEWAL', 'CREATE'),
    ('RNW_VALIDATE', 'RENEWAL', 'VIEW'),
    ('RNW_SETUP', 'RENEWAL', 'AMEND'),
    ('RNW_TEMPLATE_MAINTAIN', 'RENEWAL', 'AMEND'),
    ('RNW_PACKAGE_REMAP', 'RENEWAL', 'APPROVE');

-- ---------- Lists of values (design 9; FRS section 9.2) ----------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('RNW_DISPOSITION', 'Renewal disposition', 'Marketing decision on an expiring account; the codes drive the renewal path, only the labels change', true, now(), 'SYSTEM'),
       ('RNW_NONRENEWAL_REASON', 'Reason for Not for Renewal', 'Why an expiring account is not renewed', true, now(), 'SYSTEM'),
       ('RNW_RETURN_REASON', 'Renewal return reason', 'Why a renewal is returned to Marketing', true, now(), 'SYSTEM'),
       ('RNW_TRANSFER_REASON', 'Renewal transfer reason', 'Why a renewal is transferred or re-assigned', true, now(), 'SYSTEM'),
       ('RNW_FOLLOWUP_OUTCOME', 'Renewal follow-up outcome', 'Outcome of a Contact Center follow-up with the client', true, now(), 'SYSTEM'),
       ('RNW_FOLLOWUP_CHANNEL', 'Renewal follow-up channel', 'How the Contact Center reached the client', true, now(), 'SYSTEM'),
       ('RNW_OVERRIDE_REASON', 'Renewal override reason', 'Why a control of the renewal is overridden', true, now(), 'SYSTEM'),
       ('RNW_INSURER_RESPONSE', 'Insurer renewal response', 'Decision of the insurer on a renewal', true, now(), 'SYSTEM'),
       ('RNW_LAMD_STATUS', 'LAMD loan status', 'Loan status reported by LAMD', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('RNW_DISPOSITION', 'FOR_RENEWAL', 'For Renewal', 10),
    ('RNW_DISPOSITION', 'NOT_FOR_RENEWAL', 'Not for Renewal', 20),
    ('RNW_DISPOSITION', 'FOR_QUOTATION', 'For Quotation', 30),
    ('RNW_DISPOSITION', 'FOR_PROPOSAL', 'For Proposal', 40),
    ('RNW_DISPOSITION', 'LOST_BUSINESS', 'Lost Business', 50),
    ('RNW_NONRENEWAL_REASON', 'RMU', 'RMU', 10),
    ('RNW_NONRENEWAL_REASON', 'WITH_SUBMITTED_POLICY', 'With submitted policy', 20),
    ('RNW_NONRENEWAL_REASON', 'LOAN_FULLY_PAID', 'Loan fully paid', 30),
    ('RNW_NONRENEWAL_REASON', 'DIRECT_TO_INSURER', 'Direct to Insurer', 40),
    ('RNW_NONRENEWAL_REASON', 'TOTAL_LOSS_CLAIM', 'Total Loss Claim', 50),
    ('RNW_NONRENEWAL_REASON', 'UNIT_SOLD', 'Unit Sold', 60),
    ('RNW_NONRENEWAL_REASON', 'CANCELED_POLICY', 'Canceled Policy', 70),
    ('RNW_NONRENEWAL_REASON', 'TRANSFER_TO_OTHER_UNIT', 'Transfer to Another Marketing Unit', 80),
    ('RNW_NONRENEWAL_REASON', 'NON_RENEWABLE_ACCOUNT', 'Non-renewable Accounts', 90),
    ('RNW_NONRENEWAL_REASON', 'BOOKED_TO_NEW_INVOICE', 'Booked to New Invoice', 100),
    ('RNW_NONRENEWAL_REASON', 'INSURER_DECLINED', 'Insurer declined the renewal', 110),
    ('RNW_RETURN_REASON', 'DISAPPROVED', 'Disapproved', 10),
    ('RNW_RETURN_REASON', 'INCOMPLETE_DETAILS', 'Incomplete details', 20),
    ('RNW_RETURN_REASON', 'WRONG_DISPOSITION', 'Disposition to be reviewed', 30),
    ('RNW_RETURN_REASON', 'FOR_CORRECTION', 'For correction of the account data', 40),
    ('RNW_RETURN_REASON', 'OTHERS', 'Others (see remarks)', 90),
    ('RNW_TRANSFER_REASON', 'WRONG_UNIT', 'Account belongs to another unit', 10),
    ('RNW_TRANSFER_REASON', 'CLIENT_REQUEST', 'Client request', 20),
    ('RNW_TRANSFER_REASON', 'WORKLOAD', 'Workload balancing', 30),
    ('RNW_TRANSFER_REASON', 'OTHERS', 'Others (see remarks)', 90),
    ('RNW_FOLLOWUP_OUTCOME', 'WILL_RENEW', 'Client will renew', 10),
    ('RNW_FOLLOWUP_OUTCOME', 'NOT_RENEWING', 'Client will not renew', 20),
    ('RNW_FOLLOWUP_OUTCOME', 'CALL_BACK', 'Call back requested', 30),
    ('RNW_FOLLOWUP_OUTCOME', 'NOT_REACHED', 'Client not reached', 40),
    ('RNW_FOLLOWUP_OUTCOME', 'DOCUMENTS_RECEIVED', 'Documents received', 50),
    ('RNW_FOLLOWUP_OUTCOME', 'OTHERS', 'Others (see remarks)', 90),
    ('RNW_FOLLOWUP_CHANNEL', 'CALL', 'Call', 10),
    ('RNW_FOLLOWUP_CHANNEL', 'EMAIL', 'E-mail', 20),
    ('RNW_FOLLOWUP_CHANNEL', 'SMS', 'SMS', 30),
    ('RNW_OVERRIDE_REASON', 'TL_APPROVED', 'Approved by the Team Leader', 10),
    ('RNW_OVERRIDE_REASON', 'PAYMENT_ARRANGED', 'Payment arranged with the client', 20),
    ('RNW_OVERRIDE_REASON', 'CLIENT_CONFIRMED', 'Confirmed with the client', 30),
    ('RNW_OVERRIDE_REASON', 'INSURER_CONFIRMED', 'Confirmed with the insurer', 40),
    ('RNW_OVERRIDE_REASON', 'OTHERS', 'Others (see remarks)', 90),
    ('RNW_INSURER_RESPONSE', 'RENEW_AS_IS', 'Renew As Is', 10),
    ('RNW_INSURER_RESPONSE', 'REVISE', 'Revise', 20),
    ('RNW_INSURER_RESPONSE', 'REJECT', 'Reject', 30),
    ('RNW_LAMD_STATUS', 'PAID_OFF', 'Paid-off', 10),
    ('RNW_LAMD_STATUS', 'RMU', 'RMU', 20)
) as v(type_code, code, label, sort_order);

-- Renewal document types (design 9); RENEWAL_ADVICE is shared with Employee Benefits (V1031).
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'DOCUMENT_TYPE', v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('RENEWAL_ADVICE', 'Renewal advice', 77),
    ('RA_ACCEPTANCE', 'Renewal advice acceptance (e-mail)', 130),
    ('SIGNED_RA', 'Signed renewal advice', 131),
    ('LAMD_REPORT', 'LAMD loan report', 132),
    ('INSURER_RENEWAL_FILE', 'Insurer renewal file', 133),
    ('RENEWAL_LETTER', 'Renewal letter', 134)
) as v(code, label, sort_order)
on conflict (type_code, code) do nothing;

-- ---------- Business parameters (design 9; FRS section 9.1) ------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('RNW_EXTRACTION_LEAD_DAYS', '140', 'INTEGER', 'RENEWAL',
     'Days before expiry at which the daily extraction takes a policy', 1, 400, now(), 'SYSTEM'),
    ('RNW_EXTRACTION_LEAD_DAYS_BY_SEGMENT', '', 'STRING', 'RENEWAL',
     'Lead days per market segment, e.g. CBG=150,IBG=120; empty = the general lead days', null, null, now(), 'SYSTEM'),
    ('RNW_BULK_INITIATION_SEGMENTS', 'CLG', 'CODE_LIST', 'RENEWAL',
     'Market segments whose renewals may be initiated in bulk; the others one at a time', null, null, now(), 'SYSTEM'),
    ('RNW_CBG_SEGMENTS', 'CBG', 'CODE_LIST', 'RENEWAL',
     'Market segments on the loan-driven straight-through path (no assignment to an AO)', null, null, now(), 'SYSTEM'),
    ('RNW_CBG_STP_LINES', 'MOTOR,PROPERTY', 'CODE_LIST', 'RENEWAL',
     'Product lines of the straight-through path of those segments', null, null, now(), 'SYSTEM'),
    ('RNW_PN_REQUIRED_LINES', 'MOTOR,PROPERTY', 'CODE_LIST', 'RENEWAL',
     'Product lines whose mortgaged or straight-through renewals need the PN number', null, null, now(), 'SYSTEM'),
    ('RNW_STP_SKIP_TL_REVIEW', 'true', 'BOOLEAN', 'RENEWAL',
     'Automatic dispositions of the decision matrix skip the Team Leader review', null, null, now(), 'SYSTEM'),
    ('RNW_OUTSTANDING_THRESHOLD', '0.00', 'DECIMAL', 'RENEWAL',
     'Open premium of the expiring invoice family above which the renewal is flagged Outstanding', 0, null, now(), 'SYSTEM'),
    ('RNW_FIN_IMPACT_TOLERANCE', '0.00', 'DECIMAL', 'RENEWAL',
     'Difference in premium, sum insured or rate above which a renewal has a financial impact', 0, null, now(), 'SYSTEM'),
    ('RNW_RA_MIN_NOTICE_DAYS', '30', 'INTEGER', 'RENEWAL',
     'A Renewal Advice generated fewer days than this before expiry needs a confirmation', 0, 365, now(), 'SYSTEM'),
    ('RNW_RA_SECOND_NOTICE_DAYS', '15', 'INTEGER', 'RENEWAL',
     'Days after the first notice without acceptance before a second notice is offered', 1, 365, now(), 'SYSTEM'),
    ('RNW_NRNS_REMINDER_DAYS', '90', 'INTEGER', 'RENEWAL',
     'Days before expiry of the reminder to renewals not yet submitted (NRNS)', 0, 365, now(), 'SYSTEM'),
    ('RNW_NON_ACCEPTANCE_DAYS', '0', 'INTEGER', 'RENEWAL',
     'Days after expiry of the non-acceptance letter and the closure of unrenewed accounts', 0, 365, now(), 'SYSTEM'),
    ('RNW_REOPEN_DAYS', '30', 'INTEGER', 'RENEWAL',
     'Days after expiry during which a Not for Renewal account can be re-opened', 0, 365, now(), 'SYSTEM'),
    ('RNW_ESCALATION_DAYS', 'IBG=60,LEASING=60,*=30', 'STRING', 'RENEWAL',
     'Days to expiry at which an account not disposed or not accepted is at risk, per segment (* = others)', null, null, now(), 'SYSTEM'),
    ('RNW_KYC_SEGMENTS', '', 'CODE_LIST', 'RENEWAL',
     'Market segments where the KYC due flag applies; empty = all', null, null, now(), 'SYSTEM'),
    ('RNW_RMU_UNIT', '', 'STRING', 'RENEWAL',
     'Sales unit that receives RMU accounts; empty = tag them Not for Renewal', null, null, now(), 'SYSTEM'),
    ('RNW_AUTO_PLACEMENT', 'true', 'BOOLEAN', 'RENEWAL',
     'Generate and send the placement slip automatically once an accepted renewal is ready for placement', null, null, now(), 'SYSTEM'),
    ('RNW_REFERENCE_PREFIX', 'RNW', 'STRING', 'RENEWAL',
     'Prefix of the renewal reference (prefix-yyyy-nnnnnn)', null, null, now(), 'SYSTEM'),
    ('RNW_EXCLUDED_LINES', 'HMO,GLI,GPA', 'CODE_LIST', 'RENEWAL',
     'Product lines never extracted for renewal (renewed as Employee Benefits programmes)', null, null, now(), 'SYSTEM'),
    ('RNW_NAL_REASONS', 'DIRECT_TO_INSURER,WITH_SUBMITTED_POLICY,BOOKED_TO_NEW_INVOICE,UNIT_SOLD,CANCELED_POLICY', 'CODE_LIST', 'RENEWAL',
     'Reasons for Not for Renewal answered with a No Advice Letter instead of a Not for Renewal Letter', null, null, now(), 'SYSTEM'),
    ('RNW_INVOICE_NO_REASONS', 'BOOKED_TO_NEW_INVOICE', 'CODE_LIST', 'RENEWAL',
     'Reasons for Not for Renewal that need the number of the new invoice', null, null, now(), 'SYSTEM'),
    ('RNW_INSURER_REPLY_DAYS', '10', 'INTEGER', 'RENEWAL',
     'Days the insurer has to answer a renewal batch', 1, 90, now(), 'SYSTEM'),
    ('RNW_EXCEPTION_AGEING_DAYS', '7', 'INTEGER', 'RENEWAL',
     'Days a renewal may stay in the Exception bucket before it is escalated', 1, 90, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- The go-live window of the renewal take-over is owned by Data Migration (DATA_MIGRATION_DESIGN
-- section 15.1, parameters of its V1080): whichever of V1010 and V1080 runs first inserts them.
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('MIG_GOLIVE_RENEWAL_TO', '2028-05-31', 'STRING', 'DATA_MIGRATION',
     'Last expiry date of the migrated policies taken over as renewals at go-live (yyyy-MM-dd)', null, null, now(), 'SYSTEM'),
    ('MIG_RENEWAL_URGENT_TO', '2028-01-31', 'STRING', 'DATA_MIGRATION',
     'Renewals taken over at go-live that expire up to this date are flagged Urgent (yyyy-MM-dd)', null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- ---------- Exception codes (design 9) -----------------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('RNW_RENEWAL_AT_RISK', 'Renewal at risk',
     'An expiring account is not disposed or not accepted within the escalation days before its expiry.',
     'RENEWAL', 'HIGH', null, null, now(), 'SYSTEM'),
    ('RNW_EXTRACTION_FAILED', 'Renewal extraction failed',
     'The daily extraction of expiring accounts failed; the next run takes the missed dates.',
     'RENEWAL', 'HIGH', null, null, now(), 'SYSTEM'),
    ('RNW_INSURER_OVERDUE', 'Insurer renewal batch overdue',
     'An insurer has not answered a renewal batch by its reply date.',
     'RENEWAL', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('RNW_LETTER_FAILED', 'Renewal letter not delivered',
     'A Renewal Advice or renewal letter could not be sent to the client.',
     'RENEWAL', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('RNW_EXCEPTION_AGEING', 'Renewal exception ageing',
     'A renewal has stayed in the Exception bucket longer than the allowed days.',
     'RENEWAL', 'MEDIUM', null, 7, now(), 'SYSTEM');

-- ---------- Notification events (design 9) --------------------------------------------------------
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('RNW_ASSIGNED', 'Renewal assigned', 'RENEWAL',
     'Expiring accounts were assigned to you for disposition or processing', true, false, 700),
    ('RNW_TRANSFER_REQUESTED', 'Renewal transfer requested', 'RENEWAL',
     'Another Marketing unit transferred an expiring account to your unit', true, true, 710),
    ('RNW_TRANSFER_DECIDED', 'Renewal transfer decided', 'RENEWAL',
     'The receiving unit accepted or declined your transfer of an expiring account', true, false, 720),
    ('RNW_RETURNED', 'Renewal returned', 'RENEWAL',
     'An expiring account was returned to you with remarks', true, true, 730),
    ('RNW_POSTED', 'Renewal posted', 'RENEWAL',
     'Dispositioned accounts were posted by the Team Leader', true, false, 740),
    ('RNW_INSURER_RESPONDED', 'Insurer renewal response', 'RENEWAL',
     'An insurer answered the renewal of an account you process', true, false, 750),
    ('RNW_ACCEPTED', 'Renewal accepted', 'RENEWAL',
     'A client accepted the Renewal Advice of an account of yours', true, false, 760),
    ('RNW_RENEWED', 'Renewal booked', 'RENEWAL',
     'The renewal of an account of yours was booked', true, false, 770),
    ('RNW_GOLIVE_COMPLETED', 'Go-live renewal extraction completed', 'RENEWAL',
     'The expiring migrated policies of the go-live window were taken over as renewals', true, true, 780),
    ('RNW_PACKAGE_DECIDED', 'Renewal package choice decided', 'RENEWAL',
     'Your choice of the package version of a migrated policy was approved or rejected', true, false, 790);

-- ---------- Workflow RNW_CASE (design 7.1; FRS section 5) ------------------------------------------
-- The stage mirrors rnw_candidate.stage. User actions are checked by the renewal services and run
-- as user transitions; routes, insurer matching, expiry and booking run as system transitions. The
-- SLA of a renewal is its days to expiry (alerts), so no stage SLA is configured.
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('RNW_CASE', 'EXTRACTED', 'Extracted', 'RNW_EXTRACT', null, true, false, 10),
       ('RNW_CASE', 'EVALUATING', 'Evaluating', null, null, false, false, 20),
       ('RNW_CASE', 'UNASSIGNED', 'Unassigned Disposition', 'RNW_ASSIGN', null, false, false, 30),
       ('RNW_CASE', 'TRANSFER_PENDING', 'Transfer Pending', 'RNW_ASSIGN', null, false, false, 40),
       ('RNW_CASE', 'FOR_DISPOSITION', 'For Disposition', 'RNW_DISPOSE', null, false, false, 50),
       ('RNW_CASE', 'FOR_TL_REVIEW', 'Review in Progress', 'RNW_REVIEW', null, false, false, 60),
       ('RNW_CASE', 'NB_PATH', 'New Business Path', 'RNW_DISPOSE', null, false, false, 70),
       ('RNW_CASE', 'FOR_PROCESSING', 'For Processing', 'RNW_PROCESS_ASSIGN', null, false, false, 80),
       ('RNW_CASE', 'IN_PROCESSING', 'In Processing', 'RNW_PROCESS', null, false, false, 90),
       ('RNW_CASE', 'WITH_INSURER', 'With Insurer', 'RNW_INSURER', null, false, false, 100),
       ('RNW_CASE', 'RA_READY', 'RA Ready', 'RNW_RA_GENERATE', null, false, false, 110),
       ('RNW_CASE', 'RA_GENERATED', 'RA Generated', 'RNW_RA_SEND', null, false, false, 120),
       ('RNW_CASE', 'RA_SENT', 'Awaiting Response', 'RNW_ACCEPT', null, false, false, 130),
       ('RNW_CASE', 'ACCEPTED', 'Accepted', null, null, false, false, 140),
       ('RNW_CASE', 'FOR_PLACEMENT_BOOKING', 'For Placement and Booking', null, null, false, false, 150),
       ('RNW_CASE', 'LETTER_PENDING', 'Letter Pending', 'RNW_RA_SEND', null, false, false, 160),
       ('RNW_CASE', 'RENEWED', 'Renewed', null, null, false, true, 170),
       ('RNW_CASE', 'CLOSED', 'Closed', null, null, false, true, 180);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('RNW_CASE', 'EXTRACTED', 'initiate', 'EVALUATING', 'Initiate', 'RNW_EXTRACT', false, null, 10),
       ('RNW_CASE', 'EVALUATING', 'route_unassigned', 'UNASSIGNED', 'Route to Unassigned Disposition', 'RNW_EXTRACT', false, null, 10),
       ('RNW_CASE', 'EVALUATING', 'route_review', 'FOR_TL_REVIEW', 'Route to Team Leader Review', 'RNW_EXTRACT', false, null, 20),
       ('RNW_CASE', 'EVALUATING', 'route_processing', 'FOR_PROCESSING', 'Route to Processing', 'RNW_EXTRACT', false, null, 30),
       ('RNW_CASE', 'EVALUATING', 'route_letter', 'LETTER_PENDING', 'Route to Letter Pending', 'RNW_EXTRACT', false, null, 40),
       ('RNW_CASE', 'UNASSIGNED', 'assign', 'FOR_DISPOSITION', 'Assign Disposition', 'RNW_ASSIGN', false, null, 10),
       ('RNW_CASE', 'UNASSIGNED', 'transfer_request', 'TRANSFER_PENDING', 'Transfer', 'RNW_ASSIGN', false, null, 20),
       ('RNW_CASE', 'FOR_DISPOSITION', 'transfer_request', 'TRANSFER_PENDING', 'Transfer', 'RNW_ASSIGN', false, null, 20),
       ('RNW_CASE', 'TRANSFER_PENDING', 'transfer_accept', 'UNASSIGNED', 'Accept Transfer', 'RNW_ASSIGN', false, null, 10),
       ('RNW_CASE', 'TRANSFER_PENDING', 'transfer_back_unassigned', 'UNASSIGNED', 'Return to Sending Unit', 'RNW_ASSIGN', false, null, 20),
       ('RNW_CASE', 'TRANSFER_PENDING', 'transfer_back_disposition', 'FOR_DISPOSITION', 'Return to Sending Unit', 'RNW_ASSIGN', false, null, 30),
       ('RNW_CASE', 'FOR_DISPOSITION', 'push', 'FOR_TL_REVIEW', 'Push to Team Leader', 'RNW_DISPOSE', false, null, 10),
       ('RNW_CASE', 'FOR_TL_REVIEW', 'return', 'FOR_DISPOSITION', 'Return to AO', 'RNW_REVIEW', false, 'RNW_RETURN_REASON', 10),
       ('RNW_CASE', 'FOR_TL_REVIEW', 'post_processing', 'FOR_PROCESSING', 'Post to Processing', 'RNW_REVIEW', false, null, 20),
       ('RNW_CASE', 'FOR_TL_REVIEW', 'post_nb_path', 'NB_PATH', 'Post to the New Business Path', 'RNW_REVIEW', false, null, 30),
       ('RNW_CASE', 'FOR_TL_REVIEW', 'post_letter', 'LETTER_PENDING', 'Post to Letter Pending', 'RNW_REVIEW', false, null, 40),
       ('RNW_CASE', 'FOR_TL_REVIEW', 'post_close', 'CLOSED', 'Post and Close', 'RNW_REVIEW', false, null, 50),
       ('RNW_CASE', 'FOR_PROCESSING', 'assign_po', 'IN_PROCESSING', 'Assign Processing Officer', 'RNW_PROCESS_ASSIGN', false, null, 10),
       ('RNW_CASE', 'FOR_PROCESSING', 'return_to_ao', 'FOR_DISPOSITION', 'Return to AO', 'RNW_PROCESS', false, 'RNW_RETURN_REASON', 20),
       ('RNW_CASE', 'FOR_PROCESSING', 'return_to_tl', 'FOR_TL_REVIEW', 'Return to Marketing TL', 'RNW_PROCESS', false, 'RNW_RETURN_REASON', 30),
       ('RNW_CASE', 'IN_PROCESSING', 'return_to_ao', 'FOR_DISPOSITION', 'Return to AO', 'RNW_PROCESS', false, 'RNW_RETURN_REASON', 20),
       ('RNW_CASE', 'IN_PROCESSING', 'return_to_tl', 'FOR_TL_REVIEW', 'Return to Marketing TL', 'RNW_PROCESS', false, 'RNW_RETURN_REASON', 30),
       ('RNW_CASE', 'IN_PROCESSING', 'send_to_insurer', 'WITH_INSURER', 'Send to Insurer', 'RNW_INSURER', false, null, 10),
       ('RNW_CASE', 'WITH_INSURER', 'insurer_renew', 'RA_READY', 'Insurer Renews As Is', 'RNW_INSURER', false, null, 10),
       ('RNW_CASE', 'WITH_INSURER', 'insurer_revise', 'IN_PROCESSING', 'Insurer Revises', 'RNW_INSURER', false, null, 20),
       ('RNW_CASE', 'WITH_INSURER', 'insurer_reject_remarket', 'FOR_DISPOSITION', 'Insurer Rejects - Re-market', 'RNW_INSURER', false, null, 30),
       ('RNW_CASE', 'WITH_INSURER', 'insurer_reject_close', 'LETTER_PENDING', 'Insurer Rejects - Not for Renewal', 'RNW_INSURER', false, null, 40),
       ('RNW_CASE', 'WITH_INSURER', 'override_insurer', 'RA_READY', 'Override Insurer Mismatch', 'RNW_OVERRIDE', false, null, 50),
       ('RNW_CASE', 'RA_READY', 'generate_ra', 'RA_GENERATED', 'Generate Renewal Advice', 'RNW_RA_GENERATE', false, null, 10),
       ('RNW_CASE', 'RA_READY', 'ra_already_sent', 'RA_SENT', 'Renewal Advice Already Sent', 'RNW_RA_SEND', false, null, 20),
       ('RNW_CASE', 'RA_GENERATED', 'send_ra', 'RA_SENT', 'Send Renewal Advice', 'RNW_RA_SEND', false, null, 10),
       ('RNW_CASE', 'RA_GENERATED', 'cancel_ra', 'RA_READY', 'Cancel Renewal Advice', 'RNW_OVERRIDE', false, null, 90),
       ('RNW_CASE', 'RA_SENT', 'cancel_ra', 'RA_READY', 'Cancel Renewal Advice', 'RNW_OVERRIDE', false, null, 90),
       ('RNW_CASE', 'RA_SENT', 'accept', 'ACCEPTED', 'Record Acceptance', 'RNW_ACCEPT', false, null, 10),
       ('RNW_CASE', 'ACCEPTED', 'fast_track', 'FOR_PLACEMENT_BOOKING', 'Fast Track to Placement and Booking', 'RNW_ACCEPT', false, null, 10),
       ('RNW_CASE', 'FOR_PLACEMENT_BOOKING', 'renewed', 'RENEWED', 'Renewal Booked', 'RNW_ACCEPT', false, null, 10),
       ('RNW_CASE', 'NB_PATH', 'renewed', 'RENEWED', 'Renewal Booked', 'RNW_DISPOSE', false, null, 10),
       ('RNW_CASE', 'NB_PATH', 'nb_lost', 'CLOSED', 'Close as Lost', 'RNW_DISPOSE', false, null, 20),
       ('RNW_CASE', 'LETTER_PENDING', 'send_letter', 'CLOSED', 'Send Letter and Close', 'RNW_RA_SEND', false, null, 10),
       ('RNW_CASE', 'LETTER_PENDING', 'reopen', 'FOR_DISPOSITION', 'Re-open Disposition', 'RNW_DISPOSE,RNW_PROCESS_ASSIGN', false, null, 20),
       ('RNW_CASE', 'CLOSED', 'reopen', 'FOR_DISPOSITION', 'Re-open Disposition', 'RNW_DISPOSE,RNW_PROCESS_ASSIGN', false, null, 10);

-- A system Not for Renewal (non-renewable risk code, LAMD paid-off or RMU) found after initiation.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'RNW_CASE', s.stage_code, 'system_not_for_renewal', 'LETTER_PENDING', 'Not for Renewal (system)', 'RNW_VALIDATE', false, null, 80
from wf_stage s
where s.workflow_code = 'RNW_CASE'
  and s.stage_code in ('UNASSIGNED', 'FOR_DISPOSITION', 'FOR_TL_REVIEW', 'FOR_PROCESSING', 'IN_PROCESSING');

-- Override of a disposition: the account goes back to the Team Leader with the new disposition.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'RNW_CASE', s.stage_code, 'override_disposition', 'FOR_TL_REVIEW', 'Override Disposition', 'RNW_OVERRIDE', false, null, 85
from wf_stage s
where s.workflow_code = 'RNW_CASE'
  and s.stage_code in ('UNASSIGNED', 'FOR_DISPOSITION', 'FOR_PROCESSING', 'IN_PROCESSING', 'LETTER_PENDING');

-- Expiry without renewal: closed EXPIRED_UNRENEWED by the expiry sweep.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'RNW_CASE', s.stage_code, 'expire', 'CLOSED', 'Close as Expired Unrenewed', 'RNW_EXTRACT', false, null, 95
from wf_stage s
where s.workflow_code = 'RNW_CASE'
  and s.stage_code in ('EXTRACTED', 'UNASSIGNED', 'FOR_DISPOSITION', 'FOR_TL_REVIEW', 'FOR_PROCESSING',
                       'IN_PROCESSING', 'WITH_INSURER', 'RA_READY', 'RA_GENERATED', 'RA_SENT');

-- ---------- Templates (design 9; drafts until BDOI supplies the layouts, RQ26) -------------------
insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('RNW_RA_FIRST', 1, 'Renewal Advice - {{assuredName}} - policy {{policyNo}}',
 'Dear {{clientName}},

Your policy {{policyNo}} with {{insurerName}} expires on {{expiryDate}}. We are pleased to offer its renewal for the period {{renewalFrom}} to {{renewalTo}} on the following terms:

Product: {{productName}}
Sum insured: {{currency}} {{sumInsured}}
Renewal premium: {{currency}} {{grossPremium}}

To renew, please reply to this e-mail, sign and return this Renewal Advice, or pay the renewal premium on or before {{expiryDate}}.

Renewal reference: {{renewalRef}}
Account officer: {{aoName}}

Thank you for your continued trust,
BDO Insurance and Reinsurance Brokers, Inc.', date '2020-01-01', now(), 'SYSTEM'),
('RNW_RA_SECOND', 1, 'Second notice: Renewal Advice - {{assuredName}} - policy {{policyNo}}',
 'Dear {{clientName}},

This is our second notice on the renewal of your policy {{policyNo}} with {{insurerName}}, which expires on {{expiryDate}}. The renewal terms remain:

Sum insured: {{currency}} {{sumInsured}}
Renewal premium: {{currency}} {{grossPremium}}

Please reply to this e-mail, sign and return this Renewal Advice, or pay the renewal premium on or before {{expiryDate}} to keep your cover.

Renewal reference: {{renewalRef}}
Account officer: {{aoName}}

BDO Insurance and Reinsurance Brokers, Inc.', date '2020-01-01', now(), 'SYSTEM'),
('RNW_NAL', 1, 'Notice on your policy {{policyNo}}',
 'Dear {{clientName}},

This is to inform you that your policy {{policyNo}} with {{insurerName}}, expiring on {{expiryDate}}, will not be renewed through BDO Insurance and Reinsurance Brokers, Inc. No further action is needed from you.

Reference: {{renewalRef}}
Account officer: {{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('RNW_NFR', 1, 'Your policy {{policyNo}} will not be renewed',
 'Dear {{clientName}},

We regret to inform you that your policy {{policyNo}} with {{insurerName}}, expiring on {{expiryDate}}, is not for renewal: {{reason}}.

For questions, please contact your account officer {{aoName}}.

Reference: {{renewalRef}}
BDO Insurance and Reinsurance Brokers, Inc.', date '2020-01-01', now(), 'SYSTEM'),
('RNW_NRNS_REMINDER', 1, 'Reminder: renewal of your policy {{policyNo}}',
 'Dear {{clientName}},

Your policy {{policyNo}} with {{insurerName}} expires on {{expiryDate}} and its renewal has not been confirmed yet. Please contact your account officer {{aoName}} to renew your cover.

Reference: {{renewalRef}}', date '2020-01-01', now(), 'SYSTEM'),
('RNW_NON_ACCEPTANCE', 1, 'Your policy {{policyNo}} has expired',
 'Dear {{clientName}},

Your policy {{policyNo}} with {{insurerName}} expired on {{expiryDate}} without renewal, as we did not receive your acceptance of the Renewal Advice. Your property or risk is no longer covered by this policy.

Please contact your account officer {{aoName}} if you wish to be covered again.

Reference: {{renewalRef}}', date '2020-01-01', now(), 'SYSTEM'),
('RNW_INSURER_COVER', 1, 'Renewals for your decision - batch {{batchNo}}',
 'Dear {{insurerName}},

Please find attached the policies expiring from {{expiryFrom}} to {{expiryTo}} that we propose for renewal ({{count}} accounts). Please return the file with your decision on each account (Renew As Is, Revise or Reject) by {{replyDue}}.

BDO Insurance and Reinsurance Brokers, Inc. - Processing', date '2020-01-01', now(), 'SYSTEM'),
('RNW_ACCOUNT_DETAILS', 1, 'Renewal account details - {{renewalRef}}',
 'Details of the expiring account {{expiringArn}} of {{clientName}} as of {{asOf}}.', date '2020-01-01', now(), 'SYSTEM');
