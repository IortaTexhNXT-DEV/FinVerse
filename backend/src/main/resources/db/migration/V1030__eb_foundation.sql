-- =====================================================================================
-- iNXT BrokerVerse - V1030 Employee Benefits (BRD-8) foundation: roles and grants of the EB
-- permissions, permission action classes, lists of values, EB document types, business
-- parameters, exception codes, notification events, the workflows EB_CYCLE, EB_FRANCHISE,
-- EB_MEMBER_CHANGE and EB_SOA, and the EB templates.
--   Scope: BDOI Drop 2 "Employee Benefits (No Portal Feature)". The partner portal (BRID-005,
--   005.01-005.03, 014 realm and staging; design sections 2.2, 6.3, 10.2) is parked: no portal
--   permission, role grant, parameter, exception code, template or PORTAL_UPLOAD_REVIEW workflow is
--   seeded here. Insurers and client HR send their files by e-mail and the EB users record them
--   (design section 16, as built).
--   Requirements: docs/requirements/BDOI_EB_BRD_SPEC.md
--     BRID-001-004     renewal advice, reminders, feedback (EB_RA_LEAD_DAYS, EB_RA_REMINDER_DAYS; EBQ02)
--     BRID-007/008     document types, TOR, protection of outbound files (EBQ08, EBQ09)
--     BRID-009-012     proposals, comparative, revisions (EB_PROPOSAL_REPLY_DAYS, EB_COMPARATIVE_DAYS)
--     BRID-010/016     comparative sign-off and value threshold approval (EB_TL, EB_MANAGEMENT; EBQ11)
--     BRID-013         member changes (workflow EB_MEMBER_CHANGE; EBQ16)
--     BRID-020         insurer billing number at booking (BOOKING_BILLING_NO_LINES; EBQ18)
--     BRID-021         SOA intake, validation and release (workflow EB_SOA; EBQ19)
--     BRID-022         TAT annex p.42 (EB_TAT_* parameters; EBQ21)
--     BRID-025         process tags (EB_PROCESS_TYPE); access classes follow in V1031
--     BRID-026/027/029 franchise requests (workflow EB_FRANCHISE; EBQ07)
--     BRID-030         tracked items and follow-ups (EB_FOLLOWUP_DAYS, EB_FOLLOWUP_MAX; EBQ20)
--   Design: docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md sections 3, 6, 7 and 8. The permissions
--   are the enum security.domain.Permission; the grants are a proposal until OQ48 is answered.
--   Every default value is a placeholder until EBQ02, EBQ07, EBQ10, EBQ13, EBQ16, EBQ18 and EBQ20
--   are answered. The document type RENEWAL_ADVICE and the access classes come with V1031.
--   Runs after V750 (LOV), V751 (workflow), V754 (templates), V755 (sec_permission_action), V762
--   (notification events) and V1020 (lov_type.owner_permission); references platform tables only.
-- =====================================================================================

-- ---------- EB role profiles (design 6.2) ------------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('EB_AO', 'Marketing Account Officer (Employee Benefits)', now(), 'SYSTEM'),
       ('EB_TL', 'Marketing Team Lead / Unit Head (Employee Benefits)', now(), 'SYSTEM'),
       ('EB_MANAGEMENT', 'BDOI Management (EB threshold approver)', now(), 'SYSTEM'),
       ('EB_PROCESSOR', 'Processing (Employee Benefits)', now(), 'SYSTEM'),
       ('EB_PROC_SUPERVISOR', 'Processing Supervisor (Employee Benefits)', now(), 'SYSTEM'),
       ('EB_COLLECTION', 'Collection (Employee Benefits)', now(), 'SYSTEM');

-- Every EB role: EB screens and reports, My Work, documents, report catalogue, client and account
-- look-ups (FRS BRD-8 section 3.3).
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('EB_VIEW'), ('EB_REPORT_VIEW'), ('WORK_VIEW'), ('ATTACHMENT_VIEW'),
                   ('REPORT_VIEW'), ('CLIENT_VIEW'), ('ACCOUNT_VIEW')) as p(permission)
where r.code in ('EB_AO', 'EB_TL', 'EB_MANAGEMENT', 'EB_PROCESSOR', 'EB_PROC_SUPERVISOR', 'EB_COLLECTION')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Processing (EB) and its supervisor also hold the BRD-1 Processing permissions (design 6.2).
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
join sec_role_permission p on p.role_id = (select id from sec_role where code = 'PROCESSOR')
where r.code in ('EB_PROCESSOR', 'EB_PROC_SUPERVISOR')
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);

