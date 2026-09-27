-- =====================================================================================
-- iNXT BrokerVerse - V1981 Seed data of Data Migration (BRD-13, wave DM0): SIT/UAT set-up.
-- SEED DATA ONLY - never load in production.
--   Owners:     migowner is the business owner and migsteward the Data Steward of every object.
--   Decisions:  the objects of Mock 1 (reference data, clients, policy headers, the RA-sent file,
--               open legacy invoices) decided by migowner on the proposal of miglead (gate G1 signed).
--   Code maps:  approved first versions of the market segment and civil status lists (with a
--               legacy value created in BIBS), and of the product, line, status, user, sales unit,
--               branch and insurer maps used by the client and policy header extracts.
-- The register, layouts and rules themselves are delivered in V1081 (production catalogue).
-- =====================================================================================

update mig_data_object
set business_owner = 'migowner', data_steward = 'migsteward', updated_at = now(), updated_by = 'SYSTEM'
where business_owner is null;

-- ---------- Decisions of the Mock 1 objects (G1) ----------------------------------------------
insert into mig_object_decision (company_id, decision_no, object_code, proposed_class, condition_text,
    condition_met, criteria, rationale, status, submitted_by, submitted_at, decided_by, decided_at,
    created_at, created_by)
select c.id, 'MGD-2026-9000' || lpad(o.seq::text, 2, '0'), d.code, d.proposed_class, d.condition_text,
       d.proposed_class = 'CONDITIONAL',
       'Day-1 need: ' || case when d.day1_need then 'Yes' else 'No' end
           || '; compliance need: ' || case when d.compliance_need then 'Yes' else 'No' end
           || '; read-only / archival option: ' || case when d.archival_option then 'Yes' else 'No' end
           || '; data trust: ' || d.data_trust,
       d.rationale, 'DECIDED', 'miglead', now() - interval '20 day', 'migowner', now() - interval '18 day',
       now() - interval '20 day', 'miglead'
from (values ('R01', 1), ('R02', 2), ('R03', 3), ('R04', 4), ('R05', 5), ('R06', 6), ('R07', 7),
             ('R08', 8), ('R11', 9), ('C01', 10), ('C03', 11), ('P01', 12), ('P03', 13), ('F01', 14))
     as o(code, seq)
join mig_data_object d on d.code = o.code
cross join org_company c
where c.code = 'FVI'
  and not exists (select 1 from mig_object_decision x where x.object_code = d.code);

update mig_data_object d
set decided_class = x.proposed_class, condition_met = x.condition_met, status = 'DECIDED',
    decided_by = x.decided_by, decided_at = x.decided_at, updated_at = now(), updated_by = 'migowner'
from mig_object_decision x
where x.object_code = d.code and x.status = 'DECIDED' and d.status <> 'DECIDED';

insert into mig_signoff (company_id, object_code, gate, role_code, username, decision, comment, signed_at)
select x.company_id, x.object_code, 'G1', 'DATA_OWNER', x.decided_by, 'APPROVED',
       'Class ' || x.proposed_class, x.decided_at
from mig_object_decision x
where x.status = 'DECIDED'
  and not exists (select 1 from mig_signoff s where s.object_code = x.object_code and s.gate = 'G1');

-- ---------- Approved code map versions (G2 of the lists) --------------------------------------
insert into mig_code_map_version (set_code, version_no, status, comment, submitted_by, submitted_at,
    approved_by, approved_at, created_at, created_by)
select s.code, 1, 'APPROVED', 'First mapping of the legacy values', 'migsteward', now() - interval '15 day',
       'migowner', now() - interval '14 day', now() - interval '16 day', 'migsteward'
from mig_code_map_set s
where (s.code in ('LOV:MARKET_SEGMENT', 'LOV:CIVIL_STATUS')
       or s.code in (select c.map_set from mig_layout_column c join mig_layout l on l.id = c.layout_id
                     where l.object_code in ('C01', 'C03', 'P01', 'P03', 'F01') and c.map_set is not null))
  and not exists (select 1 from mig_code_map_version v where v.set_code = s.code);

insert into mig_code_map_entry (version_id, source_system, legacy_code, legacy_description, action, target_code,
    remarks)
