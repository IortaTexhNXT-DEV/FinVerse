-- =====================================================================================
-- iNXT BrokerVerse - V1060 User Access Maintenance (BRD-11) foundation: roles and grants,
-- permission action classes (area USER_ACCESS, plus the platform administration permissions),
-- business parameters, lists of values, exception codes and notification events.
--   Requirements: docs/requirements/BDOI_UAM_BRD_SPEC.md
--     BRD 4.002.2.1-10   the ten request functions as permissions (UAM_*; ACCESS_APPROVE kept)
--     BRD 1.002.1.1.1    business unit and user level of a user (LOVs, values from BDOI, UQ05)
--     BRD 1.005          deactivation reasons (UAM_DEACTIVATION_REASON, to confirm)
--     BRD 3.003.2.2      a module (area) for the tasks of the group-profile report
--     UAM-NFR-11/17/33   directory sign-in as a parked port (AUTH_MODE; decision D6)
--     UAM-NFR-13         user ID format (USER_ID_PATTERN, UQ05)
--     UAM-NFR-14         effective-dated changes; UAM-NFR-19 lockout for all users (CQ23, D5)
--     UAM-NFR-25         batch failure e-mail (JOB_FAILURE_RECIPIENTS)
--     UAM-NFR-36         password history and age (UQ08)
--     UAM-NFR-40         second approval of privileged / out-of-hours changes (UQ07)
--     PQ17               role edits only through an implemented request; audited emergency path
--   Design: docs/architecture/USER_ACCESS_DESIGN.md sections 3, 6 and 8.
--   Behaviour switches keep today's behaviour until BDOI answers (and until wave U1-A delivers
--   the implementation flow): UAM_DIRECT_ROLE_EDIT = true (the Roles screen still edits directly,
--   every direct edit is audited and raises UAM_DIRECT_ROLE_EDIT) and UAM_ROLE_APPLY_ON_APPROVAL =
--   true (approved role-permission requests apply at once, as PMADD05 does today; UQ03).
--   The schema extensions are V1061; the request lifecycle is V1062 (U1-A), SIT/UAT users V1960.
--   Runs after V790 / V791 (nba_access_request), V755 (sec_permission_action) and V1000
--   (LOGIN_MAX_FAILED_ATTEMPTS) on a fresh database.
-- =====================================================================================

-- ---------- User Access roles (design 6.2) ----------------------------------------------------
insert into sec_role (code, name, created_at, created_by)
values ('UAM_REQUESTOR', 'User Access Requestor', now(), 'SYSTEM'),
       ('UAM_APPROVER', 'User Access Approver', now(), 'SYSTEM'),
       ('UAM_SECOND_APPROVER', 'User Access Second Approver', now(), 'SYSTEM');

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    -- Requestor: the user request functions (BRD 4.002.2.1-7)
    ('UAM_REQUESTOR', 'UAM_ENROLL'), ('UAM_REQUESTOR', 'UAM_MODIFY'), ('UAM_REQUESTOR', 'UAM_DEACTIVATE'),
    ('UAM_REQUESTOR', 'UAM_REACTIVATE'), ('UAM_REQUESTOR', 'UAM_CORRECT'), ('UAM_REQUESTOR', 'UAM_CANCEL'),
    ('UAM_REQUESTOR', 'UAM_VIEW'),
    -- Approver (BRD 4.002.2.8, 10)
    ('UAM_APPROVER', 'ACCESS_APPROVE'), ('UAM_APPROVER', 'UAM_VIEW'), ('UAM_APPROVER', 'UAM_REPORT_VIEW'),
    -- Second approver of risky changes (UAM-NFR-40, UQ07)
    ('UAM_SECOND_APPROVER', 'UAM_SECOND_APPROVE'), ('UAM_SECOND_APPROVER', 'UAM_VIEW'),
    -- Business Administrator: group-profile requests, corrections, cancellations, reports
    ('BUSINESS_ADMIN', 'UAM_GROUP_REQUEST'), ('BUSINESS_ADMIN', 'UAM_VIEW'),
    ('BUSINESS_ADMIN', 'UAM_REPORT_VIEW'), ('BUSINESS_ADMIN', 'UAM_CORRECT'), ('BUSINESS_ADMIN', 'UAM_CANCEL'),
    -- System Administrator (implements approved group-profile requests with ROLE_MANAGE) and auditor
    ('SYSADMIN', 'UAM_VIEW'), ('SYSADMIN', 'UAM_REPORT_VIEW'),
    ('AUDITOR', 'UAM_VIEW'), ('AUDITOR', 'UAM_REPORT_VIEW')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

-- Compatibility (design 1.6): every role that raises requests today (ACCESS_REQUEST) keeps every
-- request function, and every role that approves them (ACCESS_APPROVE) may view them.
insert into sec_role_permission (role_id, permission)
select distinct holder.role_id, p.permission
from sec_role_permission holder
cross join (values ('UAM_ENROLL'), ('UAM_MODIFY'), ('UAM_DEACTIVATE'), ('UAM_REACTIVATE'),
                   ('UAM_CORRECT'), ('UAM_CANCEL'), ('UAM_VIEW'), ('UAM_GROUP_REQUEST')) as p(permission)
