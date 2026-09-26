-- =====================================================================================
-- iNXT BrokerVerse - V1930 Seed Employee Benefits (BRD-8) SIT/UAT users (seed profile only;
-- password for all users: Brokerverse@2026). SEED DATA ONLY - never load in production.
--   Users of the EB personas (EMPLOYEE_BENEFITS_DESIGN 6.2; FRS BRD-8 section 3): ebao and ebao2
--   (Marketing AO, EB), ebtl (Marketing TL / UH, EB), ebmgmt (BDOI Management), ebproc (Processing,
--   EB), ebprocsup (Processing Supervisor) and ebcoll (Collection, EB). The roles and grants are in
--   V1030; the Business Administrator seed user badmin holds EB_SETUP through BUSINESS_ADMIN.
--   BDOI Drop 2 has no partner portal: no portal user is seeded. The EB products and HMO provider
--   panels of the design follow with the product set-up (EBQ01); the programmes of V1931 name the
--   seed insurers of V982.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select b.id from org_branch b join org_company c on c.id = b.company_id
        where c.code = 'FVI' and b.code = 'HO'),
       now(), 'SYSTEM'
from (values ('ebao',      'Erlinda Benefits Officer',       'ebao@brokerverse-seed.ph'),
             ('ebao2',     'Ernesto Benefits Officer',       'ebao2@brokerverse-seed.ph'),
             ('ebtl',      'Teresita Benefits Team Lead',    'ebtl@brokerverse-seed.ph'),
             ('ebmgmt',    'Manuel Benefits Management',     'ebmgmt@brokerverse-seed.ph'),
             ('ebproc',    'Priscilla Benefits Processor',   'ebproc@brokerverse-seed.ph'),
             ('ebprocsup', 'Porfirio Processing Supervisor', 'ebprocsup@brokerverse-seed.ph'),
             ('ebcoll',    'Corazon Benefits Collection',    'ebcoll@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('ebao', 'EB_AO'), ('ebao2', 'EB_AO'), ('ebtl', 'EB_TL'), ('ebmgmt', 'EB_MANAGEMENT'),
             ('ebproc', 'EB_PROCESSOR'), ('ebprocsup', 'EB_PROC_SUPERVISOR'),
             ('ebcoll', 'EB_COLLECTION'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
