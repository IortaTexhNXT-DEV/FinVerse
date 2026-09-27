-- =====================================================================================
-- iNXT BrokerVerse - V1980 Seed data of Data Migration (BRD-13, wave DM0): SIT/UAT users.
-- SEED DATA ONLY - never load in production. Password of the SIT/UAT users: Brokerverse@2026.
--   miglead     Data Migration Lead            (DATA_MIGRATION_LEAD)
--   migsteward  Data Steward                   (DATA_STEWARD)
--   migowner    Business owner of the objects  (DATA_OWNER)
--   migops      Migration operator (IT)        (MIGRATION_OPERATOR)
--   migrecon    Reconciliation approver        (MIGRATION_RECON_APPROVER)
--   miggonogo   Go / no-go board               (MIGRATION_GONOGO)
--   legacyaudit Audit / Compliance user        (LEGACY_INQUIRY)
--   legacyrev   Compliance reviewer            (LEGACY_ACCESS_REVIEWER)
--   topmgmt     Top management approver        (TOP_MANAGEMENT_APPROVER)
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, home_branch_id, created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2',
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('miglead',     'Mila Migration Lead',        'miglead@brokerverse-seed.ph'),
             ('migsteward',  'Stefan Data Steward',        'migsteward@brokerverse-seed.ph'),
             ('migowner',    'Olivia Data Owner',          'migowner@brokerverse-seed.ph'),
             ('migops',      'Oscar Migration Operator',   'migops@brokerverse-seed.ph'),
             ('migrecon',    'Rosa Reconciliation Approver', 'migrecon@brokerverse-seed.ph'),
             ('miggonogo',   'Gregorio Go-Live Board',     'miggonogo@brokerverse-seed.ph'),
             ('legacyaudit', 'Lara Legacy Auditor',        'legacyaudit@brokerverse-seed.ph'),
             ('legacyrev',   'Ramon Access Reviewer',      'legacyrev@brokerverse-seed.ph'),
             ('topmgmt',     'Teresa Top Management',      'topmgmt@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = lower(u.username));

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join sec_role r on r.code = case u.username
    when 'miglead' then 'DATA_MIGRATION_LEAD'
    when 'migsteward' then 'DATA_STEWARD'
    when 'migowner' then 'DATA_OWNER'
    when 'migops' then 'MIGRATION_OPERATOR'
    when 'migrecon' then 'MIGRATION_RECON_APPROVER'
    when 'miggonogo' then 'MIGRATION_GONOGO'
    when 'legacyaudit' then 'LEGACY_INQUIRY'
    when 'legacyrev' then 'LEGACY_ACCESS_REVIEWER'
    when 'topmgmt' then 'TOP_MANAGEMENT_APPROVER' end
where u.username in ('miglead', 'migsteward', 'migowner', 'migops', 'migrecon', 'miggonogo',
                     'legacyaudit', 'legacyrev', 'topmgmt')
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