where holder.permission = 'ACCESS_REQUEST'
  and not exists (select 1 from sec_role_permission x where x.role_id = holder.role_id and x.permission = p.permission);

insert into sec_role_permission (role_id, permission)
select distinct holder.role_id, 'UAM_VIEW'
from sec_role_permission holder
where holder.permission = 'ACCESS_APPROVE'
  and not exists (select 1 from sec_role_permission x where x.role_id = holder.role_id and x.permission = 'UAM_VIEW');

-- ---------- Permission action classes (design 6.1; BRD 3.003.2.2) -----------------------------
-- The access request permissions move from Broking Administration to User Access.
update sec_permission_action set area = 'USER_ACCESS'
where permission in ('ACCESS_REQUEST', 'ACCESS_APPROVE');

insert into sec_permission_action (permission, area, action)
values
    ('UAM_VIEW', 'USER_ACCESS', 'VIEW'),
    ('UAM_REPORT_VIEW', 'USER_ACCESS', 'VIEW'),
    ('UAM_ENROLL', 'USER_ACCESS', 'CREATE'),
    ('UAM_GROUP_REQUEST', 'USER_ACCESS', 'CREATE'),
    ('UAM_MODIFY', 'USER_ACCESS', 'AMEND'),
    ('UAM_DEACTIVATE', 'USER_ACCESS', 'AMEND'),
    ('UAM_REACTIVATE', 'USER_ACCESS', 'AMEND'),
    ('UAM_CORRECT', 'USER_ACCESS', 'AMEND'),
    ('UAM_CANCEL', 'USER_ACCESS', 'AMEND'),
    ('UAM_SECOND_APPROVE', 'USER_ACCESS', 'APPROVE'),
    -- Platform administration: user and role maintenance, parameters, audit, alerts, monitoring.
    -- The finance permissions (journals, periods, sub-ledgers, tax...) stay unclassified until the
    -- finance owner maps them; the group-profile report lists them under "Other".
    ('USER_MANAGE', 'USER_ACCESS', 'CREATE'),
    ('USER_MANAGE', 'USER_ACCESS', 'AMEND'),
    ('ROLE_MANAGE', 'USER_ACCESS', 'CREATE'),
    ('ROLE_MANAGE', 'USER_ACCESS', 'AMEND'),
    ('AUDIT_VIEW', 'ADMINISTRATION', 'VIEW'),
    ('SYSTEM_MONITOR', 'ADMINISTRATION', 'VIEW'),
    ('ALERT_VIEW', 'ADMINISTRATION', 'VIEW'),
    ('ALERT_MANAGE', 'ADMINISTRATION', 'AMEND'),
    ('SYSTEM_PARAMETER_MANAGE', 'ADMINISTRATION', 'AMEND');

-- ---------- Business parameters (design section 8) --------------------------------------------
insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('AUTH_MODE', 'LOCAL', 'STRING', 'SECURITY',
     'Sign-in mode: LOCAL (BrokerVerse passwords) or DIRECTORY (BDO EUA / AD by Windows ID, once the adapter exists) (UAM-NFR-11, 17; Q42, UQ04)', null, null, now(), 'SYSTEM'),
    ('USER_ID_PATTERN', '^[a-zA-Z][0-9]{9}$', 'STRING', 'SECURITY',
     'Regular expression of a new user ID on enrolment requests (UAM-NFR-13; to confirm, UQ05)', null, null, now(), 'SYSTEM'),
    ('PASSWORD_HISTORY_COUNT', '8', 'INTEGER', 'SECURITY',
     'Previous passwords a user may not reuse, LOCAL mode (UAM-NFR-36; to confirm, UQ08)', 0, 24, now(), 'SYSTEM'),
    ('PASSWORD_MAX_AGE_DAYS', '90', 'INTEGER', 'SECURITY',
     'Days after which a password must be changed, LOCAL mode; 0 = never (UAM-NFR-36; to confirm, UQ08)', 0, 365, now(), 'SYSTEM'),
    ('PASSWORD_MIN_AGE_DAYS', '1', 'INTEGER', 'SECURITY',
     'Days before a changed password may be changed again, LOCAL mode (UAM-NFR-36; to confirm, UQ08)', 0, 30, now(), 'SYSTEM'),
    ('UAM_WORKING_HOURS', '08:00-18:00,MON-FRI', 'STRING', 'SECURITY',
     'Working hours; an access request submitted outside them needs a second approval (UAM-NFR-40; to confirm, UQ07)', null, null, now(), 'SYSTEM'),
    ('UAM_ANY_APPROVER', 'false', 'BOOLEAN', 'SECURITY',
     'When true, any holder of ACCESS_APPROVE may decide a request, not only the approver chosen by the requester (UQ02)', null, null, now(), 'SYSTEM'),
    ('UAM_DIRECT_ROLE_EDIT', 'true', 'BOOLEAN', 'SECURITY',
     'Emergency path: when true, the System Administrator may create and edit roles directly on the Roles screen (audited, alert UAM_DIRECT_ROLE_EDIT); when false, only an approved group-profile request number allows it (PQ17). True until the implement-request flow is live', null, null, now(), 'SYSTEM'),
    ('UAM_ROLE_APPLY_ON_APPROVAL', 'true', 'BOOLEAN', 'SECURITY',
     'When true, an approved group-profile or role-permission request applies at once (today''s behaviour); when false it waits for the System Administrator to implement it (BRD-11 p.6; UQ03)', null, null, now(), 'SYSTEM'),
    ('JOB_FAILURE_RECIPIENTS', '', 'STRING', 'SECURITY',
     'Comma separated e-mail addresses told of every failed batch job (UAM-NFR-25)', null, null, now(), 'SYSTEM');

