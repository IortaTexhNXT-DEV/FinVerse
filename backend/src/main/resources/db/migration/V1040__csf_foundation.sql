-- =====================================================================================
-- iNXT BrokerVerse - V1040 Customer Servicing Facility (BRD-9) foundation: the roles and grants
-- of the CSF permissions, permission action classes, lists of values, document types, business
-- parameters, exception codes and the retention rule of the contact changes.
--   Requirements: docs/requirements/BDOI_CSF_BRD_SPEC.md; FRS BRD-9 v1.0 sections 3, 5 and 9.
--     BRCSF-001        roles and grants (proposal until CSQ10)
--     BRCSF-004        verification checks, change reasons (CSQ03)
--     BRCSF-005 / EM07 CSF status mapping (CSQ04)
--     BRCSF-007        document types of the CSF uploads (CSQ07)
--     BRCSF-010, 011   retention of the contact changes (5 years online, 15 in total)
--   Design: docs/architecture/CUSTOMER_SERVICING_DESIGN.md sections 3, 6 and 7. Every default
--   value is a placeholder until the CSQ questions are answered; all of it is configuration.
--   Runs after V750 (LOV), V755 (sec_permission_action), V790 (retention rules) and V1031
--   (document access classes, which already name CSF_VIEW); references platform tables only.
-- =====================================================================================

-- ---------- Roles (design 6.2) -----------------------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('CSF_AGENT', 'Contact Center Agent', now(), 'SYSTEM'),
       ('CSF_SUPERVISOR', 'Contact Center Supervisor', now(), 'SYSTEM'),
       ('CSF_MANAGEMENT', 'Contact Center Management', now(), 'SYSTEM')
on conflict (code) do nothing;

