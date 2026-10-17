-- Renewal TSU queue: the TSU group opens the TSU requests of renewal accounts from its menu
-- (BDOI Renewal FRS FRRN.018.01).
insert into sec_permission_action (permission, area, action)
values ('RNW_TSU_QUEUE', 'RENEWAL', 'AMEND')
on conflict do nothing;

insert into sec_role_permission (role_id, permission)
select r.id, 'RNW_TSU_QUEUE'
from sec_role r
where r.code = 'TSU'
  and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = 'RNW_TSU_QUEUE');
