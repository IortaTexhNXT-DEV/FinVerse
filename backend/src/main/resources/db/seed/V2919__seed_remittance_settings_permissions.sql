-- Remittance Settings (V2918): the Remittance Officer and Team Head propose a change, the Remittance Team Lead
-- approves it; the super user holds both, like every permission.
insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values ('BDOI_REMIT_OFFICER', 'REMIT_SETTINGS_MAINTAIN'), ('BDOI_REMIT_TEAM_HEAD', 'REMIT_SETTINGS_MAINTAIN'),
             ('BDOI_REMIT_TEAM_LEAD', 'REMIT_SETTINGS_APPROVE'),
             ('BDOI_SUPER_USER', 'REMIT_SETTINGS_MAINTAIN'), ('BDOI_SUPER_USER', 'REMIT_SETTINGS_APPROVE'))
  as g(role_code, permission)
  on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);
