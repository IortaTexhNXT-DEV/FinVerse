-- =====================================================================================
-- iNXT BrokerVerse - V1109 Seed data of the document storage foundation (build step ST0).
-- SEED DATA ONLY - never load in production. Password of the SIT/UAT users: Brokerverse@2026.
--   Users: holdofficer (RECORDS_HOLD_OFFICER, requests legal holds), holdapprover
--          (RECORDS_HOLD_APPROVER, approves them), infosec (INFOSEC_OFFICER, quarantine alerts).
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, home_branch_id, created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2',
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('holdofficer',  'Hilda Records Officer',    'holdofficer@brokerverse-seed.ph'),
             ('holdapprover', 'Homer Records Approver',   'holdapprover@brokerverse-seed.ph'),
             ('infosec',      'Ingrid Security Officer',  'infosec@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = lower(u.username));

insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u join sec_role r on r.code = case u.username
    when 'holdofficer' then 'RECORDS_HOLD_OFFICER' when 'holdapprover' then 'RECORDS_HOLD_APPROVER'
    else 'INFOSEC_OFFICER' end
where u.username in ('holdofficer', 'holdapprover', 'infosec')
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