-- Grants (FRS BRD-9 section 3.3). REPORT_VIEW opens the Report Centre, AUDIT_VIEW the audit trail
-- (CTL-AUDIT), ATTACHMENT_VIEW the documents under their access classes.
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    ('CSF_AGENT', 'CSF_VIEW'), ('CSF_AGENT', 'CSF_CONTACT_UPDATE'), ('CSF_AGENT', 'CSF_RESEND'),
    ('CSF_AGENT', 'CSF_DOCUMENT_UPLOAD'), ('CSF_AGENT', 'ATTACHMENT_VIEW'),
    ('CSF_SUPERVISOR', 'CSF_VIEW'), ('CSF_SUPERVISOR', 'CSF_CONTACT_UPDATE'), ('CSF_SUPERVISOR', 'CSF_RESEND'),
    ('CSF_SUPERVISOR', 'CSF_RESEND_OTHER'), ('CSF_SUPERVISOR', 'CSF_DOCUMENT_UPLOAD'),
    ('CSF_SUPERVISOR', 'CSF_REPORT_VIEW'), ('CSF_SUPERVISOR', 'ATTACHMENT_VIEW'), ('CSF_SUPERVISOR', 'REPORT_VIEW'),
    ('CSF_MANAGEMENT', 'CSF_VIEW'), ('CSF_MANAGEMENT', 'CSF_REPORT_VIEW'), ('CSF_MANAGEMENT', 'AUDIT_VIEW'),
    ('CSF_MANAGEMENT', 'REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- ---------- Permission action classes (User Access Matrix) -------------------------------------
insert into sec_permission_action (permission, area, action)
values ('CSF_VIEW', 'CUSTOMER_SERVICE', 'VIEW'),
       ('CSF_CONTACT_UPDATE', 'CUSTOMER_SERVICE', 'AMEND'),
       ('CSF_RESEND', 'CUSTOMER_SERVICE', 'AMEND'),
       ('CSF_RESEND_OTHER', 'CUSTOMER_SERVICE', 'APPROVE'),
       ('CSF_DOCUMENT_UPLOAD', 'CUSTOMER_SERVICE', 'CREATE'),
       ('CSF_REPORT_VIEW', 'CUSTOMER_SERVICE', 'VIEW')
on conflict (permission, action) do nothing;

-- ---------- Lists of values (design 7; FRS section 9.2) ----------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('CSF_STATUS', 'Customer service status', 'Status of an account shown to the contact centre', true, now(), 'SYSTEM'),
       ('CSF_STATUS_MAP', 'Customer service status mapping',
        'Which customer service status an account shows: the code is the account stage (ANY for every stage), optionally followed by _OUTSTANDING (premium not fully paid) or _EXPIRED (policy period ended); the group above is the status; rows are read in their order and the first match applies',
        true, now(), 'SYSTEM'),
       ('CSF_DOCUMENT_TYPE', 'Contact centre document type', 'Document types the contact centre may upload', true, now(), 'SYSTEM'),
       ('CSF_VERIFY_CHECK', 'Caller verification check', 'Questions asked to verify a caller before a change', true, now(), 'SYSTEM'),
       ('CSF_CHANGE_REASON', 'Contact change reason', 'Why the contact details of a client are changed', true, now(), 'SYSTEM'),
       ('CSF_CHANNEL', 'Contact channel', 'How the client contacted the contact centre', true, now(), 'SYSTEM'),
       ('CSF_REFERRAL_FIELD', 'Change referred to the fulfilment unit', 'Client information the contact centre cannot change itself', true, now(), 'SYSTEM')
on conflict (code) do nothing;

-- Platform document types of the contact centre uploads (BRCSF-007).
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'DOCUMENT_TYPE', v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('CLIENT_REQUEST', 'Client request or letter', 150),
    ('PROOF_OF_ADDRESS', 'Proof of address', 151),
    ('CLIENT_PHOTO', 'Photo sent by the client', 152)
) as v(code, label, sort_order)
on conflict (type_code, code) do nothing;

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, v.parent_code, date '2020-01-01', 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from (values
    ('CSF_STATUS', 'PENDING', 'Pending', 10, null),
    ('CSF_STATUS', 'AWAITING', 'Awaiting', 20, null),
    ('CSF_STATUS', 'BOOKED', 'Booked', 30, null),
    ('CSF_STATUS', 'OPEN', 'Open', 40, null),
    ('CSF_STATUS', 'CLOSED', 'Closed', 50, null),
    -- Status mapping (FRS section 5.1), read in this order; the first matching row applies.
    ('CSF_STATUS_MAP', 'CANCELLED', 'Cancelled after issuance', 10, 'CLOSED'),
    ('CSF_STATUS_MAP', 'VOIDED', 'Voided while in process', 20, 'CLOSED'),
    ('CSF_STATUS_MAP', 'PLACEMENT_CANCELLED', 'Placement cancelled', 30, 'CLOSED'),
    ('CSF_STATUS_MAP', 'BOOKED_EXPIRED', 'Booked, policy period ended', 40, 'CLOSED'),
    ('CSF_STATUS_MAP', 'BOOKED_OUTSTANDING', 'Booked, premium outstanding', 50, 'BOOKED'),
    ('CSF_STATUS_MAP', 'BOOKED', 'Booked and in force', 60, 'OPEN'),
    ('CSF_STATUS_MAP', 'DRAFT', 'Being prepared by Marketing', 70, 'PENDING'),
    ('CSF_STATUS_MAP', 'SUBMITTED', 'Submitted to Processing', 80, 'PENDING'),
    ('CSF_STATUS_MAP', 'RETURNED_TO_MARKETING', 'Returned to Marketing', 90, 'PENDING'),
    ('CSF_STATUS_MAP', 'AWAITING_PAYMENT', 'Awaiting payment or client confirmation', 100, 'AWAITING'),
    ('CSF_STATUS_MAP', 'READY_FOR_PLACEMENT', 'Ready for placement', 110, 'AWAITING'),
    ('CSF_STATUS_MAP', 'PLACED', 'Placed with the insurer', 120, 'AWAITING'),
    ('CSF_STATUS_MAP', 'RETURNED_BY_INSURER', 'Returned by the insurer', 130, 'AWAITING'),
    ('CSF_STATUS_MAP', 'POLICY_ISSUED', 'Policy issued, not yet booked', 140, 'AWAITING'),
    ('CSF_DOCUMENT_TYPE', 'CLIENT_REQUEST', 'Client request or letter', 10, null),
    ('CSF_DOCUMENT_TYPE', 'VALID_ID', 'Valid ID', 20, null),
    ('CSF_DOCUMENT_TYPE', 'PROOF_OF_ADDRESS', 'Proof of address', 30, null),
    ('CSF_DOCUMENT_TYPE', 'CLIENT_PHOTO', 'Photo sent by the client', 40, null),
    ('CSF_DOCUMENT_TYPE', 'OFFICIAL_RECEIPT', 'Official receipt', 50, null),
    ('CSF_DOCUMENT_TYPE', 'POLICY_COPY', 'Policy copy', 60, null),
    ('CSF_DOCUMENT_TYPE', 'OTHERS', 'Others', 90, null),
    ('CSF_VERIFY_CHECK', 'ADDRESS', 'Address', 10, null),
    ('CSF_VERIFY_CHECK', 'CONTACT_NUMBER', 'Contact number', 20, null),
    ('CSF_VERIFY_CHECK', 'EMAIL', 'E-mail address', 30, null),
    ('CSF_VERIFY_CHECK', 'INSURED_PROPERTY', 'Insured property details', 40, null),
    ('CSF_CHANGE_REASON', 'CLIENT_REQUEST', 'Client request', 10, null),
    ('CSF_CHANGE_REASON', 'CORRECTION', 'Correction of wrong details', 20, null),
    ('CSF_CHANGE_REASON', 'RETURNED_MAIL', 'Returned mail or bounced e-mail', 30, null),
    ('CSF_CHANGE_REASON', 'OTHERS', 'Others', 90, null),
    ('CSF_CHANNEL', 'HOTLINE', 'Hotline', 10, null),
    ('CSF_CHANNEL', 'EMAIL', 'E-mail', 20, null),
    ('CSF_CHANNEL', 'WEBSITE', 'Website', 30, null),
    ('CSF_REFERRAL_FIELD', 'NAME', 'Name', 10, null),
    ('CSF_REFERRAL_FIELD', 'CIVIL_STATUS', 'Civil status', 20, null),
    ('CSF_REFERRAL_FIELD', 'BIRTH_DATE', 'Birth date', 30, null),
    ('CSF_REFERRAL_FIELD', 'ID_DOCUMENT', 'ID document', 40, null),
    ('CSF_REFERRAL_FIELD', 'TIN', 'TIN', 50, null),
    ('CSF_REFERRAL_FIELD', 'OTHERS', 'Other client information', 90, null)
) as v(type_code, code, label, sort_order, parent_code)
on conflict (type_code, code) do nothing;

