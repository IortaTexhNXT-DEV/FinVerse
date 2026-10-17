-- =====================================================================================
-- iNXT BrokerVerse - V2520 SIT/UAT users of the walkthrough personas that had none (seed profile
-- only; the SIT/UAT password is provided to testers separately). SEED DATA ONLY - never load in
-- production.
--   The UAT walkthrough users workbook (docs/deliverables/src/programme/uat) gives every persona of
--   the FRS persona tables a sign-in ID. These personas had no SIT/UAT user before this script:
--     appsupport  Application Support (APP_SUPPORT, BRD-4): Collections set-up, audit log,
--                 interface view and job runs
--     dco         Data Control Officer (DCO, BRD-4): the same Collections support functions
--     sbmrules    Rule administrator of Submitted Policies (SBM_RULE_ADMIN, BRD-12): maker of the
--                 rule sets, limits, insurer and letter rules; the NB Team Lead approves them
--     rnwmaker    Renewal processing team, maker (Data Steward profile, BRD-13 FR-DM-125):
--                 corrects the rejected rows of the renewal advices already sent and prepares the
--                 resubmission
--     rnwchecker  Renewal processing team, checker (Data owner profile, BRD-13 FR-DM-125):
--                 approves or returns the resubmission (never its maker)
--     migpm       Program Manager, Business Project Services (Go / no-go board profile, BRD-13):
--                 chairs the go / no-go board
--   Same pattern as the other SIT/UAT user scripts: the shared password hash of the seed scripts,
--   which SeedPasswords replaces with the password of the environment at start-up
--   (BROKERVERSE_SEED_PASSWORD; must-change as set by BROKERVERSE_SEED_PASSWORD_MUST_CHANGE), Head
--   Office as home branch, every company in scope (the default). Each user holds one role.
--   Version: above every other seed script, clear of the removal scripts V2500-V2510 of the
--   insurer suite.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select b.id from org_branch b join org_company c on c.id = b.company_id
        where c.code = 'FVI' and b.code = 'HO'),
       now(), 'SYSTEM'
from (values ('appsupport', 'Apolinario Application Support', 'appsupport@brokerverse-seed.ph'),
             ('dco',        'Dionisia Data Control Officer',  'dco@brokerverse-seed.ph'),
             ('sbmrules',   'Rufino Rule Administrator',      'sbmrules@brokerverse-seed.ph'),
             ('rnwmaker',   'Rhodora Renewal Maker',          'rnwmaker@brokerverse-seed.ph'),
             ('rnwchecker', 'Rogelio Renewal Checker',        'rnwchecker@brokerverse-seed.ph'),
             ('migpm',      'Patricio Program Manager',       'migpm@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('appsupport', 'APP_SUPPORT'), ('dco', 'DCO'), ('sbmrules', 'SBM_RULE_ADMIN'),
             ('rnwmaker', 'DATA_STEWARD'), ('rnwchecker', 'DATA_OWNER'), ('migpm', 'MIGRATION_GONOGO'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