update sys_parameter
set description = 'Consecutive failed logins that lock a user account; applies to all users (BDOI NFR 1-2, UAM-NFR-19; CQ23 answered by BRD-11)',
    updated_at = now(), updated_by = 'SYSTEM'
where param_key = 'LOGIN_MAX_FAILED_ATTEMPTS';

-- ---------- Lists of values (design section 8) ------------------------------------------------
insert into lov_type (code, name, description, maintainable, created_at, created_by)
values ('UAM_BUSINESS_UNIT', 'Business unit group', 'Business unit group of a user (BRD 1.002.1.1.1; values from BDOI, UQ05)', true, now(), 'SYSTEM'),
       ('UAM_USER_LEVEL', 'User level', 'Level of a user in the organisation (BRD 1.002.1.1.1; values from BDOI, UQ05)', true, now(), 'SYSTEM'),
       ('UAM_DEACTIVATION_REASON', 'User deactivation reason', 'Why a user is deactivated (BRD 1.005)', true, now(), 'SYSTEM');

insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from (values
    ('UAM_DEACTIVATION_REASON', 'RESIGNED', 'Resigned (to confirm)', 10),
    ('UAM_DEACTIVATION_REASON', 'TRANSFERRED', 'Transferred to another unit (to confirm)', 20),
    ('UAM_DEACTIVATION_REASON', 'LONG_LEAVE', 'Long leave (to confirm)', 30),
    ('UAM_DEACTIVATION_REASON', 'SECURITY', 'Security reason (to confirm)', 40),
    ('UAM_DEACTIVATION_REASON', 'OTHERS', 'Others (see remarks)', 90)
) as v(type_code, code, label, sort_order);

-- ---------- Exception codes (design section 8) ------------------------------------------------
insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by) values
    ('UAM_PRIVILEGED_CHANGE', 'Privileged access change',
     'An access request raises a privilege level or was submitted outside working hours (UAM-NFR-40).',
     'USER_ACCESS', 'MEDIUM', null, null, now(), 'SYSTEM'),
    ('UAM_SCHEDULED_APPLY_FAILED', 'Scheduled access change failed',
     'An approved access request could not be applied on its effective date (UAM-NFR-14).',
     'USER_ACCESS', 'HIGH', null, null, now(), 'SYSTEM'),
    ('UAM_DIRECT_ROLE_EDIT', 'Role edited outside a request',
     'A role was created or changed directly on the Roles screen through the emergency path UAM_DIRECT_ROLE_EDIT (PQ17).',
     'USER_ACCESS', 'MEDIUM', null, null, now(), 'SYSTEM');

-- ---------- Notification events (design section 8) --------------------------------------------
insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order) values
    ('UAM_REQUEST_TO_APPROVE', 'Access request to approve', 'USER_ACCESS',
     'An access request waits for your approval (BRD 2.002)', true, true, 600),
    ('UAM_REQUEST_RETURNED', 'Access request returned', 'USER_ACCESS',
     'Your access request was returned with remarks (BRD 2.002.7)', true, true, 610),
    ('UAM_REQUEST_CANCELLED', 'Access request cancelled', 'USER_ACCESS',
     'An access request assigned to you was cancelled (BRD 1.007)', true, false, 620),
    ('UAM_REQUEST_DECIDED', 'Access request decided', 'USER_ACCESS',
     'Your access request was approved or rejected (BRD 2.002)', true, true, 630),
    ('UAM_ACCESS_CHANGED', 'Your access changed', 'USER_ACCESS',
     'Your user data or group profiles were changed (BRD 1.002-1.006)', true, true, 640),
    ('UAM_SECOND_APPROVAL', 'Access request for second approval', 'USER_ACCESS',
     'A privileged or out-of-hours access request waits for your second approval (UAM-NFR-40)', true, true, 650),
    ('UAM_FOR_IMPLEMENTATION', 'Group profile request to implement', 'USER_ACCESS',
     'An approved group-profile request waits for the System Administrator (BRD 3.002; UQ03)', true, true, 660);