-- ---------- Business parameters (design 7; FRS section 9.1) ------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('CSF_PAYMENT_HISTORY_MONTHS', '12', 'INTEGER', 'CUSTOMER_SERVICE',
     'Months of payment history the contact centre sees before Show Older', 1, 120, now(), 'SYSTEM'),
    ('CSF_VERIFY_MIN_MATCHES', '2', 'INTEGER', 'CUSTOMER_SERVICE',
     'Verification checks that must match before the contact details of a client can be changed', 1, 10, now(), 'SYSTEM'),
    ('CSF_VERIFY_MAX_FAILS', '3', 'INTEGER', 'CUSTOMER_SERVICE',
     'Failed verifications of one client in a day that raise an alert to the supervisors', 1, 20, now(), 'SYSTEM'),
    ('CSF_VERIFICATION_VALID_MINUTES', '30', 'INTEGER', 'CUSTOMER_SERVICE',
     'Minutes a passed verification of a caller stays valid for a change', 1, 480, now(), 'SYSTEM'),
    ('CSF_LEGACY_SYNC_ENABLED', 'false', 'BOOLEAN', 'CUSTOMER_SERVICE',
     'Send contact changes to the legacy policy systems while they are still in use', null, null, now(), 'SYSTEM'),
    ('CSF_SEARCH_MIN_CHARS', '3', 'INTEGER', 'CUSTOMER_SERVICE',
     'Minimum characters of a client name search', 1, 20, now(), 'SYSTEM'),
    ('CSF_SEARCH_MAX_RESULTS', '50', 'INTEGER', 'CUSTOMER_SERVICE',
     'Maximum clients a customer search returns before the agent must refine it', 5, 500, now(), 'SYSTEM')
on conflict (param_key) do nothing;

-- ---------- Exception codes (design 7) ---------------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('CSF_VERIFICATION_FAILED_REPEAT', 'Repeated failed caller verification',
     'Callers failed the verification of the same client several times in one day; check whether someone is trying to change the client''s contact details.',
     'CUSTOMER_SERVICE', 'HIGH', null, null, now(), 'SYSTEM'),
    ('CSF_SYNC_FAILED', 'Contact change not sent to the legacy system',
     'A client contact change could not be sent to a legacy policy system; it stays queued and is sent again by the next run.',
     'CUSTOMER_SERVICE', 'MEDIUM', null, null, now(), 'SYSTEM')
on conflict (code) do nothing;

-- ---------- Retention (FRS FR-CSF-040 R2: 5 years online, 15 years in total) -------------------
insert into nba_retention_rule (record_type, statuses, years_online, years_archive, action, active,
                                description, created_at, created_by)
select 'CSF_CONTACT_CHANGE', 'APPLIED,REFUSED,REFERRED', 5, 10, 'REVIEW', true,
       'Contact changes of clients with their verification, sync status and referrals: 5 years online, 15 years in total',
       now(), 'SYSTEM'
where not exists (select 1 from nba_retention_rule where record_type = 'CSF_CONTACT_CHANGE');
