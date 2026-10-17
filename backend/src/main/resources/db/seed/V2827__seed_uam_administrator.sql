-- SIT/UAT user of the User Access Administrator persona (V2826): uamadmin. SEED DATA ONLY - never
-- load in production. The SIT/UAT password is provided to testers separately.
insert into sec_user (username, full_name, email, password_hash, home_branch_id, created_at, created_by)
select 'uamadmin', 'Ursula Access Administrator', 'uamadmin@uat.brokerverse.cloud',
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2',
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
where not exists (select 1 from sec_user x where lower(x.username) = 'uamadmin');

insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u join sec_role r on r.code = 'UAM_ADMINISTRATOR'
where u.username = 'uamadmin'
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
