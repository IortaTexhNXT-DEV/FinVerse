-- The group profile BDOI_SUPER_USER holds every permission: give it the reinsurance permission of Phase 2 (V2914),
-- which no BDOI group profile holds.
insert into sec_role_permission (role_id, permission)
select r.id, 'RI_BROKING_TRANSACT'
from sec_role r
where r.code = 'BDOI_SUPER_USER'
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = 'RI_BROKING_TRANSACT');
