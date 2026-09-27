-- =====================================================================================
-- iNXT BrokerVerse - V1960 Seed data of User Access Maintenance (BRD-11, wave U1-A).
-- SEED DATA ONLY - never load in production. Password of the SIT/UAT users: Brokerverse@2026.
--   Users (design 6.2):   requestor (UAM_REQUESTOR), uamapprover (UAM_APPROVER),
--                         secapprover (UAM_SECOND_APPROVER); subject users a013000101-104.
--   Requests (TD-UA-02):  a draft, a request pending for uamapprover, a returned request, a
--                         cancelled request, a scheduled request (future date), an approved and
--                         applied enrolment (with its change-log rows), a bulk batch of three
--                         lines, a CREATE_ROLE request FOR_IMPLEMENTATION for admin, and a
--                         privileged change waiting for the second approval.
--   Working hours:        the seed and the automated tests run around the clock, so
--                         UAM_WORKING_HOURS is opened to the whole week here; the out-of-hours
--                         flag is shown by setting the delivered value 08:00-18:00,MON-FRI
--                         (TD-UA-06) on Administration > System Parameters.
-- =====================================================================================

-- ---------- Users -----------------------------------------------------------------------------
insert into sec_user (username, full_name, email, password_hash, home_branch_id, created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2',
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('requestor',   'Rhea Access Requestor',   'requestor@brokerverse-seed.ph'),
             ('uamapprover', 'Ulysses Access Approver', 'uamapprover@brokerverse-seed.ph'),
             ('secapprover', 'Selina Second Approver',  'secapprover@brokerverse-seed.ph'),
             ('a013000101',  'SIT Enrolled User',      'a013000101@brokerverse-seed.ph'),
             ('a013000102',  'SIT Leaver',             'a013000102@brokerverse-seed.ph'),
             ('a013000103',  'SIT Transferee',         'a013000103@brokerverse-seed.ph'),
             ('a013000104',  'SIT Team Lead',          'a013000104@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = lower(u.username));

insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u join sec_role r on r.code = case u.username
    when 'requestor' then 'UAM_REQUESTOR' when 'uamapprover' then 'UAM_APPROVER'
    when 'secapprover' then 'UAM_SECOND_APPROVER' else 'MKT_AO' end
where u.username in ('requestor', 'uamapprover', 'secapprover', 'a013000101', 'a013000102',
                     'a013000103', 'a013000104')
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

update sys_parameter set param_value = '00:00-24:00,MON-SUN', updated_at = now(), updated_by = 'SYSTEM'
where param_key = 'UAM_WORKING_HOURS';

-- ---------- Bulk batch ------------------------------------------------------------------------
insert into nba_access_request_batch (batch_no, file_name, lines, status, remarks, created_at, created_by)
values ('BLK-2026-900001', 'uam-bulk-seed.xlsx', 3, 'PENDING', 'Three new Marketing account officers',
        now() - interval '1 day', 'requestor');

-- ---------- Requests in every status ----------------------------------------------------------
insert into nba_access_request (request_no, request_type, user_type, username, full_name, email, role_codes,
    justification, status, assigned_approver, effective_from, batch_id, role_code, permissions_added,
    role_name, role_description, privilege_level, submitted_by, submitted_at, decided_by, decided_at,
    decision_comment, returned_count, second_approval_required, risk_flags, cancel_reason, cancelled_by,
    cancelled_at, applied_at, reason_code, created_at, created_by)
values
    -- Draft (visible to requestor only)
    ('AR-2026-900001', 'CREATE_USER', 'INTERNAL', 'a013000190', 'SIT Draft User', null, 'MKT_AO',
     'New Marketing account officer', 'DRAFT', null, null, null, null, null, null, null, null,
     null, null, null, null, null, 0, false, null, null, null, null, null, null,
     now() - interval '2 day', 'requestor'),
    -- Pending for uamapprover
    ('AR-2026-900002', 'MODIFY_USER', 'INTERNAL', 'a013000101', null, null, 'MKT_AO,TSU',
     'Covers the TSU desk during the peak season', 'PENDING', 'uamapprover', null, null, null, null,
     null, null, null, 'requestor', now() - interval '1 day', null, null, null, 0, false, null,
     null, null, null, null, null, now() - interval '1 day', 'requestor'),
    -- Returned with remarks
    ('AR-2026-900003', 'DISABLE_USER', 'INTERNAL', 'a013000102', null, null, null,
     'Resigned', 'RETURNED', null, null, null, null, null, null, null, null, 'requestor',
     now() - interval '3 day', 'uamapprover', now() - interval '2 day',
     'Attach the clearance form number in the remarks', 1, false, null, null, null, null, null,
     'RESIGNED', now() - interval '3 day', 'requestor'),
    -- Cancelled
    ('AR-2026-900004', 'CREATE_USER', 'INTERNAL', 'a013000191', 'SIT Cancelled Hire', null, 'MKT_AO',
     'New hire', 'CANCELLED', null, null, null, null, null, null, null, null, 'requestor',
     now() - interval '5 day', null, null, null, 0, false, null, 'The hire was withdrawn', 'requestor',
     now() - interval '4 day', null, null, now() - interval '5 day', 'requestor'),
    -- Scheduled (future effective date)
    ('AR-2026-900005', 'DISABLE_USER', 'INTERNAL', 'a013000103', null, null, null,
     'Transfers to BDO Unibank at month end', 'SCHEDULED', null, current_date + 30, null, null, null,
     null, null, null, 'requestor', now() - interval '2 day', 'uamapprover', now() - interval '1 day',
     null, 0, false, null, null, null, null, null, 'TRANSFERRED', now() - interval '2 day', 'requestor'),
    -- Approved and applied enrolment
    ('AR-2026-900006', 'CREATE_USER', 'INTERNAL', 'a013000101', 'SIT Enrolled User',
     'a013000101@brokerverse-seed.ph', 'MKT_AO', 'Joined Marketing', 'APPROVED', null, null, null,
     null, null, null, null, null, 'requestor', now() - interval '10 day', 'uamapprover',
     now() - interval '9 day', 'Welcome', 0, false, null, null, null, null, now() - interval '9 day',
     null, now() - interval '10 day', 'requestor'),
    -- Group profile request waiting for the System Administrator
    ('AR-2026-900007', 'CREATE_ROLE', 'INTERNAL', null, null, null, null,
     'Second processing team lead profile for the Makati branch', 'FOR_IMPLEMENTATION', null, null,
     null, 'PROCESSING_TL2', 'PRODUCT_VIEW,REPORT_VIEW', 'Processing Team Lead 2',
     'Processing team lead of the Makati branch', 'STANDARD', 'badmin', now() - interval '3 day',
     'uamapprover', now() - interval '2 day', null, 0, false, null, null, null, null, null, null,
     now() - interval '3 day', 'badmin'),
    -- Privileged change waiting for the second approval
    ('AR-2026-900008', 'MODIFY_USER', 'INTERNAL', 'a013000104', null, null, 'MKT_AO,SYSADMIN',
     'Back-up system administrator during the migration', 'PENDING_SECOND', null, null, null, null,
     null, null, null, null, 'requestor', now() - interval '1 day', 'uamapprover',
     now() - interval '12 hour', null, 0, true, 'PRIVILEGE_INCREASE', null, null, null, null, null,
     now() - interval '1 day', 'requestor');

-- Bulk lines (pending for uamapprover)
insert into nba_access_request (request_no, request_type, user_type, username, full_name, role_codes,
    justification, status, assigned_approver, batch_id, submitted_by, submitted_at, created_at, created_by)
select 'AR-2026-90001' || n, 'CREATE_USER', 'INTERNAL', 'a01300021' || n, 'SIT Bulk Officer ' || n,
       'MKT_AO', 'Joined Marketing', 'PENDING', 'uamapprover',
       (select id from nba_access_request_batch where batch_no = 'BLK-2026-900001'),
       'requestor', now() - interval '1 day', now() - interval '1 day', 'requestor'
from generate_series(1, 3) as n;

-- ---------- Approvers and history -------------------------------------------------------------
insert into nba_access_request_approver (request_id, sequence, approver, decision, remarks, decided_at,
    created_at, created_by)
select r.id, 1, 'uamapprover',
       case r.status when 'PENDING' then 'PENDING' when 'RETURNED' then 'RETURNED' else 'APPROVED' end,
       r.decision_comment, r.decided_at, r.created_at, r.created_by
from nba_access_request r
where r.request_no like 'AR-2026-9%' and r.status not in ('DRAFT', 'CANCELLED');

insert into nba_access_request_event (request_id, action, from_status, to_status, remarks, actor, occurred_at)
select r.id, 'SAVE', null, 'DRAFT', r.justification, r.created_by, r.created_at
from nba_access_request r where r.request_no like 'AR-2026-9%';

insert into nba_access_request_event (request_id, action, from_status, to_status, remarks, actor, occurred_at)
select r.id, 'SUBMIT', 'DRAFT', 'PENDING', r.justification, r.submitted_by, r.submitted_at
from nba_access_request r where r.request_no like 'AR-2026-9%' and r.submitted_at is not null;

insert into nba_access_request_event (request_id, action, from_status, to_status, remarks, actor, occurred_at)
select r.id, case r.status when 'RETURNED' then 'RETURN' else 'APPROVE' end, 'PENDING', r.status,
       r.decision_comment, r.decided_by, r.decided_at
from nba_access_request r
where r.request_no like 'AR-2026-9%' and r.decided_at is not null;

insert into nba_access_request_event (request_id, action, from_status, to_status, remarks, actor, occurred_at)
select r.id, 'CANCEL', 'PENDING', 'CANCELLED', r.cancel_reason, r.cancelled_by, r.cancelled_at
from nba_access_request r where r.request_no = 'AR-2026-900004';

insert into nba_access_request_event (request_id, action, from_status, to_status, remarks, actor, occurred_at)
select r.id, 'APPLY', 'APPROVED', 'APPROVED', null, r.decided_by, r.applied_at
from nba_access_request r where r.request_no = 'AR-2026-900006';

-- ---------- Access change log of the applied enrolment (BRD 4.003.1) ---------------------------
insert into sec_access_change_log (occurred_at, subject_type, subject, activity, attribute, from_value,
    to_value, request_no, done_by, approved_by)
select now() - interval '9 day', 'USER', 'a013000101', 'CREATE_USER', a.attribute, null, a.value,
       'AR-2026-900006', 'uamapprover', 'uamapprover'
from (values ('fullName', 'SIT Enrolled User'), ('email', 'a013000101@brokerverse-seed.ph'),
             ('roles', 'MKT_AO'), ('enabled', 'true')) as a(attribute, value);
