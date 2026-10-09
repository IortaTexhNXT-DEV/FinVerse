-- =====================================================================================
-- iNXT BrokerVerse - V2560 Configuration Promotion in the SIT/UAT databases (seed profile only).
-- SEED DATA ONLY - never load in production.
--   cfgapprover  Configuration Release Approver (CONFIG_APPROVER, V1320): the second user who
--                approves the imports the System Administrator (admin) prepares; separation-of-duties
--                rule SOD-CFG-01 keeps the two profiles apart.
--   CONFIG_PROMOTION_PIPELINE_APPLY stays false: the pipeline prepares and submits, a second user
--   approves, as in production.
--   Same pattern as the other SIT/UAT user scripts: the shared password hash of the seed scripts,
--   which SeedPasswords replaces with the password of the environment at start-up, Head Office as
--   home branch, every company in scope.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select 'cfgapprover', 'Consolacion Configuration Approver', 'cfgapprover@brokerverse-seed.ph',
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select b.id from org_branch b join org_company c on c.id = b.company_id
        where c.code = 'FVI' and b.code = 'HO'),
       now(), 'SYSTEM'
where not exists (select 1 from sec_user x where lower(x.username) = 'cfgapprover');

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join sec_role r on r.code = 'CONFIG_APPROVER'
where u.username = 'cfgapprover'
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