-- Role-specific grants (design 6.1-6.2; FRS BRD-8 permissions matrix).
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Marketing AO: maker of the cycle and of member changes
    ('EB_AO', 'EB_MARKET'), ('EB_AO', 'CLIENT_MAINTAIN'),
    ('EB_AO', 'ACCOUNT_MAINTAIN'), ('EB_AO', 'ATTACHMENT_MANAGE'), ('EB_AO', 'BULK_PROCESS'),
    -- Marketing TL / UH: authorised signatory of the comparative, assigns work
    ('EB_TL', 'EB_COMPARATIVE_APPROVE'), ('EB_TL', 'WORK_ASSIGN'),
    -- BDOI Management: approval above the value threshold
    ('EB_MANAGEMENT', 'EB_THRESHOLD_APPROVE'),
    -- Processing and Processing Supervisor: validations, policy forms, SOA, uploads
    ('EB_PROCESSOR', 'EB_PROCESS'), ('EB_PROCESSOR', 'ATTACHMENT_MANAGE'),
    ('EB_PROCESSOR', 'BULK_PROCESS'),
    ('EB_PROC_SUPERVISOR', 'EB_PROCESS'), ('EB_PROC_SUPERVISOR', 'ATTACHMENT_MANAGE'),
    ('EB_PROC_SUPERVISOR', 'BULK_PROCESS'), ('EB_PROC_SUPERVISOR', 'WORK_ASSIGN'),
    -- Collection: SOA and billing, the Collections worklist
    ('EB_COLLECTION', 'EB_COLLECT'), ('EB_COLLECTION', 'CLX_VIEW'), ('EB_COLLECTION', 'CLX_WORK'),
    -- Business Administrator: EB set-up (threshold rules, required documents, parameters)
    ('BUSINESS_ADMIN', 'EB_VIEW'), ('BUSINESS_ADMIN', 'EB_REPORT_VIEW'), ('BUSINESS_ADMIN', 'EB_SETUP'),
    -- Platform administrator and auditor: read access and reports
    ('SYSADMIN', 'EB_VIEW'), ('SYSADMIN', 'EB_REPORT_VIEW'),
    ('AUDITOR', 'EB_VIEW'), ('AUDITOR', 'EB_REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix, PMADD05) ---------------------------
insert into sec_permission_action (permission, area, action)
values
    ('EB_VIEW', 'EMPLOYEE_BENEFITS', 'VIEW'),
    ('EB_REPORT_VIEW', 'EMPLOYEE_BENEFITS', 'VIEW'),
    ('EB_COLLECT', 'EMPLOYEE_BENEFITS', 'VIEW'),
    ('EB_MARKET', 'EMPLOYEE_BENEFITS', 'CREATE'),
    ('EB_MARKET', 'EMPLOYEE_BENEFITS', 'AMEND'),
    ('EB_PROCESS', 'EMPLOYEE_BENEFITS', 'AMEND'),
    ('EB_SETUP', 'EMPLOYEE_BENEFITS', 'AMEND'),
    ('EB_COMPARATIVE_APPROVE', 'EMPLOYEE_BENEFITS', 'APPROVE'),
    ('EB_THRESHOLD_APPROVE', 'EMPLOYEE_BENEFITS', 'APPROVE');

-- ---------- Lists of values (design 8.4; owner EB_SETUP, lov_type.owner_permission V1020) -------
insert into lov_type (code, name, description, maintainable, owner_permission, created_at, created_by)
values ('EB_BENEFIT_LINE', 'EB benefit line', 'Employee Benefits line of a programme (HMO, GLI, GPA; EBQ01)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_TEAM', 'EB team', 'Employee Benefits team of a programme (BRD volumes p.36; EBQ22)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_PROCESS_TYPE', 'EB process type', 'Process tag of an EB document and of the required documents (BRID-025)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_CAPABILITY_FACTOR', 'EB capability factor', 'Insurer capability factor rated on the comparative (BRID-010; EBQ10)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_LOST_REASON', 'EB lost / not renewed reason', 'Why a cycle is lost or not renewed (BRID-003)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_MEMBER_CHANGE_TYPE', 'EB member change type', 'Action of a member change line (BRID-013; EBQ16)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_TRACKED_ITEM_TYPE', 'EB tracked item type', 'Pending item followed up per programme or member (BRID-030; EBQ20)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_FRANCHISE_REJECT_REASON', 'EB franchise rejection reason', 'Why an insurer rejects a franchise request (BRID-027)', true, 'EB_SETUP', now(), 'SYSTEM'),
       ('EB_SOA_REJECT_REASON', 'EB SOA rejection reason', 'Why Processing rejects an insurer SOA (BRID-021)', true, 'EB_SETUP', now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('EB_BENEFIT_LINE', 'HMO', 'Health Maintenance Organization (HMO)', 10),
    ('EB_BENEFIT_LINE', 'GLI', 'Group Life Insurance (GLI)', 20),
    ('EB_BENEFIT_LINE', 'GPA', 'Group Personal Accident (GPA)', 30),
    ('EB_TEAM', 'BDO', 'BDO', 10),
    ('EB_TEAM', 'SM', 'SM', 20),
    ('EB_TEAM', 'VOLUNTARY', 'Voluntary', 30),
    ('EB_TEAM', 'SOLICITED', 'Solicited', 40),
    ('EB_TEAM', 'NEW_BUSINESS', 'New Business', 50),
    ('EB_PROCESS_TYPE', 'NB_PLACEMENT', 'New business placement', 10),
    ('EB_PROCESS_TYPE', 'RENEWAL_PLACEMENT', 'Renewal placement', 20),
    ('EB_PROCESS_TYPE', 'ENDORSEMENT', 'Endorsement', 30),
    ('EB_PROCESS_TYPE', 'ADJUSTMENT', 'Adjustment', 40),
    ('EB_PROCESS_TYPE', 'FRANCHISE', 'Franchise request', 50),
    ('EB_PROCESS_TYPE', 'PROPOSAL', 'Proposal', 60),
    ('EB_CAPABILITY_FACTOR', 'COMPANY_STABILITY', 'Company stability', 10),
    ('EB_CAPABILITY_FACTOR', 'CLINIC_PROVIDERS', 'Clinic providers', 20),
    ('EB_CAPABILITY_FACTOR', 'HOSPITAL_NETWORK', 'Hospital network', 30),
    ('EB_CAPABILITY_FACTOR', 'TECHNOLOGY', 'Technology', 40),
    ('EB_LOST_REASON', 'PRICE', 'Premium not competitive', 10),
    ('EB_LOST_REASON', 'COVERAGE', 'Coverage or benefits not met', 20),
    ('EB_LOST_REASON', 'SERVICE', 'Service or network issues', 30),
    ('EB_LOST_REASON', 'OTHER_BROKER', 'Placed through another broker', 40),
    ('EB_LOST_REASON', 'NO_RESPONSE', 'No response from the client', 50),
    ('EB_LOST_REASON', 'CLIENT_CLOSED', 'Client closed or merged', 60),
    ('EB_LOST_REASON', 'OTHERS', 'Others (see remarks)', 90),
    ('EB_MEMBER_CHANGE_TYPE', 'ADD', 'Add member', 10),
    ('EB_MEMBER_CHANGE_TYPE', 'DELETE', 'Delete member', 20),
    ('EB_MEMBER_CHANGE_TYPE', 'CHANGE_PLAN', 'Change plan', 30),
    ('EB_MEMBER_CHANGE_TYPE', 'CHANGE_DATA', 'Change member data', 40),
    ('EB_TRACKED_ITEM_TYPE', 'CONTRACT', 'Contract / policy', 10),
    ('EB_TRACKED_ITEM_TYPE', 'HMO_CARD', 'HMO card', 20),
    ('EB_TRACKED_ITEM_TYPE', 'CARD_REPLACEMENT', 'Card replacement', 30),
    ('EB_TRACKED_ITEM_TYPE', 'BILLING_INVOICE', 'Billing / invoice', 40),
    ('EB_FRANCHISE_REJECT_REASON', 'INCOMPLETE_DOCUMENTS', 'Incomplete documents', 10),
    ('EB_FRANCHISE_REJECT_REASON', 'EXISTING_BROKER', 'Account held by another broker', 20),
    ('EB_FRANCHISE_REJECT_REASON', 'RISK_APPETITE', 'Outside the insurer''s risk appetite', 30),
    ('EB_FRANCHISE_REJECT_REASON', 'OTHERS', 'Others (see remarks)', 90),
    ('EB_SOA_REJECT_REASON', 'AMOUNT_MISMATCH', 'Amount does not match the billing', 10),
    ('EB_SOA_REJECT_REASON', 'WRONG_PERIOD', 'Wrong period or programme', 20),
    ('EB_SOA_REJECT_REASON', 'UNREADABLE', 'File unreadable or incomplete', 30),
    ('EB_SOA_REJECT_REASON', 'OTHERS', 'Others (see remarks)', 90),
    -- Employee Benefits document types (spec 6.1; access classes in V1031)
    ('DOCUMENT_TYPE', 'EB_CLIENT_FEEDBACK', 'EB client feedback', 101),
    ('DOCUMENT_TYPE', 'EB_BOR', 'Broker on Record (signed)', 102),
    ('DOCUMENT_TYPE', 'EB_TOR', 'Terms of Reference (TOR)', 103),
    ('DOCUMENT_TYPE', 'EB_MASTERLIST', 'Master list (named)', 104),
    ('DOCUMENT_TYPE', 'EB_MASTERLIST_UNNAMED', 'Unnamed master list (census)', 105),
    ('DOCUMENT_TYPE', 'EB_UTILIZATION', 'Utilization report', 106),
    ('DOCUMENT_TYPE', 'EB_INDICATIVE_PROPOSAL', 'Indicative (incumbent) proposal', 107),
    ('DOCUMENT_TYPE', 'EB_PROPOSAL', 'Insurer proposal', 108),
    ('DOCUMENT_TYPE', 'EB_COMPARATIVE', 'Comparative analysis', 109),
    ('DOCUMENT_TYPE', 'EB_FRANCHISE_FORM', 'Franchise request form', 110),
    ('DOCUMENT_TYPE', 'EB_CLIENT_CONFIRMATION', 'Client confirmation of the chosen proposal', 111),
    ('DOCUMENT_TYPE', 'EB_POLICY_FORM', 'EB policy form / contract', 112),
    ('DOCUMENT_TYPE', 'EB_DIRECT_BILLING', 'Insurer direct billing (EB changes)', 113),
    ('DOCUMENT_TYPE', 'EB_SOA', 'Insurer statement of account (EB)', 114),
    ('DOCUMENT_TYPE', 'EB_MEMBER_CHANGE', 'Member change request', 115),
    ('DOCUMENT_TYPE', 'EB_ISACOM_APPROVAL', 'ISACOM approval (non-accredited provider)', 116)
) as v(type_code, code, label, sort_order);

-- ---------- Business parameters (design 8.3; spec 6.3 TAT annex p.42) ---------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('EB_RA_LEAD_DAYS', '135', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Days before expiry the renewal advice is sent and the renewal cycle opened (BRID-001; EBQ02)', 1, 365, now(), 'SYSTEM'),
    ('EB_RA_REMINDER_DAYS', '90,105,120', 'INTEGER_LIST', 'EMPLOYEE_BENEFITS',
     'Days before expiry of the RA reminders while no feedback is recorded (BRID-002; EBQ02)', 1, 365, now(), 'SYSTEM'),
    ('EB_PROPOSAL_REPLY_DAYS', '5', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Working days an insurer has to answer a request for proposal (TAT p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_FRANCHISE_TAT_DAYS', '5', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Working days for the insurer franchise decision (BRID-027; EBQ07)', 1, 60, now(), 'SYSTEM'),
    ('EB_FRANCHISE_ADVICE_DAYS', '2', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Working days to advise the client of the franchise outcome (BRID-029)', 1, 30, now(), 'SYSTEM'),
    ('EB_COMPARATIVE_DAYS', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Working days from the last proposal to the comparative sent to the client (TAT p.42)', 1, 30, now(), 'SYSTEM'),
    ('EB_FOLLOWUP_DAYS', '5', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Working days between follow-ups of a pending tracked item (BRID-030; EBQ20)', 1, 60, now(), 'SYSTEM'),
    ('EB_FOLLOWUP_MAX', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Follow-ups sent before a tracked item is escalated (BRID-030; EBQ20)', 1, 20, now(), 'SYSTEM'),
    ('EB_ADJ_BOOKING_REQUIRES_PAYMENT', 'false', 'BOOLEAN', 'EMPLOYEE_BENEFITS',
     'Raise the endorsement request of a member change only after its billing is paid (p.3; EBQ16)', null, null, now(), 'SYSTEM'),
    ('EB_TAT_FRANCHISE_SUBMIT', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to submit a franchise request upon complete documents (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_RFQ_SUBMIT', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to send the request for quotation upon franchise approval (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_CONFIRMATION', '2', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to send the client confirmation to the insurer (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_COC', '1', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days for the insurer certificate of cover (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_PLACEMENT_REQUEST', '5', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to request placement and booking upon client confirmation (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_PLACEMENT_SLIP', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: days for Processing to submit the placement slip (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_POLICY_SOA', '10', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days for the insurer policy / contract and billing / SOA (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_SOA_VALIDATION', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days for Processing to validate the final SOA (p.42; alert EB_SOA_VALIDATION_LATE)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_BOOKING', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to book once billing is validated (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_POLICY_CHECK', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to check the policy / contract against the proposal (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_RELEASE', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to release the SOA to Collection and the policy to the client (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_MEMBER_CHANGE', '3', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days per step of an inclusion / deletion (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_COLLECTION', '2', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to collect the premium upon SOA / billing receipt (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_CARDS', '10', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days to submit, validate and release member cards (p.42)', 1, 60, now(), 'SYSTEM'),
    ('EB_TAT_OR', '5', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'TAT: working days for the insurer to submit the official receipt (weekly, p.42)', 1, 60, now(), 'SYSTEM'),
    ('BOOKING_BILLING_NO_LINES', 'HMO,GLI,GPA', 'CODE_LIST', 'BOOKING',
     'Product lines whose bookings need the insurer billing number, unique per insurer (BRID-020; EBQ18)', null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- ---------- Exception codes (design 8.2) --------------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('EB_RA_NOT_SENT', 'EB renewal advice not sent',
     'A programme is inside the RA lead time and no renewal advice was sent (no contact, no renewal flag) (BRID-001).',
     'EMPLOYEE_BENEFITS', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('EB_FRANCHISE_OVERDUE', 'EB franchise decision overdue',
     'A franchise request has no insurer decision after EB_FRANCHISE_TAT_DAYS (BRID-027).',
     'EMPLOYEE_BENEFITS', 'MEDIUM', null, 5, now(), 'SYSTEM'),
    ('EB_PROPOSAL_OVERDUE', 'EB proposal overdue',
     'An insurer request for proposal is past its due date without an answer (BRID-009).',
     'EMPLOYEE_BENEFITS', 'LOW', null, 5, now(), 'SYSTEM'),
    ('EB_COMPARATIVE_LATE', 'EB comparative late',
     'No comparative was sent EB_COMPARATIVE_DAYS after the last proposal (BRID-010, TAT p.42).',
     'EMPLOYEE_BENEFITS', 'LOW', null, 3, now(), 'SYSTEM'),
    ('EB_SOA_VALIDATION_LATE', 'EB SOA validation late',
     'An insurer SOA is not validated within EB_TAT_SOA_VALIDATION working days (BRID-021).',
     'EMPLOYEE_BENEFITS', 'MEDIUM', null, 3, now(), 'SYSTEM'),
    ('EB_ITEM_OVERDUE', 'EB pending item overdue',
     'A contract, HMO card or billing is past its due date after the follow-ups (BRID-030).',
     'EMPLOYEE_BENEFITS', 'LOW', null, null, now(), 'SYSTEM');

-- ---------- Notification events (design 8.2; wording EBQ21) -------------------------------------
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('EB_FEEDBACK_RECEIVED', 'EB client feedback received', 'EMPLOYEE_BENEFITS',
     'A client gave feedback on the renewal advice of your programme (BRID-003)', true, false, 500),
    ('EB_FRANCHISE_DECIDED', 'EB franchise decided', 'EMPLOYEE_BENEFITS',
     'An insurer approved or rejected a franchise request of yours (BRID-027)', true, true, 510),
    ('EB_PROPOSAL_RECEIVED', 'EB proposal received', 'EMPLOYEE_BENEFITS',
     'An insurer submitted a proposal to validate (BRID-009)', true, false, 520),
    ('EB_COMPARATIVE_SIGNOFF', 'EB comparative to sign off', 'EMPLOYEE_BENEFITS',
     'A comparative waits for your sign-off (BRID-010)', true, true, 530),
    ('EB_THRESHOLD_APPROVAL', 'EB threshold approval', 'EMPLOYEE_BENEFITS',
     'A comparative above the value threshold waits for your approval (BRID-016)', true, true, 540),
    ('EB_CLIENT_CONFIRMED', 'EB client confirmation', 'EMPLOYEE_BENEFITS',
     'A client confirmed the chosen proposals of your programme (BRID-017)', true, false, 550),
    ('EB_PLACEMENT_TRIGGERED', 'EB placement triggered', 'EMPLOYEE_BENEFITS',
     'The accounts of an EB cycle were created for placement (BRID-017, 019)', true, false, 560),
    ('EB_MEMBER_CHANGE_BILLED', 'EB member change billed', 'EMPLOYEE_BENEFITS',
     'An insurer billed a member change to validate (BRID-013, 025)', true, false, 570),
    ('EB_SOA_RELEASED', 'EB SOA released', 'EMPLOYEE_BENEFITS',
     'An insurer SOA was validated and released to Collection and the client (BRID-021)', true, false, 580),
    ('EB_INVOICE_PAID', 'EB invoice paid', 'EMPLOYEE_BENEFITS',
     'An invoice of an EB programme became paid (BRID-021)', true, false, 590),
    ('EB_ITEM_ESCALATED', 'EB pending item escalated', 'EMPLOYEE_BENEFITS',
     'A tracked item passed its follow-ups without being received (BRID-030)', true, true, 600),
    ('BOOKING_BILLING_BOOKED', 'Booked with billing number', 'BOOKING',
     'An account was booked with its insurer billing number (BRID-020)', true, false, 620),
    ('BOOKING_BILLING_DUPLICATE', 'Duplicate billing number', 'BOOKING',
     'A booking was refused because the insurer billing number is already on an invoice (BRID-020)', true, true, 630);

-- ---------- Workflow EB_CYCLE (design 7.1; FRS BRD-8 section 5.1) -------------------------------
-- The stage mirrors eb_cycle.stage. Business actions are called by the EB services
-- (systemTransition after their own checks: four eyes, BOR, threshold rules, required documents);
-- the permission column documents the right. close_lost / not_renewed are generic with a reason.
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('EB_CYCLE', 'OPEN', 'Open', 'EB_MARKET', null, true, false, 10),
       ('EB_CYCLE', 'RA_SENT', 'Renewal advice sent', 'EB_MARKET', null, false, false, 20),
       ('EB_CYCLE', 'REQUIREMENTS', 'Client requirements', 'EB_MARKET', null, false, false, 30),
       ('EB_CYCLE', 'INCUMBENT_TERMS', 'Incumbent terms', 'EB_MARKET', null, false, false, 40),
       ('EB_CYCLE', 'FRANCHISE', 'Franchise', 'EB_MARKET', null, false, false, 50),
       ('EB_CYCLE', 'PROPOSALS', 'Proposals', 'EB_MARKET', null, false, false, 60),
       ('EB_CYCLE', 'COMPARATIVE', 'Comparative', 'EB_MARKET', null, false, false, 70),
       ('EB_CYCLE', 'FOR_SIGNOFF', 'For sign-off', 'EB_COMPARATIVE_APPROVE', 24, false, false, 80),
       ('EB_CYCLE', 'THRESHOLD_APPROVAL', 'Threshold approval', 'EB_THRESHOLD_APPROVE', 24, false, false, 90),
       ('EB_CYCLE', 'READY_TO_PRESENT', 'Ready to present', 'EB_MARKET', null, false, false, 100),
       ('EB_CYCLE', 'WITH_CLIENT', 'With client', 'EB_MARKET', null, false, false, 110),
       ('EB_CYCLE', 'REVISION', 'Revision', 'EB_MARKET', null, false, false, 120),
       ('EB_CYCLE', 'CONFIRMED', 'Confirmed', 'EB_MARKET', null, false, false, 130),
       ('EB_CYCLE', 'IN_PLACEMENT', 'In placement', null, null, false, false, 140),
       ('EB_CYCLE', 'PLACED', 'Placed', null, null, false, true, 150),
       ('EB_CYCLE', 'CLOSED_LOST', 'Closed - lost', null, null, false, true, 160),
       ('EB_CYCLE', 'NOT_RENEWED', 'Not renewed', null, null, false, true, 170);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('EB_CYCLE', 'OPEN', 'send_ra', 'RA_SENT', 'Send Renewal Advice', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'OPEN', 'start', 'REQUIREMENTS', 'Start Requirements', 'EB_MARKET', false, null, 20),
       ('EB_CYCLE', 'RA_SENT', 'record_feedback', 'REQUIREMENTS', 'Record Feedback', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'REQUIREMENTS', 'stay_with_incumbent', 'INCUMBENT_TERMS', 'Stay with Incumbent', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'REQUIREMENTS', 'remarket', 'FRANCHISE', 'Remarket', 'EB_MARKET', false, null, 20),
       ('EB_CYCLE', 'FRANCHISE', 'release_tor', 'PROPOSALS', 'Release TOR', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'INCUMBENT_TERMS', 'build_comparative', 'COMPARATIVE', 'Build Comparative', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'PROPOSALS', 'build_comparative', 'COMPARATIVE', 'Build Comparative', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'REVISION', 'build_comparative', 'COMPARATIVE', 'Build Comparative', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'COMPARATIVE', 'submit', 'FOR_SIGNOFF', 'Submit for Sign-off', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'FOR_SIGNOFF', 'approve', 'READY_TO_PRESENT', 'Sign Off', 'EB_COMPARATIVE_APPROVE', false, null, 10),
       ('EB_CYCLE', 'FOR_SIGNOFF', 'approve_to_threshold', 'THRESHOLD_APPROVAL', 'Sign Off (threshold approval needed)', 'EB_COMPARATIVE_APPROVE', false, null, 20),
       ('EB_CYCLE', 'FOR_SIGNOFF', 'return', 'COMPARATIVE', 'Return to AO', 'EB_COMPARATIVE_APPROVE', false, null, 30),
       ('EB_CYCLE', 'THRESHOLD_APPROVAL', 'approve', 'READY_TO_PRESENT', 'Approve', 'EB_THRESHOLD_APPROVE', false, null, 10),
       ('EB_CYCLE', 'THRESHOLD_APPROVAL', 'return', 'COMPARATIVE', 'Return to AO', 'EB_THRESHOLD_APPROVE', false, null, 20),
       ('EB_CYCLE', 'READY_TO_PRESENT', 'present', 'WITH_CLIENT', 'Present to Client', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'WITH_CLIENT', 'request_revision', 'REVISION', 'Request Revision', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'WITH_CLIENT', 'confirm', 'CONFIRMED', 'Record Confirmation', 'EB_MARKET', false, null, 20),
       ('EB_CYCLE', 'WITH_CLIENT', 'reconfirm_threshold', 'THRESHOLD_APPROVAL', 'Threshold Approval Needed', 'EB_MARKET', false, null, 30),
       ('EB_CYCLE', 'CONFIRMED', 'trigger_placement', 'IN_PLACEMENT', 'Trigger Placement', 'EB_MARKET', false, null, 10),
       ('EB_CYCLE', 'IN_PLACEMENT', 'placed', 'PLACED', 'All Accounts Booked', 'EB_MARKET', false, null, 10);

-- close_lost from every open stage before placement; not_renewed from the renewal stages.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'EB_CYCLE', s.stage_code, 'close_lost', 'CLOSED_LOST', 'Close as Lost', 'EB_MARKET', true, 'EB_LOST_REASON', 80
from wf_stage s
where s.workflow_code = 'EB_CYCLE'
  and s.stage_code in ('OPEN', 'RA_SENT', 'REQUIREMENTS', 'INCUMBENT_TERMS', 'FRANCHISE', 'PROPOSALS',
                       'COMPARATIVE', 'READY_TO_PRESENT', 'WITH_CLIENT', 'REVISION', 'CONFIRMED');

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
select 'EB_CYCLE', s.stage_code, 'not_renewed', 'NOT_RENEWED', 'Not Renewed', 'EB_MARKET', true, 'EB_LOST_REASON', 90
from wf_stage s
where s.workflow_code = 'EB_CYCLE'
  and s.stage_code in ('RA_SENT', 'REQUIREMENTS', 'INCUMBENT_TERMS', 'COMPARATIVE', 'READY_TO_PRESENT',
                       'WITH_CLIENT', 'REVISION');

-- ---------- Workflow EB_FRANCHISE (design 7.2; BRID-026, 027, 029) ------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('EB_FRANCHISE', 'DRAFT', 'Draft', 'EB_MARKET', null, true, false, 10),
       ('EB_FRANCHISE', 'SUBMITTED', 'Submitted to insurer', null, null, false, false, 20),
       ('EB_FRANCHISE', 'APPROVED', 'Approved', 'EB_MARKET', null, false, false, 30),
       ('EB_FRANCHISE', 'REJECTED', 'Rejected', 'EB_MARKET', null, false, false, 40),
       ('EB_FRANCHISE', 'EXPIRED', 'Expired', null, null, false, true, 50),
       ('EB_FRANCHISE', 'ADVISED', 'Client advised', null, null, false, true, 60);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('EB_FRANCHISE', 'DRAFT', 'submit', 'SUBMITTED', 'Submit to Insurer', 'EB_MARKET', false, null, 10),
       ('EB_FRANCHISE', 'SUBMITTED', 'approve', 'APPROVED', 'Record Approval', 'EB_MARKET', false, null, 10),
       ('EB_FRANCHISE', 'SUBMITTED', 'reject', 'REJECTED', 'Record Rejection', 'EB_MARKET', false, 'EB_FRANCHISE_REJECT_REASON', 20),
       ('EB_FRANCHISE', 'SUBMITTED', 'expire', 'EXPIRED', 'Expire', 'EB_MARKET', false, null, 30),
       ('EB_FRANCHISE', 'APPROVED', 'advise', 'ADVISED', 'Advise Client', 'EB_MARKET', false, null, 10),
       ('EB_FRANCHISE', 'REJECTED', 'advise', 'ADVISED', 'Advise Client', 'EB_MARKET', false, null, 10);

-- ---------- Workflow EB_MEMBER_CHANGE (design 7.2; BRID-013, 025) -------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('EB_MEMBER_CHANGE', 'CAPTURED', 'Captured', 'EB_MARKET', null, true, false, 10),
       ('EB_MEMBER_CHANGE', 'RELAYED', 'Relayed to insurer', null, null, false, false, 20),
       ('EB_MEMBER_CHANGE', 'BILLED', 'Billed', 'EB_PROCESS', null, false, false, 30),
       ('EB_MEMBER_CHANGE', 'VALIDATED', 'Validated', 'EB_MARKET', null, false, false, 40),
       ('EB_MEMBER_CHANGE', 'CLOSED', 'Closed', null, null, false, true, 50),
       ('EB_MEMBER_CHANGE', 'CANCELLED', 'Cancelled', null, null, false, true, 60);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('EB_MEMBER_CHANGE', 'CAPTURED', 'relay', 'RELAYED', 'Relay to Insurer', 'EB_MARKET', false, null, 10),
       ('EB_MEMBER_CHANGE', 'CAPTURED', 'cancel', 'CANCELLED', 'Cancel', 'EB_MARKET', true, 'VOID_REASON', 90),
       ('EB_MEMBER_CHANGE', 'RELAYED', 'bill', 'BILLED', 'Record Billing', 'EB_MARKET', false, null, 10),
       ('EB_MEMBER_CHANGE', 'RELAYED', 'return', 'CAPTURED', 'Return', 'EB_MARKET', true, 'RETURN_REASON', 20),
       ('EB_MEMBER_CHANGE', 'RELAYED', 'cancel', 'CANCELLED', 'Cancel', 'EB_MARKET', true, 'VOID_REASON', 90),
       ('EB_MEMBER_CHANGE', 'BILLED', 'validate', 'VALIDATED', 'Validate', 'EB_PROCESS', false, null, 10),
       ('EB_MEMBER_CHANGE', 'BILLED', 'return', 'CAPTURED', 'Return', 'EB_PROCESS', true, 'RETURN_REASON', 20),
       ('EB_MEMBER_CHANGE', 'VALIDATED', 'close', 'CLOSED', 'Close', 'EB_MARKET', false, null, 10);

-- ---------- Workflow EB_SOA (design 7.2; BRID-021) ----------------------------------------------
insert into wf_stage (workflow_code, stage_code, name, owner_permission, sla_hours, initial, terminal, sort_order)
values ('EB_SOA', 'RECEIVED', 'Received', 'EB_PROCESS', 72, true, false, 10),
       ('EB_SOA', 'VALIDATED', 'Validated', 'EB_PROCESS', null, false, false, 20),
       ('EB_SOA', 'RELEASED', 'Released', null, null, false, true, 30),
       ('EB_SOA', 'REJECTED', 'Rejected', null, null, false, true, 40);

insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('EB_SOA', 'RECEIVED', 'validate', 'VALIDATED', 'Validate', 'EB_PROCESS', false, null, 10),
       ('EB_SOA', 'RECEIVED', 'reject', 'REJECTED', 'Reject', 'EB_PROCESS', false, 'EB_SOA_REJECT_REASON', 20),
       ('EB_SOA', 'VALIDATED', 'release', 'RELEASED', 'Release', 'EB_PROCESS', false, null, 10);

-- ---------- Templates (design 8.4; placeholders until BDOI supplies the layouts, EBQ21) ---------
-- Without the portal (Drop 2) the insurer and the client answer by e-mail.
insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('EB_RENEWAL_ADVICE', 1, 'Renewal advice - {{programmeName}} ({{policyYear}})',
 'Dear {{contactName}},

Your employee benefits programme {{programmeName}} ({{lines}}) expires on {{expiryDate}}. To prepare the renewal, please send us your updated master list, the utilization report and any change you would like in the benefits.

Programme: {{programmeNo}}
Current insurer(s): {{insurers}}
Account officer: {{aoName}}

Thank you,
{{aoName}}
BDO Insurance and Reinsurance Brokers, Inc. - Employee Benefits', date '2020-01-01', now(), 'SYSTEM'),
('EB_RA_REMINDER', 1, 'Reminder: renewal of {{programmeName}}',
 'Dear {{contactName}},

This is a reminder that {{programmeName}} expires on {{expiryDate}}. We have not yet received your renewal requirements. Please reply or send them by reply e-mail.

Thank you,
{{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('EB_INDICATIVE_PROPOSAL', 1, 'Indicative renewal terms - {{programmeName}}',
 'Dear {{contactName}},

Please find attached the indicative renewal terms of {{incumbentName}} for {{programmeName}}, policy year {{policyYear}}.

{{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('EB_TOR', 1, 'Terms of Reference - {{programmeName}} ({{policyYear}})',
 'Terms of Reference, version {{torVersion}}

Client: {{clientName}}
Programme: {{programmeNo}}
Benefit lines: {{lines}}
Target inception: {{inceptionDate}}

{{torItems}}', date '2020-01-01', now(), 'SYSTEM'),
('EB_RFP_COVER', 1, 'Request for proposal {{requestNo}} - {{clientName}}',
 'Dear {{insurerName}},

We invite you to submit a proposal for the employee benefits of {{clientName}} ({{lines}}) according to the attached Terms of Reference. Please send your proposal by e-mail by {{dueDate}}.

{{aoName}}
BDO Insurance and Reinsurance Brokers, Inc.', date '2020-01-01', now(), 'SYSTEM'),
('EB_FRANCHISE_REQUEST', 1, 'Franchise request {{franchiseNo}} - {{clientName}}',
 'Dear {{insurerName}},

We request the franchise to market the employee benefits of {{clientName}} ({{lines}}). The supporting documents are attached. Please send us your decision by {{dueDate}}.

{{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('EB_FRANCHISE_ADVICE', 1, 'Franchise outcome - {{programmeName}}',
 'Dear {{contactName}},

{{insurerName}} has {{outcome}} our franchise request for {{programmeName}}.{{reasonText}}

{{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('EB_COMPARATIVE', 1, 'Comparative analysis - {{programmeName}} ({{policyYear}})',
 'Dear {{contactName}},

Please find attached the comparative analysis {{comparativeNo}} of the proposals received for {{programmeName}}, with our recommendation. Please send us your comments or your confirmation of the chosen proposals.

{{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('EB_REVISION_RELAY', 1, 'Revision request - {{clientName}}',
 'Dear {{insurerName}},

The client asks the following changes to your proposal {{proposalNo}}:

{{revisionItems}}

Please send your revised proposal by {{dueDate}}.

{{aoName}}', date '2020-01-01', now(), 'SYSTEM'),
('EB_ITEM_FOLLOWUP', 1, 'Follow-up: {{itemType}} for {{programmeName}}',
 'Dear {{recipientName}},

We are still waiting for the {{itemType}} of {{subject}} ({{programmeName}}), due on {{dueDate}}. This is follow-up number {{followUpNo}}.

Thank you,
{{aoName}}', date '2020-01-01', now(), 'SYSTEM');
