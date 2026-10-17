-- =====================================================================================
-- iNXT BrokerVerse - V1070 Submitted Policies (BRD-12) foundation: roles and grants of the SBM
-- permissions, permission action classes, lists of values, document types, business parameters,
-- exception codes, notification events, accounting event types, the workflows SBM_POLICY,
-- SBM_IAAF and SBM_TOR, and the document templates (drafts).
--   Requirements: docs/requirements/BDOI_SP_BRD_SPEC.md; FRS BRD-12 v1.0 sections 3, 5 and 9.
--     BRIDSP-01..33   the functions of the personas (permissions of design section 6.1)
--     BRIDSP-12/14/15 buckets with their renewal action (LOV SBM_BUCKET, parent_code = RENEW,
--                     MANUAL or EXCLUDE)
--     BRIDSP-23/32    lead days per segment, insurer acceptance days
--     BRIDSP-24       notification events and alerts
--     BRIDSP-31       accounting event SBM_HANDLING_FEE (posted by cashiering, V769)
--   Design: docs/architecture/SUBMITTED_POLICIES_DESIGN.md sections 5, 6, 7 and 8. The grants
--   follow the matrix of FRS section 3.3 until SP SQ15 / OQ48 are answered; every default value
--   is a placeholder until SP SQ07-SQ09, SQ11-SQ13 are answered.
--   Runs after V750 (LOV), V751 (workflow), V754 (templates), V755 (sec_permission_action), V762
--   (notification events); references platform tables only.
-- =====================================================================================

-- ---------- Roles (design 6.2) -----------------------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('SBM_HANDLER', 'Submitted Handler', now(), 'SYSTEM'),
       ('SBM_CHECKER', 'Submitted Checker', now(), 'SYSTEM'),
       ('SBM_SANITATION', 'Sanitation Handler', now(), 'SYSTEM'),
       ('SBM_TL', 'Submitted Policies Team Lead', now(), 'SYSTEM'),
       ('SBM_POLICY_REVIEWER', 'Policy Review Officer', now(), 'SYSTEM'),
       ('SBM_RULE_ADMIN', 'Submitted Policies Rule Administrator', now(), 'SYSTEM'),
       ('SBM_UPP_HANDLER', 'Handling Fee Handler (Unapplied Payments)', now(), 'SYSTEM')
on conflict (code) do nothing;

