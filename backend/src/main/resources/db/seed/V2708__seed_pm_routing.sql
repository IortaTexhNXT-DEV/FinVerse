-- =====================================================================================
-- SIT/UAT seed data (seed profile only; never loaded in production): the people of BDOI's Product
-- Maintenance routing - the Marketing Team Head and Unit Head of the three-level Marketing approval,
-- a second ManCom member as the President who approves last - and a product master change waiting
-- for the transfer, so the routing and the monitoring screens can be walked through. The users
-- take the password of the existing SIT users (no password is written here).
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    user_level, created_at, created_by)
select u.username, u.full_name, u.email,
       (select x.password_hash from sec_user x where x.username = 'mancom'), null,
       (select id from org_branch where code = 'HO' order by id limit 1), u.user_level, now(), 'SYSTEM'
from (values ('mktth',      'Marco Marketing Team Head', 'mktth@brokerverse-seed.ph',      'TEAM_HEAD'),
             ('mktuh',      'Mila Marketing Unit Head',  'mktuh@brokerverse-seed.ph',      'UNIT_HEAD'),
             ('mancompres', 'Patricia ManCom President', 'mancompres@brokerverse-seed.ph', null))
     as u(username, full_name, email, user_level)
where not exists (select 1 from sec_user x where lower(x.username) = u.username)
  and exists (select 1 from sec_user x where x.username = 'mancom');

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('mktth', 'MKT_TL'), ('mktuh', 'MKT_TL'), ('mancompres', 'MANCOM'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

update sec_user set user_level = 'TEAM_LEAD' where username = 'mkttl' and user_level is null;

update sys_parameter set param_value = 'mancompres', updated_at = now(), updated_by = 'SYSTEM'
where param_key = 'PM_MANCOM_PRESIDENT';

insert into pm_master_change (product_code, version_no, change_kind, effective_date,
    source_request_no, status, attempts, created_at, created_by)
values ('MTR29', 1, 'RETIRED', current_date + 45, 'PKD-2026-900003', 'PENDING', 0, now(), 'SYSTEM');
