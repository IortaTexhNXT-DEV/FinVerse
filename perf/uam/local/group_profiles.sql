-- 60 group profiles for the load test (PERF_GP_01 to PERF_GP_60), each with a share of the view
-- permissions only, so that no separation-of-duties rule refuses a membership. Throwaway database.
insert into sec_role (code, name, description, privilege_level, active, created_at, created_by, version)
select 'PERF_GP_' || lpad(g::text, 2, '0'), 'Perf Group Profile ' || g, 'Load test group profile',
       'STANDARD', true, now(), 'SYSTEM', 0
from generate_series(1, 60) g
where not exists (select 1 from sec_role r where r.code = 'PERF_GP_' || lpad(g::text, 2, '0'));

with perms as (
  select permission, row_number() over (order by permission) rn
  from (select distinct permission from sec_role_permission where permission like '%\_VIEW') p)
insert into sec_role_permission (role_id, permission)
select r.id, p.permission
from sec_role r
join perms p on (p.rn + r.id) % 5 = 0
where r.code like 'PERF_GP_%'
on conflict do nothing;