-- Grants (FRS BRD-12 section 3.3). WORK_VIEW, ATTACHMENT_*, REPORT_VIEW and BULK_PROCESS open the
-- platform screens the personas use (My Work, documents, Report Centre, uploads).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    ('SBM_HANDLER', 'SBM_VIEW'), ('SBM_HANDLER', 'SBM_MAINTAIN'), ('SBM_HANDLER', 'SBM_INTAKE'),
    ('SBM_HANDLER', 'IAAF_PREPARE'), ('SBM_HANDLER', 'TOR_PREPARE'), ('SBM_HANDLER', 'SBM_EXPORT'),
    ('SBM_HANDLER', 'SBM_REPORT_VIEW'), ('SBM_HANDLER', 'SBM_REPORT_EXPORT'),
    ('SBM_HANDLER', 'BULK_PROCESS'), ('SBM_HANDLER', 'WORK_VIEW'), ('SBM_HANDLER', 'REPORT_VIEW'),
    ('SBM_HANDLER', 'ATTACHMENT_VIEW'), ('SBM_HANDLER', 'ATTACHMENT_MANAGE'),
    ('SBM_CHECKER', 'SBM_VIEW'), ('SBM_CHECKER', 'IAAF_APPROVE'), ('SBM_CHECKER', 'SBM_REPORT_VIEW'),
    ('SBM_CHECKER', 'WORK_VIEW'), ('SBM_CHECKER', 'REPORT_VIEW'), ('SBM_CHECKER', 'ATTACHMENT_VIEW'),
    ('SBM_SANITATION', 'SBM_VIEW'), ('SBM_SANITATION', 'SBM_MAINTAIN'), ('SBM_SANITATION', 'SBM_INTAKE'),
    ('SBM_SANITATION', 'SBM_PROCESS'), ('SBM_SANITATION', 'SBM_LETTER_SEND'), ('SBM_SANITATION', 'SBM_EXPORT'),
    ('SBM_SANITATION', 'SBM_REPORT_VIEW'), ('SBM_SANITATION', 'SBM_REPORT_EXPORT'),
    ('SBM_SANITATION', 'BULK_PROCESS'), ('SBM_SANITATION', 'WORK_VIEW'), ('SBM_SANITATION', 'REPORT_VIEW'),
    ('SBM_SANITATION', 'ATTACHMENT_VIEW'), ('SBM_SANITATION', 'ATTACHMENT_MANAGE'),
    ('SBM_TL', 'SBM_VIEW'), ('SBM_TL', 'SBM_MAINTAIN'), ('SBM_TL', 'SBM_INTAKE'), ('SBM_TL', 'SBM_PROCESS'),
    ('SBM_TL', 'SBM_RULE_MAINTAIN'), ('SBM_TL', 'IAAF_PREPARE'), ('SBM_TL', 'IAAF_APPROVE'),
    ('SBM_TL', 'TOR_PREPARE'), ('SBM_TL', 'SBM_LETTER_SEND'), ('SBM_TL', 'SBM_HANDLING_FEE'),
    ('SBM_TL', 'SBM_MIGRATE'), ('SBM_TL', 'SBM_EXPORT'), ('SBM_TL', 'SBM_REPORT_VIEW'),
    ('SBM_TL', 'SBM_REPORT_EXPORT'), ('SBM_TL', 'WORK_ASSIGN'), ('SBM_TL', 'WORK_VIEW'),
    ('SBM_TL', 'BULK_PROCESS'), ('SBM_TL', 'REPORT_VIEW'), ('SBM_TL', 'ATTACHMENT_VIEW'),
    ('SBM_TL', 'ATTACHMENT_MANAGE'),
    ('SBM_POLICY_REVIEWER', 'SBM_VIEW'), ('SBM_POLICY_REVIEWER', 'SBM_MAINTAIN'),
    ('SBM_POLICY_REVIEWER', 'IAAF_PREPARE'), ('SBM_POLICY_REVIEWER', 'SBM_REPORT_VIEW'),
    ('SBM_POLICY_REVIEWER', 'WORK_VIEW'), ('SBM_POLICY_REVIEWER', 'REPORT_VIEW'),
    ('SBM_POLICY_REVIEWER', 'ATTACHMENT_VIEW'), ('SBM_POLICY_REVIEWER', 'ATTACHMENT_MANAGE'),
    ('SBM_RULE_ADMIN', 'SBM_RULE_MAINTAIN'),
    ('SBM_UPP_HANDLER', 'SBM_VIEW'), ('SBM_UPP_HANDLER', 'SBM_HANDLING_FEE'),
    ('MKT_AO', 'SBM_VIEW'), ('MKT_AO', 'SBM_MAINTAIN'), ('MKT_AO', 'TOR_PREPARE'),
    ('MKT_TL', 'SBM_VIEW'), ('MKT_TL', 'IAAF_APPROVE'), ('MKT_TL', 'SBM_RULE_APPROVE'),
    ('TSU', 'TOR_APPROVE'),
    ('PROCESSOR', 'SBM_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix) -------------------------------------
insert into sec_permission_action (permission, area, action)
values
    ('SBM_VIEW', 'SUBMITTED_POLICIES', 'VIEW'),
    ('SBM_MAINTAIN', 'SUBMITTED_POLICIES', 'AMEND'),
    ('SBM_INTAKE', 'SUBMITTED_POLICIES', 'CREATE'),
    ('SBM_PROCESS', 'SUBMITTED_POLICIES', 'AMEND'),
    ('SBM_RULE_MAINTAIN', 'SUBMITTED_POLICIES', 'AMEND'),
    ('SBM_RULE_APPROVE', 'SUBMITTED_POLICIES', 'APPROVE'),
    ('IAAF_PREPARE', 'SUBMITTED_POLICIES', 'CREATE'),
    ('IAAF_APPROVE', 'SUBMITTED_POLICIES', 'APPROVE'),
    ('TOR_PREPARE', 'SUBMITTED_POLICIES', 'CREATE'),
    ('TOR_APPROVE', 'SUBMITTED_POLICIES', 'APPROVE'),
    ('SBM_LETTER_SEND', 'SUBMITTED_POLICIES', 'AMEND'),
    ('SBM_HANDLING_FEE', 'SUBMITTED_POLICIES', 'AMEND'),
    ('SBM_MIGRATE', 'SUBMITTED_POLICIES', 'CREATE'),
    ('SBM_EXPORT', 'SUBMITTED_POLICIES', 'VIEW'),
    ('SBM_REPORT_VIEW', 'SUBMITTED_POLICIES', 'VIEW'),
    ('SBM_REPORT_EXPORT', 'SUBMITTED_POLICIES', 'VIEW')
on conflict (permission, action) do nothing;

-- ---------- Lists of values (design 8; FRS section 9.2) ----------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('SBM_SEGMENT', 'Submitted policy segment', 'Segment of a submitted policy', true, now(), 'SYSTEM'),
       ('SBM_BUCKET', 'Submitted policy bucket', 'Qualification group of a submitted policy; the group above each value says whether it is renewed, disposed by hand or excluded', true, now(), 'SYSTEM'),
       ('SBM_REASON', 'Submitted policy reason', 'Reason of a fallout, an exclusion or a manual disposition of a submitted policy', true, now(), 'SYSTEM'),
       ('SBM_NON_RENEWAL_REASON', 'Non-renewal reason (submitted policy)', 'Why a submitted policy is not renewed', true, now(), 'SYSTEM'),
       ('SBM_CONVERSION_STATUS', 'Conversion status', 'Conversion of a submitted policy into a BDOI renewal', true, now(), 'SYSTEM'),
       ('SBM_LOAN_STATUS', 'Loan status', 'Status of the bank loan of a submitted policy', true, now(), 'SYSTEM'),
       ('SBM_LETTER_TYPE', 'Submitted policy letter type', 'Letters sent on submitted policies that are not renewal letters', true, now(), 'SYSTEM'),
       ('SBM_DECLINE_REASON', 'Insurer re-assignment reason', 'Why the insurer of a submitted policy renewal is re-assigned', true, now(), 'SYSTEM'),
       ('SBM_IAAF_FINDING', 'Policy review finding', 'Finding of a policy review of a submitted policy', true, now(), 'SYSTEM'),
       ('SBM_RETURN_REASON', 'IAAF or TOR return reason', 'Why an IAAF or a TOR is returned to its preparer', true, now(), 'SYSTEM')
on conflict (code) do nothing;

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, v.parent_code, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('SBM_SEGMENT', 'CBG_MOTOR', 'CBG Motor', 10, null),
    ('SBM_SEGMENT', 'CBG_FIRE', 'CBG Fire', 20, null),
    ('SBM_SEGMENT', 'NONCBG_CORPORATE', 'Non-CBG Corporate and Branches', 30, null),
    ('SBM_SEGMENT', 'NONCBG_RETAIL', 'Non-CBG Retail', 40, null),
    ('SBM_BUCKET', 'FOR_RENEWAL', 'For Renewal', 10, 'RENEW'),
    ('SBM_BUCKET', 'FOR_MANUAL_DISPOSITION', 'For Manual Disposition', 20, 'MANUAL'),
    ('SBM_BUCKET', 'FOR_REVIEW', 'For Review', 25, 'MANUAL'),
    ('SBM_BUCKET', 'NON_RENEWAL', 'Non-Renewal', 30, 'EXCLUDE'),
    ('SBM_BUCKET', 'NO_TOUCH', 'No Touch', 40, 'EXCLUDE'),
    ('SBM_BUCKET', 'FFY', 'Free First Year', 50, 'EXCLUDE'),
    ('SBM_BUCKET', 'EMPLOYEE', 'BDO / SM Group Employee Accounts', 60, 'EXCLUDE'),
    ('SBM_BUCKET', 'RMU', 'RMU', 70, 'EXCLUDE'),
    ('SBM_BUCKET', 'FULLY_PAID', 'Fully Paid Loan', 80, 'RENEW'),
    ('SBM_BUCKET', 'OPM', 'OPM', 90, 'RENEW'),
    ('SBM_BUCKET', 'BDOFC', 'BDOFC', 100, 'MANUAL'),
    ('SBM_BUCKET', 'SOLD', 'Sold', 110, 'EXCLUDE'),
    ('SBM_REASON', 'SBM_NO_RULE', 'No rule applies to the policy', 10, null),
    ('SBM_REASON', 'MISSING_PN', 'PN number missing', 20, null),
    ('SBM_REASON', 'MISSING_EXPIRY', 'Expiry date missing', 30, null),
    ('SBM_REASON', 'MISSING_ASSURED', 'Assured name missing', 40, null),
    ('SBM_REASON', 'DUPLICATE_PN', 'Same PN as another policy', 50, null),
    ('SBM_REASON', 'DUPLICATE_UNIT', 'Same serial or motor number as another policy', 60, null),
    ('SBM_REASON', 'NO_LAMD_RECORD', 'No record in the LAMD loan snapshot', 70, null),
    ('SBM_REASON', 'PN_MISMATCH', 'Policy details do not match the loan', 80, null),
    ('SBM_REASON', 'LOAN_ACTIVE', 'Active loan', 90, null),
    ('SBM_REASON', 'LOAN_NOT_ACTIVE', 'Loan not active', 100, null),
    ('SBM_REASON', 'EXCLUDED_LIST', 'Account on an exclusion list', 110, null),
    ('SBM_REASON', 'LIMIT_BREACH', 'Above the insurer limits', 120, null),
    ('SBM_REASON', 'NO_INSURER', 'No insurer rule applies', 130, null),
    ('SBM_REASON', 'FOR_REVIEW', 'Documents to review', 140, null),
    ('SBM_REASON', 'MANUAL_DECISION', 'Decided by the handler', 150, null),
    ('SBM_NON_RENEWAL_REASON', 'FFY', 'Free First Year', 10, null),
    ('SBM_NON_RENEWAL_REASON', 'SM_EMPLOYEE', 'SM Group or BDO employee', 20, null),
    ('SBM_NON_RENEWAL_REASON', 'NO_TOUCH', 'No Touch', 30, null),
    ('SBM_NON_RENEWAL_REASON', 'CARI', 'CARI', 40, null),
    ('SBM_NON_RENEWAL_REASON', 'BONDS', 'Bonds', 50, null),
    ('SBM_NON_RENEWAL_REASON', 'RMU', 'RMU', 60, null),
    ('SBM_NON_RENEWAL_REASON', 'MORTGAGED', 'Mortgaged', 70, null),
    ('SBM_NON_RENEWAL_REASON', 'FULLY_PAID', 'Fully paid', 80, null),
    ('SBM_NON_RENEWAL_REASON', 'CANCELLED', 'Cancelled or did not materialise', 90, null),
    ('SBM_NON_RENEWAL_REASON', 'BRANCH_ACCOUNT', 'Branch account', 100, null),
    ('SBM_NON_RENEWAL_REASON', 'TOTAL_LOSS', 'Total loss', 110, null),
    ('SBM_NON_RENEWAL_REASON', 'ENDORSEMENT', 'Endorsement', 120, null),
    ('SBM_NON_RENEWAL_REASON', 'DUPLICATE', 'Duplicate', 130, null),
    ('SBM_NON_RENEWAL_REASON', 'INCORRECT_DATA', 'Incorrect expiry or encoding', 140, null),
    ('SBM_NON_RENEWAL_REASON', 'NO_LAMD_RECORD', 'No record with LAMD', 150, null),
    ('SBM_NON_RENEWAL_REASON', 'OTHERS', 'Others (see remarks)', 900, null),
    ('SBM_CONVERSION_STATUS', 'PROCESS_PLACEMENT', 'Process Placement', 10, null),
    ('SBM_CONVERSION_STATUS', 'PLACED', 'Placed', 15, null),
    ('SBM_CONVERSION_STATUS', 'RENEWED', 'Renewed', 20, null),
    ('SBM_CONVERSION_STATUS', 'UNRENEWED', 'Unrenewed', 30, null),
    ('SBM_CONVERSION_STATUS', 'ISSUED_SFU', 'Issued SFU', 40, null),
    ('SBM_CONVERSION_STATUS', 'FOR_REVIEW', 'For review', 50, null),
    ('SBM_LOAN_STATUS', 'ACTIVE', 'Active', 10, null),
    ('SBM_LOAN_STATUS', 'OPEN_MARKET', 'Open Market', 20, null),
    ('SBM_LOAN_STATUS', 'FULLY_PAID', 'Fully Paid', 30, null),
    ('SBM_LOAN_STATUS', 'REMEDIAL', 'Remedial (RMU)', 40, null),
    ('SBM_LETTER_TYPE', 'REMINDER', 'Reminder', 10, null),
    ('SBM_LETTER_TYPE', 'RENEWAL_NOTICE', 'Renewal notice', 20, null),
    ('SBM_LETTER_TYPE', 'RENEWAL_PROPOSAL', 'Renewal proposal', 30, null),
    ('SBM_DECLINE_REASON', 'NOT_ACCEPTED', 'Insurer did not accept within the days allowed', 10, null),
    ('SBM_DECLINE_REASON', 'DECLINED', 'Insurer declined the hold cover', 20, null),
    ('SBM_DECLINE_REASON', 'BETTER_TERMS', 'Better terms from another insurer', 30, null),
    ('SBM_DECLINE_REASON', 'CLIENT_REQUEST', 'Client request', 40, null),
    ('SBM_DECLINE_REASON', 'OTHERS', 'Others (see remarks)', 90, null),
    ('SBM_IAAF_FINDING', 'SUM_INSURED_BELOW_LOAN', 'Sum insured below the loan amount', 10, null),
    ('SBM_IAAF_FINDING', 'MORTGAGEE_CLAUSE_MISSING', 'Mortgagee clause missing', 20, null),
    ('SBM_IAAF_FINDING', 'PERIOD_NOT_COVERED', 'Loan period not covered', 30, null),
    ('SBM_IAAF_FINDING', 'PERILS_MISSING', 'Required perils not covered', 40, null),
    ('SBM_IAAF_FINDING', 'INSURER_NOT_ACCREDITED', 'Insurer not accredited', 50, null),
    ('SBM_IAAF_FINDING', 'OTHERS', 'Others (see remarks)', 90, null),
    ('SBM_RETURN_REASON', 'INCOMPLETE', 'Incomplete details', 10, null),
    ('SBM_RETURN_REASON', 'WRONG_DATA', 'Data to be corrected', 20, null),
    ('SBM_RETURN_REASON', 'TERMS_TO_REVIEW', 'Terms to be reviewed', 30, null),
    ('SBM_RETURN_REASON', 'OTHERS', 'Others (see remarks)', 90, null)
) as v(type_code, code, label, sort_order, parent_code)
on conflict (type_code, code) do nothing;

