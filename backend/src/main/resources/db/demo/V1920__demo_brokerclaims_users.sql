-- =====================================================================================
-- iNXT BrokerVerse - V1920 Demo Claims Handling (BRD-7) users (demo profile only; password for all
-- users: Brokerverse@2026). DEMO ONLY - never load in production.
--   Users of the Claims personas (CLAIMS_BROKING_DESIGN 7.3; FRS BRD-7 section 3): clmofficer
--   (Claims Officer, Motor HO), clmofficer2 (Claims Officer, Non-Motor HO), clmbranch (Claims
--   Officer, Cebu branch), clmtl (Team Lead, Motor HO), clmth (Team Head, Non-Motor HO), clmuh
--   (Unit Head) and clmrisk (Claims / Risk user), with their units in the claims handler register.
--   The demo Marketing users (ao, ao2 = MKT_AO; mkttl = MKT_TL) receive the Claims report
--   permissions through their roles (V1020). The roles and grants are in V1020; the insurer demo
--   user "claims" is unrelated and keeps its insurer roles. Insurer location references come with
--   V1921 (CL1-A); demo claims with brokerclaims.demo.BrokerClaimsDemoData (CL1-A / CL1-B).
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       coalesce((select b.id from org_branch b join org_company c on c.id = b.company_id
                 where c.code = 'FVI' and b.code = u.branch_code),
                (select id from org_branch where code = 'HO' order by id limit 1)),
       now(), 'SYSTEM'
from (values ('clmofficer',  'Clarissa Claims Officer',        'clmofficer@brokerverse-demo.ph',  'HO'),
             ('clmofficer2', 'Cedric Claims Officer',          'clmofficer2@brokerverse-demo.ph', 'HO'),
             ('clmbranch',   'Consuelo Claims Officer (Cebu)', 'clmbranch@brokerverse-demo.ph',   'CEB'),
             ('clmtl',       'Teodoro Claims Team Lead',       'clmtl@brokerverse-demo.ph',       'HO'),
             ('clmth',       'Theresa Claims Team Head',       'clmth@brokerverse-demo.ph',       'HO'),
             ('clmuh',       'Ulysses Claims Unit Head',       'clmuh@brokerverse-demo.ph',       'HO'),
             ('clmrisk',     'Rosario Claims Risk Analyst',    'clmrisk@brokerverse-demo.ph',     'HO'))
     as u(username, full_name, email, branch_code)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('clmofficer', 'CLM_OFFICER'), ('clmofficer2', 'CLM_OFFICER'), ('clmbranch', 'CLM_OFFICER'),
             ('clmtl', 'CLM_TL'), ('clmth', 'CLM_TH'), ('clmuh', 'CLM_UH'), ('clmrisk', 'CLM_RISK'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

-- ---------- Claims handler register (BRCLM.012; units CLQ04) ----------------------------------
insert into bcl_handler (username, unit_code, team, active, created_at, created_by)
select h.username, h.unit_code, h.team, true, now(), 'SYSTEM'
from (values ('clmofficer', 'MOTOR_HO', 'Motor Team 1'),
             ('clmofficer2', 'NON_MOTOR_HO', 'Non-Motor Team 1'),
             ('clmbranch', 'BRANCH_CEBU', 'Cebu Claims'),
             ('clmtl', 'MOTOR_HO', 'Motor Team 1'),
             ('clmth', 'NON_MOTOR_HO', 'Non-Motor Team 1'))
     as h(username, unit_code, team)
where not exists (select 1 from bcl_handler x where x.username = h.username);
