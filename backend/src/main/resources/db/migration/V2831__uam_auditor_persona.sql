-- User Access Auditor (read-only): the users and their group profiles, the group profiles, the
-- access matrix, the reports and certification extracts and the audit log, without any change.
-- USER_VIEW opens the Users screen and the user detail read-only (USER_MANAGE keeps unlock, end of
-- sessions, data access and the local test account password).
insert into sec_role (code, name, description, privilege_level, created_at, created_by)
select 'UAM_AUDITOR', 'User Access Auditor',
       'Reads the users, group profiles, access matrix, certification extracts and audit log of User Access',
       'STANDARD', now(), 'SYSTEM'
where not exists (select 1 from sec_role where code = 'UAM_AUDITOR');

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values ('UAM_AUDITOR', 'USER_VIEW'),
             ('UAM_AUDITOR', 'UAM_VIEW'),
             ('UAM_AUDITOR', 'UAM_REPORT_VIEW'),
             ('UAM_AUDITOR', 'AUDIT_VIEW')) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);

insert into sec_permission_action (permission, area, action)
values ('USER_VIEW', 'USER_ACCESS', 'VIEW')
on conflict do nothing;