select v.id, e.source_system, e.legacy_code, e.legacy_description, e.action, e.target_code, e.remarks
from (values ('LOV:MARKET_SEGMENT', 'QPS',  'CBG',  'Consumer Banking',   'MAP',    'CBG',     null),
             ('LOV:MARKET_SEGMENT', 'QPS',  'COMM', 'Commercial Banking', 'MAP',    'COMBANK', null),
             ('LOV:MARKET_SEGMENT', 'QPS',  'CORP', 'Corporate Banking',  'MAP',    'CORBANK', null),
             ('LOV:MARKET_SEGMENT', 'EBIX', 'RTL',  'Retail',             'MAP',    'RETAIL',  null),
             ('LOV:MARKET_SEGMENT', 'EBIX', 'CBG',  'Consumer Banking',   'MAP',    'CBG',     null),
             ('LOV:MARKET_SEGMENT', 'EBIX', 'PBG',  'Private Banking Group', 'CREATE', 'PBG',
              'New segment used by legacy private banking clients'),
             ('LOV:CIVIL_STATUS',   'QPS',  'S',    'Single',             'MAP',    'SINGLE',  null),
             ('LOV:CIVIL_STATUS',   'QPS',  'M',    'Married',            'MAP',    'MARRIED', null),
             ('LOV:CIVIL_STATUS',   'QPS',  'W',    'Widow/er',           'MAP',    'WIDOWED', null),
             ('LOV:CIVIL_STATUS',   'EBIX', 'SEP',  'Separated',          'MAP',    'SEPARATED', null),
             ('PRODUCT', 'QPS', 'CAR-A', 'Contractor''s all risk (employees)', 'MAP', 'CAR01', null),
             ('LINE', 'QPS', 'ENG', 'Engineering', 'MAP', 'ENGINEERING', null),
             ('STATUS:CLIENT', 'QPS', 'A', 'Active', 'MAP', 'ACTIVE', null),
             ('STATUS:CLIENT', 'QPS', 'I', 'Inactive', 'MAP', 'INACTIVE', null),
             ('STATUS:KYC', 'QPS', 'COMPLETE', 'KYC complete', 'MAP', 'VERIFIED', null),
             ('STATUS:KYC', 'QPS', 'PEND', 'KYC pending', 'MAP', 'PENDING', null),
             ('STATUS:POLICY', 'QPS', 'IF', 'In force', 'MAP', 'IN_FORCE', null),
             ('USER', 'QPS', 'AO01', 'Account officer 01', 'MAP', 'ao', null),
             ('SALES_UNIT', 'QPS', 'U01', 'Metro unit 1', 'MAP', 'T-CBG1', null),
             ('BRANCH', 'QPS', 'MKT', 'Makati', 'MAP', 'HO', null),
             ('INSURER', 'QPS', 'MGIC', 'Mabuhay General', 'MAP', 'INS-MGIC', null),
             ('PRODUCT', 'EBIX', 'CAR-A', 'Contractor''s all risk (employees)', 'MAP', 'CAR01', null),
             ('LINE', 'EBIX', 'ENG', 'Engineering', 'MAP', 'ENGINEERING', null),
             ('STATUS:CLIENT', 'EBIX', 'A', 'Active', 'MAP', 'ACTIVE', null),
             ('STATUS:CLIENT', 'EBIX', 'I', 'Inactive', 'MAP', 'INACTIVE', null),
             ('STATUS:KYC', 'EBIX', 'COMPLETE', 'KYC complete', 'MAP', 'VERIFIED', null),
             ('STATUS:KYC', 'EBIX', 'PEND', 'KYC pending', 'MAP', 'PENDING', null),
             ('STATUS:POLICY', 'EBIX', 'IF', 'In force', 'MAP', 'IN_FORCE', null),
             ('USER', 'EBIX', 'AO01', 'Account officer 01', 'MAP', 'ao', null),
             ('SALES_UNIT', 'EBIX', 'U01', 'Metro unit 1', 'MAP', 'T-CBG1', null),
             ('BRANCH', 'EBIX', 'MKT', 'Makati', 'MAP', 'HO', null),
             ('INSURER', 'EBIX', 'MGIC', 'Mabuhay General', 'MAP', 'INS-MGIC', null),
             ('COST_CENTER', 'EBIX', 'OPS', 'Operations', 'MAP', 'FIN', null),
             ('COST_CENTER', 'QPS', 'OPS', 'Operations', 'MAP', 'FIN', null))
     as e(set_code, source_system, legacy_code, legacy_description, action, target_code, remarks)
join mig_code_map_version v on v.set_code = e.set_code and v.version_no = 1
where not exists (select 1 from mig_code_map_entry x where x.version_id = v.id and x.legacy_code = e.legacy_code
                  and x.source_system = e.source_system);
