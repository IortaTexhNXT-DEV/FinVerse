-- The group profile BDOI_SUPER_USER holds every permission: give it the case management
-- permissions of the Customer Servicing Facility (V2907), held by SYSADMIN and the CSF roles.
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
cross join (values ('CSF_CASE_CREATE'), ('CSF_CASE_SUPERVISE'), ('CSF_CASE_FULFIL'),
                   ('CSF_CASE_REPORT'), ('CSF_CASE_ROUTING')) as p(permission)
where r.code = 'BDOI_SUPER_USER'
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = p.permission);
