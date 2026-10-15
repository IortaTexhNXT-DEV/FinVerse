-- User Access personas under the UIDM-ISC model. UIDM-ISC orchestrates the users and their
-- group-profile membership; this system keeps the definitions of the group profiles (maker-checker),
-- data scope, unlock and session end, separation of duties, reports, audit and reconciliation.
--
--   1. Open requests about users (enrol, modify, deactivate, reactivate) are cancelled with the
--      reason in their history: users are maintained in UIDM-ISC. The decided ones stay as history.
--   2. Open group-profile changes that name a permission withdrawn below are cancelled with the
--      reason in their history (the applier also skips an unknown permission, for safety).
--   3. The request functions for users (UAM_ENROLL, UAM_MODIFY, UAM_DEACTIVATE, UAM_REACTIVATE,
--      UAM_CORRECT) are revoked from every group profile and withdrawn. The bulk upload right goes
--      from the User Access maker. The audit trail and the access change log keep the old codes.
--   4. Personas: User Access Administrator (new), Group Profile Maker (was User Access Requestor),
--      Group Profile Checker (was User Access Approver), Information Security and Auditor.

-- ---------- 1. and 2. Open requests -------------------------------------------------------------
create temporary table uam_withdrawn_permission (permission varchar(60) primary key) on commit drop;
insert into uam_withdrawn_permission (permission)
values ('UAM_ENROLL'), ('UAM_MODIFY'), ('UAM_DEACTIVATE'), ('UAM_REACTIVATE'), ('UAM_CORRECT');

create temporary table uam_cancelled_request on commit drop as
select r.id, r.status,
       case when r.request_type in ('CREATE_USER', 'MODIFY_ROLES', 'MODIFY_USER', 'DISABLE_USER', 'ENABLE_USER')
            then 'Users are maintained in UIDM-ISC'
            else 'The change names a permission that was withdrawn' end as reason
from nba_access_request r
where r.status in ('DRAFT', 'PENDING', 'PENDING_SECOND', 'RETURNED', 'SCHEDULED', 'FOR_IMPLEMENTATION')
  and (r.request_type in ('CREATE_USER', 'MODIFY_ROLES', 'MODIFY_USER', 'DISABLE_USER', 'ENABLE_USER')
       or exists (select 1 from uam_withdrawn_permission w
                  where w.permission = any (string_to_array(coalesce(r.permissions_added, ''), ','))
                     or w.permission = any (string_to_array(coalesce(r.permissions_removed, ''), ','))));

update nba_access_request r
set status = 'CANCELLED',
    cancel_reason = c.reason,
    cancelled_by = 'SYSTEM',
    cancelled_at = now()
from uam_cancelled_request c
where r.id = c.id;

insert into nba_access_request_event (request_id, action, from_status, to_status, remarks, actor, occurred_at)
select c.id, 'CANCEL', c.status, 'CANCELLED', c.reason, 'SYSTEM', now()
from uam_cancelled_request c;

-- ---------- 3. Withdrawn request functions ---------------------------------------------------------
delete from sec_role_permission where permission in (select permission from uam_withdrawn_permission);
delete from sec_permission_action where permission in (select permission from uam_withdrawn_permission);

delete from sec_role_permission p
using sec_role r
where p.role_id = r.id and r.code = 'UAM_REQUESTOR' and p.permission in ('BULK_PROCESS', 'ACCESS_REQUEST');

-- ---------- 4. Personas ------------------------------------------------------------------------------
update sec_role set name = 'Group Profile Maker',
    description = 'Proposes new group profiles and changes to their definition, permissions and status',
    updated_at = now(), updated_by = 'SYSTEM'
where code = 'UAM_REQUESTOR';

update sec_role set name = 'Group Profile Checker',
    description = 'Approves or rejects the group-profile changes proposed by another user',
    updated_at = now(), updated_by = 'SYSTEM'
where code = 'UAM_APPROVER';

update sec_role set name = 'Information Security',
    updated_at = now(), updated_by = 'SYSTEM'
where code = 'INFOSEC_OFFICER';

insert into sec_role (code, name, description, privilege_level, created_at, created_by)
select 'UAM_ADMINISTRATOR', 'User Access Administrator',
       'Looks after the users from UIDM-ISC: data access, unlock, sessions, identity synchronisation and reconciliation',
       'HIGH', now(), 'SYSTEM'
where not exists (select 1 from sec_role where code = 'UAM_ADMINISTRATOR');

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values ('UAM_REQUESTOR', 'UAM_GROUP_REQUEST'),
             ('UAM_REQUESTOR', 'UAM_REPORT_VIEW'),
             ('UAM_ADMINISTRATOR', 'USER_MANAGE'),
             ('UAM_ADMINISTRATOR', 'UAM_VIEW'),
             ('UAM_ADMINISTRATOR', 'UAM_REPORT_VIEW'),
             ('UAM_ADMINISTRATOR', 'AUDIT_VIEW'),
             ('UAM_ADMINISTRATOR', 'REPORT_VIEW'),
             ('INFOSEC_OFFICER', 'UAM_VIEW')) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);
