-- =====================================================================================
-- iNXT BrokerVerse - V1933 Seed Employee Benefits (BRD-8) catalogue and business masters (seed
-- profile only). SEED DATA ONLY - never load in production.
--   Product lines HMO, GLI and GPA (generic items rated at the item's own rate, so the account of a
--   confirmed line reproduces the chosen annual premium) and one non-package product per line:
--   EBHMO01, EBGLI01, EBGPA01 (EMPLOYEE_BENEFITS_DESIGN 11, catalogue row; BDOI gives the real
--   product set, EBQ01)
--   Two HMO providers of the seed panel (names invented for the seed): Maharlika Health Care
--   (accredited) and Kalinga Health Plans (accreditation lapsed: its placement needs the ISACOM
--   approval document, EBQ24), with commission rates of the EB products per provider and insurer
--   Threshold rules of the design examples (TSI 500M, annual premium 20M; EBQ11) and the required
--   documents per process (FR-EB-034), both authorised
-- =====================================================================================

insert into cat_product_line (code, name, risk_item_kind, rating_method, sort_order, record_status,
                              authorized_by, authorized_at, created_at, created_by)
select v.code, v.name, 'GENERIC', 'GENERIC', v.sort_order, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from (values
    ('HMO', 'Health Maintenance (HMO)', 200),
    ('GLI', 'Group Life Insurance', 210),
    ('GPA', 'Group Personal Accident', 220)
) as v(code, name, sort_order)
where not exists (select 1 from cat_product_line x where x.code = v.code);

insert into cat_product (code, name, line_code, cover_type_code, packaged, fleet_capable,
                         market_segments, mortgage_applicable, direct_payment_eligible,
                         multi_year_allowed, max_term_years, ffy_eligible, payment_gate,
                         default_rate, default_commission_rate, minimum_premium, record_status,
                         authorized_by, authorized_at, created_at, created_by)
select v.code, v.name, v.line_code, null, false, false, null, false, true, false, 1, false,
       'CLIENT_CONFIRMATION', null, v.commission, 0, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from (values
    ('EBHMO01', 'Group HMO - corporate programme', 'HMO', 10),
    ('EBGLI01', 'Group Life Insurance - corporate programme', 'GLI', 15),
    ('EBGPA01', 'Group Personal Accident - corporate programme', 'GPA', 20)
) as v(code, name, line_code, commission)
where not exists (select 1 from cat_product x where x.code = v.code);

insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, p.code, p.name, 'INSURER', p.tin, p.addr, p.email, null, 'PHP', 30, 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from org_company c, (values
  ('HMO-MHC', 'Maharlika Health Care Inc.', '504-111-222-000', 'Pasig', 'corporate@maharlikahealth.example'),
  ('HMO-KHP', 'Kalinga Health Plans Corp.', '505-111-222-000', 'Baguio City', 'accounts@kalingahealth.example')
) as p(code, name, tin, addr, email)
where c.code = 'FVI'
  and not exists (select 1 from pty_party x where x.company_id = c.id and x.code = p.code);

insert into cat_insurer (company_id, party_code, name, short_name, accreditation_no, accredited_until,
    placement_channel, placement_emails, default_credit_days, record_status, authorized_by,
    authorized_at, created_at, created_by)
select c.id, i.code, i.name, i.short, i.acc, i.until::date, 'EMAIL', i.email, 30, 'ACTIVE', 'SYSTEM',
       now(), now(), 'SYSTEM'
from org_company c, (values
  ('HMO-MHC', 'Maharlika Health Care Inc.', 'Maharlika Health', 'HMO-2020-011', '2028-06-30', 'corporate@maharlikahealth.example'),
  ('HMO-KHP', 'Kalinga Health Plans Corp.', 'Kalinga Health', 'HMO-2019-007', '2025-12-31', 'accounts@kalingahealth.example')
) as i(code, name, short, acc, until, email)
where c.code = 'FVI'
  and not exists (select 1 from cat_insurer x where x.company_id = c.id and x.party_code = i.code);

insert into cat_commission_rate (company_id, insurer_code, product_code, rate, effective_from,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, r.insurer, r.product, r.rate, date '2026-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('HMO-MHC', 'EBHMO01', 12),
  ('HMO-KHP', 'EBHMO01', 11),
  ('INS-MGIC', 'EBHMO01', 10),
  ('INS-VMI', 'EBHMO01', 10),
  ('INS-MGIC', 'EBGLI01', 15),
  ('INS-LAC', 'EBGLI01', 16),
  ('INS-LAC', 'EBGPA01', 20),
  ('INS-MPI', 'EBGPA01', 18)
) as r(insurer, product, rate)
where c.code = 'FVI'
  and not exists (select 1 from cat_commission_rate x where x.company_id = c.id
                  and x.insurer_code = r.insurer and x.product_code = r.product);

-- Threshold rules of the design examples (values for BDOI to confirm).
insert into eb_threshold_rule (company_id, benefit_line, measure, amount, currency, approver_permission,
    approval_level, effective_from, effective_to, description, record_status, authorized_by, authorized_at,
    created_at, created_by)
select c.id, null, t.measure, t.amount, 'PHP', 'EB_THRESHOLD_APPROVE', 1, date '2026-01-01', null, t.description,
       'ACTIVE', 'badmin', now(), now(), 'SYSTEM'
from org_company c, (values
  ('TSI', 500000000.00, 'Total sum insured of a line at or above PHP 500 million'),
  ('ANNUAL_PREMIUM', 20000000.00, 'Annual premium of a line at or above PHP 20 million')
) as t(measure, amount, description)
where c.code = 'FVI'
  and not exists (select 1 from eb_threshold_rule x where x.company_id = c.id and x.measure = t.measure);

-- Required documents per process (FR-EB-034).
insert into eb_required_document (company_id, process_type, benefit_line, document_type, mandatory,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, d.process, null, d.document_type, d.mandatory, 'ACTIVE', 'badmin', now(), now(), 'SYSTEM'
from org_company c, (values
  ('FRANCHISE', 'EB_BOR', true),
  ('FRANCHISE', 'EB_MASTERLIST_UNNAMED', false),
  ('PROPOSAL', 'EB_TOR', true),
  ('PROPOSAL', 'EB_UTILIZATION', false),
  ('NB_PLACEMENT', 'EB_CLIENT_CONFIRMATION', true),
  ('NB_PLACEMENT', 'EB_BOR', true),
  ('NB_PLACEMENT', 'EB_MASTERLIST', false),
  ('RENEWAL_PLACEMENT', 'EB_CLIENT_CONFIRMATION', true),
  ('RENEWAL_PLACEMENT', 'EB_MASTERLIST', false),
  ('ENDORSEMENT', 'EB_MEMBER_CHANGE', true),
  ('ADJUSTMENT', 'EB_MEMBER_CHANGE', true)
) as d(process, document_type, mandatory)
where c.code = 'FVI'
  and not exists (select 1 from eb_required_document x where x.company_id = c.id
                  and x.process_type = d.process and x.document_type = d.document_type);