-- Document types of the module's files (FRS section 6.3).
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'DOCUMENT_TYPE', v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('SUBMITTED_POLICY', 'Submitted policy document', 140),
    ('SBM_IAAF', 'Insurance Adequacy Assessment Form (IAAF)', 141),
    ('SBM_TOR', 'Terms of Reference (TOR)', 142),
    ('SBM_LETTER', 'Submitted policy letter', 143)
) as v(code, label, sort_order)
on conflict (type_code, code) do nothing;

-- ---------- Business parameters (design 8; FRS section 9.1) ------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('SBM_RENEWAL_LEAD_DAYS', 'CBG_FIRE=150,CBG_MOTOR=120,*=90', 'STRING', 'SUBMITTED',
     'Days before expiry at which a submitted policy for renewal is handed to Renewal, per segment (* = other segments)', null, null, now(), 'SYSTEM'),
    ('SBM_MASTERLIST_DAYS_FROM_EXTRACTION', '30', 'INTEGER', 'SUBMITTED',
     'Target days from the extraction of a CBG Motor policy to its masterlist record', 1, 365, now(), 'SYSTEM'),
    ('SBM_INSURER_ACCEPT_DAYS', '5', 'INTEGER', 'SUBMITTED',
     'Days the assigned insurer has to accept the hold cover of a submitted policy renewal before the handler is alerted', 1, 60, now(), 'SYSTEM'),
    ('SBM_HOLD_COVER_UNBOOKED_ALERT_DAYS', '5', 'INTEGER', 'SUBMITTED',
     'Days before a confirmed hold cover ends at which a renewal account not yet booked is alerted', 0, 60, now(), 'SYSTEM'),
    ('SBM_REVIEW_SLA_DAYS', '5', 'INTEGER', 'SUBMITTED',
     'Days an IAAF or TOR approval level may wait before it is alerted', 1, 60, now(), 'SYSTEM'),
    ('SBM_NUMBER_PREFIX', 'SBM', 'STRING', 'SUBMITTED',
     'Prefix of the masterlist number (prefix-yyyy-nnnnnn)', null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- ---------- Exception codes (design 8) -----------------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('SBM_FALLOUT', 'Submitted policies fallout',
     'A processing run of submitted policies ended with policies no rule could place or that failed a step.',
     'SUBMITTED', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('SBM_INTAKE_FAILED', 'Submitted policy intake failed',
     'Rows of a submitted policy source file could not be loaded.',
     'SUBMITTED', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('SBM_INSURER_NOT_ACCEPTED', 'Insurer has not accepted the hold cover',
     'The insurer assigned to a submitted policy renewal has not accepted the hold cover within the days allowed; follow up or re-assign the insurer.',
     'SUBMITTED', 'HIGH', null, 5, now(), 'SYSTEM'),
    ('SBM_HOLD_COVER_UNBOOKED', 'Hold cover of an unbooked renewal',
     'A confirmed hold cover of a submitted policy renewal ends soon and the renewal account is not booked.',
     'SUBMITTED', 'HIGH', null, 5, now(), 'SYSTEM'),
    ('SBM_IAAF_SLA', 'IAAF approval overdue',
     'An IAAF has waited at an approval level longer than the review days.',
     'SUBMITTED', 'MEDIUM', null, 5, now(), 'SYSTEM'),
    ('SBM_TOR_SLA', 'TOR approval overdue',
     'A Terms of Reference has waited at an approval level longer than the review days.',
     'SUBMITTED', 'MEDIUM', null, 5, now(), 'SYSTEM'),
    ('SBM_LETTER_FAILED', 'Submitted policy letter not sent',
     'A letter on a submitted policy could not be generated or sent.',
     'SUBMITTED', 'MEDIUM', null, null, now(), 'SYSTEM')
on conflict (code) do nothing;

-- ---------- Notification events (design 8; FRS section 5.6) --------------------------------------
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('SBM_NEW_SUBMISSION', 'New submitted policies', 'SUBMITTED',
     'Submitted policies were loaded from a source file', true, false, 800),
    ('SBM_MANUAL_VALIDATION', 'Policy document to confirm', 'SUBMITTED',
     'A submitted policy document waits for the confirmation of its details', true, false, 805),
    ('SBM_IAAF_PENDING', 'IAAF to approve', 'SUBMITTED',
     'An IAAF waits for your approval', true, true, 810),
    ('SBM_TOR_PENDING', 'TOR to approve', 'SUBMITTED',
     'A Terms of Reference waits for your approval', true, true, 815),
    ('SBM_TOR_RELEASED', 'TOR approved', 'SUBMITTED',
     'The Terms of Reference of your account was approved; open or download it', true, true, 820),
    ('SBM_BUCKET_CHANGED', 'Submitted policy bucket changed', 'SUBMITTED',
     'Submitted policies moved to the renewal, manual disposition or non-renewal buckets', true, false, 825),
    ('SBM_FALLOUT', 'Submitted policies fallout', 'SUBMITTED',
     'A processing run ended with fallout to resolve', true, false, 830),
    ('SBM_EXPIRY_NEAR', 'Submitted policy nearing expiry', 'SUBMITTED',
     'A submitted policy for renewal is within the renewal lead days', true, false, 835),
    ('SBM_RENEWAL_STARTED', 'Renewal of a submitted policy started', 'SUBMITTED',
     'A submitted policy was handed to Renewal', true, false, 840),
    ('SBM_PLACEMENT_READY', 'Submitted policy renewal ready for placement', 'SUBMITTED',
     'The renewal account of a submitted policy is ready for placement', true, false, 845),
    ('SBM_PLACEMENT_SENT', 'Submitted policy renewal placed', 'SUBMITTED',
     'The placement slip of a submitted policy renewal was sent to the insurer', true, false, 850),
    ('SBM_HOLD_COVER_UNBOOKED', 'Hold cover of an unbooked renewal', 'SUBMITTED',
     'A hold cover ends soon and the renewal account is not booked', true, true, 855),
    ('SBM_HANDLER_ASSIGNED', 'Submitted policy assigned', 'SUBMITTED',
     'Submitted policies were assigned to you', true, false, 860)
on conflict (code) do nothing;

-- ---------- Accounting event types (design 5; rules are seed data, V1970) -----------------------
insert into acc_event_type (code, name, category, journal_type, description, amount_components) values
 ('SBM_HANDLING_FEE', 'Handling fee recognised', 'RECEIPT', 'RECEIPT',
  'Handling-fee payment of a submitted policy recognised from unapplied collections as income, with the '
  || 'output VAT included in the amount; posted by Cashiering with the official receipt.',
  'AMOUNT,INCOME,OUTPUT_VAT'),
 ('SBM_NO_TOUCH_FEE', 'No Touch service fee billed', 'RECEIPT', 'RECEIPT',
  'Service fee of the No Touch accounts billed to an insurer: service fee receivable against service fee '
  || 'income and output VAT.',
  'RECEIVABLE,INCOME,OUTPUT_VAT')
on conflict (code) do nothing;

-- ---------- Workflow SBM_POLICY (design 7) -------------------------------------------------------
-- The stage mirrors sbm_policy.status. Processing runs, the expiry scan and the account and booking
-- events move records as system transitions; handlers act with user transitions.
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('SBM_POLICY', 'RECEIVED', 'Received', 'SBM_MAINTAIN', null, true, false, 10),
       ('SBM_POLICY', 'VALIDATED', 'Validated', 'SBM_PROCESS', null, false, false, 20),
       ('SBM_POLICY', 'CLASSIFIED', 'Classified', 'SBM_PROCESS', null, false, false, 30),
       ('SBM_POLICY', 'IN_REVIEW', 'In Review', 'IAAF_PREPARE', null, false, false, 40),
       ('SBM_POLICY', 'FOR_RENEWAL', 'For Renewal', 'SBM_PROCESS', null, false, false, 50),
       ('SBM_POLICY', 'FOR_MANUAL_DISPOSITION', 'For Manual Disposition', 'SBM_PROCESS', null, false, false, 60),
       ('SBM_POLICY', 'EXCLUDED', 'Excluded', null, null, false, false, 70),
       ('SBM_POLICY', 'RENEWAL_IN_PROGRESS', 'Renewal in Progress', null, null, false, false, 80),
       ('SBM_POLICY', 'PLACED', 'Placed', null, null, false, false, 90),
       ('SBM_POLICY', 'BOOKED', 'Booked', null, null, false, true, 100),
       ('SBM_POLICY', 'NOT_RENEWED', 'Not Renewed', null, null, false, true, 110),
       ('SBM_POLICY', 'CLOSED', 'Closed', null, null, false, true, 120);

-- Routes of the processing run: from every status before the renewal to every result status.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'SBM_POLICY', f.stage, 'route_' || lower(t.stage), t.stage, 'Route to ' || t.label, 'SBM_PROCESS', false, null, t.sort_order
from (values ('RECEIVED'), ('VALIDATED'), ('CLASSIFIED'), ('IN_REVIEW'), ('FOR_RENEWAL'), ('FOR_MANUAL_DISPOSITION'),
             ('EXCLUDED')) as f(stage)
cross join (values ('CLASSIFIED', 'Classified', 10), ('IN_REVIEW', 'In Review', 20), ('FOR_RENEWAL', 'For Renewal', 30),
                   ('FOR_MANUAL_DISPOSITION', 'For Manual Disposition', 40), ('EXCLUDED', 'Excluded', 50),
                   ('VALIDATED', 'Validated', 60)) as t(stage, label, sort_order)
where f.stage <> t.stage;

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('SBM_POLICY', 'RECEIVED', 'validate', 'VALIDATED', 'Confirm Details', 'SBM_MAINTAIN', false, null, 5),
       ('SBM_POLICY', 'FOR_MANUAL_DISPOSITION', 'dispose', 'FOR_RENEWAL', 'Dispose for Renewal', 'SBM_PROCESS', false, 'SBM_REASON', 70),
       ('SBM_POLICY', 'CLASSIFIED', 'dispose', 'FOR_RENEWAL', 'Dispose for Renewal', 'SBM_PROCESS', false, 'SBM_REASON', 70),
       ('SBM_POLICY', 'IN_REVIEW', 'dispose', 'FOR_RENEWAL', 'Dispose for Renewal', 'SBM_PROCESS', false, 'SBM_REASON', 70),
       ('SBM_POLICY', 'EXCLUDED', 'reinstate', 'VALIDATED', 'Reinstate', 'SBM_PROCESS', false, 'SBM_REASON', 80),
       ('SBM_POLICY', 'FOR_RENEWAL', 'renew', 'RENEWAL_IN_PROGRESS', 'Renew with BDOI', 'SBM_PROCESS,SBM_MAINTAIN', false, null, 90),
       ('SBM_POLICY', 'FOR_MANUAL_DISPOSITION', 'renew', 'RENEWAL_IN_PROGRESS', 'Renew with BDOI', 'SBM_PROCESS,SBM_MAINTAIN', false, null, 90),
       ('SBM_POLICY', 'RENEWAL_IN_PROGRESS', 'placed', 'PLACED', 'Placed', 'SBM_PROCESS', false, null, 10),
       ('SBM_POLICY', 'RENEWAL_IN_PROGRESS', 'booked', 'BOOKED', 'Booked', 'SBM_PROCESS', false, null, 20),
       ('SBM_POLICY', 'PLACED', 'booked', 'BOOKED', 'Booked', 'SBM_PROCESS', false, null, 20),
       ('SBM_POLICY', 'RENEWAL_IN_PROGRESS', 'not_renewed', 'NOT_RENEWED', 'Not Renewed', 'SBM_PROCESS', false, null, 30),
       ('SBM_POLICY', 'PLACED', 'not_renewed', 'NOT_RENEWED', 'Not Renewed', 'SBM_PROCESS', false, null, 30);

-- Exclusion by hand (with the non-renewal reason) and closure from every open status.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'SBM_POLICY', s.stage, 'exclude', 'EXCLUDED', 'Exclude', 'SBM_PROCESS', false, 'SBM_NON_RENEWAL_REASON', 85
from (values ('RECEIVED'), ('VALIDATED'), ('CLASSIFIED'), ('IN_REVIEW'), ('FOR_RENEWAL'), ('FOR_MANUAL_DISPOSITION')) as s(stage);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'SBM_POLICY', s.stage, 'close', 'CLOSED', 'Close', 'SBM_PROCESS', false, 'SBM_REASON', 99
from (values ('RECEIVED'), ('VALIDATED'), ('CLASSIFIED'), ('IN_REVIEW'), ('FOR_RENEWAL'), ('FOR_MANUAL_DISPOSITION'),
             ('EXCLUDED'), ('RENEWAL_IN_PROGRESS'), ('PLACED')) as s(stage);

-- ---------- Workflows SBM_IAAF and SBM_TOR (design 7) --------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('SBM_IAAF', 'DRAFT', 'Draft', 'IAAF_PREPARE', null, true, false, 10),
       ('SBM_IAAF', 'FOR_APPROVAL', 'For Approval', 'IAAF_APPROVE', null, false, false, 20),
       ('SBM_IAAF', 'RETURNED', 'Returned', 'IAAF_PREPARE', null, false, false, 30),
       ('SBM_IAAF', 'APPROVED', 'Approved', 'IAAF_PREPARE', null, false, false, 40),
       ('SBM_IAAF', 'ISSUED', 'Issued', null, null, false, true, 50),
       ('SBM_IAAF', 'CANCELLED', 'Cancelled', null, null, false, true, 60),
       ('SBM_TOR', 'DRAFT', 'Draft', 'TOR_PREPARE', null, true, false, 10),
       ('SBM_TOR', 'FOR_APPROVAL', 'For Approval', 'TOR_APPROVE', null, false, false, 20),
       ('SBM_TOR', 'RETURNED', 'Returned', 'TOR_PREPARE', null, false, false, 30),
       ('SBM_TOR', 'APPROVED', 'Approved', null, null, false, false, 40),
       ('SBM_TOR', 'RELEASED', 'Released', null, null, false, true, 50),
       ('SBM_TOR', 'CANCELLED', 'Cancelled', null, null, false, true, 60);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('SBM_IAAF', 'DRAFT', 'submit', 'FOR_APPROVAL', 'Submit for Approval', 'IAAF_PREPARE', false, null, 10),
       ('SBM_IAAF', 'RETURNED', 'submit', 'FOR_APPROVAL', 'Submit for Approval', 'IAAF_PREPARE', false, null, 10),
       ('SBM_IAAF', 'FOR_APPROVAL', 'approve_level', 'FOR_APPROVAL', 'Approve Level', 'IAAF_APPROVE', false, null, 10),
       ('SBM_IAAF', 'FOR_APPROVAL', 'approve', 'APPROVED', 'Approve', 'IAAF_APPROVE', false, null, 20),
       ('SBM_IAAF', 'FOR_APPROVAL', 'return', 'RETURNED', 'Return', 'IAAF_APPROVE', false, 'SBM_RETURN_REASON', 30),
       ('SBM_IAAF', 'APPROVED', 'issue', 'ISSUED', 'Send to Bank Counterpart', 'IAAF_PREPARE', false, null, 10),
       ('SBM_IAAF', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel', 'IAAF_PREPARE', false, 'SBM_RETURN_REASON', 90),
       ('SBM_IAAF', 'RETURNED', 'cancel', 'CANCELLED', 'Cancel', 'IAAF_PREPARE', false, 'SBM_RETURN_REASON', 90),
       ('SBM_TOR', 'DRAFT', 'submit', 'FOR_APPROVAL', 'Submit for Approval', 'TOR_PREPARE', false, null, 10),
       ('SBM_TOR', 'RETURNED', 'submit', 'FOR_APPROVAL', 'Submit for Approval', 'TOR_PREPARE', false, null, 10),
       ('SBM_TOR', 'FOR_APPROVAL', 'approve_level', 'FOR_APPROVAL', 'Approve Level', 'TOR_APPROVE', false, null, 10),
       ('SBM_TOR', 'FOR_APPROVAL', 'approve', 'APPROVED', 'Approve', 'TOR_APPROVE', false, null, 20),
       ('SBM_TOR', 'FOR_APPROVAL', 'return', 'RETURNED', 'Return', 'TOR_APPROVE', false, 'SBM_RETURN_REASON', 30),
       ('SBM_TOR', 'APPROVED', 'release', 'RELEASED', 'Released to the Account Officer', 'SBM_VIEW', false, null, 10),
       ('SBM_TOR', 'DRAFT', 'cancel', 'CANCELLED', 'Cancel', 'TOR_PREPARE', false, 'SBM_RETURN_REASON', 90),
       ('SBM_TOR', 'RETURNED', 'cancel', 'CANCELLED', 'Cancel', 'TOR_PREPARE', false, 'SBM_RETURN_REASON', 90);

-- ---------- Templates (design 8; drafts until BDOI supplies the layouts) -------------------------
insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('SBM_IAAF', 1, 'Insurance Adequacy Assessment Form {{iaafNo}}',
 'Insurance Adequacy Assessment Form {{iaafNo}}

Borrower: {{borrowerName}}
Assured: {{assuredName}}
Policy: {{policyNo}} with {{insurerName}}, {{inceptionDate}} to {{expiryDate}}
Sum insured: {{currency}} {{sumInsured}}
Masterlist reference: {{sbmNo}}

Reviews ({{reviewCount}}):
{{reviews}}

Related policies: {{links}}

Assessment: {{adequacy}}', date '2020-01-01', now(), 'SYSTEM'),
('SBM_TOR', 1, 'Terms of Reference {{torNo}}',
 'Terms of Reference {{torNo}}

Account: {{accountRef}} - {{assuredName}}
Insurer: {{insurerName}}
Sum insured: {{currency}} {{sumInsured}}

Limits exceeded:
{{breaches}}

Proposed terms:
{{proposedTerms}}

Account Officer: {{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('SBM_REMINDER', 1, 'Reminder on your policy {{policyNo}}',
 'Dear {{assuredName}},

c/o {{bankCounterpart}}

This is a reminder on your policy {{policyNo}} with {{insurerName}}, which expires on {{expiryDate}}. Our review of the policy found: {{findings}}.

BDO Insurance and Reinsurance Brokers, Inc. can assist you with the renewal of this policy. Please contact us at your convenience.

Reference: {{sbmNo}}', date '2020-01-01', now(), 'SYSTEM'),
('SBM_RENEWAL_NOTICE', 1, 'Renewal notice - policy {{policyNo}}',
 'Dear {{assuredName}},

Your policy {{policyNo}} with {{insurerName}} covering {{riskDescription}} expires on {{expiryDate}}. BDO Insurance and Reinsurance Brokers, Inc. can arrange its renewal for you.

Please contact us before the expiry date.

Reference: {{sbmNo}}', date '2020-01-01', now(), 'SYSTEM'),
('SBM_RENEWAL_PROPOSAL', 1, 'Renewal proposal - policy {{policyNo}}',
 'Dear {{assuredName}},

We propose the renewal of your policy {{policyNo}}, expiring on {{expiryDate}}, with {{proposedInsurer}} on the following terms:

Sum insured: {{currency}} {{sumInsured}}

To accept, please reply to this letter before the expiry date.

Reference: {{sbmNo}}', date '2020-01-01', now(), 'SYSTEM'),
('SBM_NO_TOUCH_BILLING', 1, 'No Touch billing statement - {{insurerName}} - {{period}}',
 'Billing statement of the service fee of the No Touch accounts insured with {{insurerName}} for {{period}}.

Accounts: {{count}}
Gross service fee: {{currency}} {{grossFee}}
VAT: {{currency}} {{vat}}
Withholding tax: {{currency}} {{wtax}}
Net amount due: {{currency}} {{netDue}}', date '2020-01-01', now(), 'SYSTEM');
