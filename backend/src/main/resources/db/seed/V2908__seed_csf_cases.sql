-- =====================================================================================
-- iNXT BrokerVerse - V2908 Seed Customer Servicing Facility case management (BRD-9) (seed profile
-- only; the SIT/UAT password is provided to testers separately). SEED DATA ONLY - never load in
-- production.
--   * csffu, the user of the Fulfilment Unit persona (FRS BRD-9 section 3), member of the units
--     Processing and Claims.
--   * The first case routing table: requests and problems to Processing, complaints to Claims with
--     escalation to Processing, everything else to Processing.
--   * The cases of the storyline of client CL-2026-000001 are logged at start-up by
--     csf.cases.seed.CsfCaseSeedData through the real case services.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select 'csffu', 'Fe Fulfilment Unit Officer', 'csffu@brokerverse-seed.ph',
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select b.id from org_branch b join org_company c on c.id = b.company_id
        where c.code = 'FVI' and b.code = 'HO'),
       now(), 'SYSTEM'
where not exists (select 1 from sec_user x where lower(x.username) = 'csffu');

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join sec_role r on r.code = 'CSF_FULFILMENT'
where u.username = 'csffu'
  and not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

insert into csf_fulfilment_member (unit_code, username, created_at, created_by)
values ('PROCESSING', 'csffu', now(), 'SYSTEM'), ('CLAIMS', 'csffu', now(), 'SYSTEM')
on conflict (unit_code, username) do nothing;

insert into csf_case_routing (case_type, market_segment, nature, fulfilment_unit, escalation_group,
    active, created_at, created_by)
values ('', '', '', 'PROCESSING', null, true, now(), 'SYSTEM'),
       ('REQUEST', 'CONSUMER_1', '', 'PROCESSING', null, true, now(), 'SYSTEM'),
       ('COMPLAINT', '', '', 'CLAIMS', 'PROCESSING', true, now(), 'SYSTEM'),
       ('', '', 'CLAIM_STATUS', 'CLAIMS', null, true, now(), 'SYSTEM')
on conflict (case_type, market_segment, nature) do nothing;
