-- The group profile BDOI_SUPER_USER holds every permission: give it the MIS fields permissions of the
-- reports (V2912), held by SYSADMIN.
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('MIS_FIELD_MAINTAIN'), ('MIS_FIELD_AUTHORIZE')) as p(permission)
where r.code = 'BDOI_SUPER_USER'
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);
