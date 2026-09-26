-- =====================================================================================
-- iNXT BrokerVerse - V996 Seed Product Maintenance catalog data (seed profile only).
-- SEED DATA ONLY - never load in production.
--   BRPM.007 / PMADD01/02/06  version 2 DRAFT of the MTR12 motor package, set up by MBS (mbs):
--            new rate scheme, coverages and the terms of two panel insurers, ready to be
--            submitted and validated by the TSU Head (tsuhead) or Business Administrator (badmin)
--   PMADD07/08  incentive criterion "CPC2 (seed)" on Motor and Property packages, ACTIVE, and the
--            booking incentive rule of the seed (V988) copied into the catalog as V871 does for
--            existing rules (pending authorisation)
--   Design: docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md sections 3 and 4. Dates are relative
--   to the load date so that the draft can always be released.
-- =====================================================================================

-- ---------- MTR12 version 2 (DRAFT) --------------------------------------------------------
insert into cat_product_version (product_code, version_no, status, effective_from, package_start_date,
    package_end_date, anniversary_date, default_rate, minimum_premium, default_commission_rate,
    max_sum_insured, rating_basis_note, manual_rate_allowed, change_summary, created_at, created_by)
values ('MTR12', 2, 'DRAFT', current_date + 30, current_date + 30, current_date + 395, null, 1.25,
        5500, 15, 5000000, 'Comprehensive rate on the vehicle value; Acts of Nature included', false,
        'Placeholder: yearly review of the MTR12 package with a panel of two insurers', now(), 'mbs');

insert into cat_package_coverage (version_id, coverage_code, included, optional, limit_amount,
    sub_limit, deductible_amount, deductible_percent, deductible_text, sort_order)
select v.id, c.code, c.included, c.optional, c.limit_amount, null, c.ded_amount, null, c.ded_text,
       c.sort_order
from cat_product_version v
cross join (values
    ('OD_THEFT', true, false, null::numeric, 2000::numeric, 'Participation fee per claim', 10),
    ('EXCESS_BI', true, false, 250000::numeric, null::numeric, null, 20),
    ('PD', true, false, 250000::numeric, null::numeric, null, 30),
    ('AON', true, false, null::numeric, null::numeric, '1% of the sum insured', 40),
    ('AUTO_PA', false, true, 50000::numeric, null::numeric, null, 50)
) as c(code, included, optional, limit_amount, ded_amount, ded_text, sort_order)
where v.product_code = 'MTR12' and v.version_no = 2;

insert into cat_package_insurer (version_id, company_id, insurer_code, role, share_percent, rate,
    minimum_premium, default_branch_code)
select v.id, co.id, i.code, 'PANEL', null, i.rate, i.minimum, i.branch
from cat_product_version v
cross join org_company co
cross join (values
    ('INS-MGIC', 1.25::numeric, null::numeric, 'MKT'),
    ('INS-LAC', 1.30::numeric, 6000::numeric, 'QC')
) as i(code, rate, minimum, branch)
where v.product_code = 'MTR12' and v.version_no = 2 and co.code = 'FVI';

insert into cat_package_insurer_term (version_id, insurer_code, coverage_code, included, limit_amount,
    sub_limit, deductible_amount, deductible_percent, deductible_text, clause_codes, remarks)
select v.id, i.code, c.code, true, c.limit_amount, null, c.ded_amount, null, c.ded_text,
       case when c.code = 'OD_THEFT' then i.clauses else 'GEN_SANCTIONS' end, null
from cat_product_version v
cross join (values
    ('INS-MGIC', 'MTR_ACCREDITED_REPAIR,MTR_PARTICIPATION,GEN_SANCTIONS'),
    ('INS-LAC', 'MTR_DEPRECIATION,MTR_PARTICIPATION,GEN_SANCTIONS')
) as i(code, clauses)
cross join (values
    ('OD_THEFT', null::numeric, 2000::numeric, 'Participation fee per claim'),
    ('EXCESS_BI', 250000::numeric, null::numeric, null),
    ('PD', 250000::numeric, null::numeric, null),
    ('AON', null::numeric, null::numeric, '1% of the sum insured')
) as c(code, limit_amount, ded_amount, ded_text)
where v.product_code = 'MTR12' and v.version_no = 2;

-- ---------- Incentive criterion CPC2 (seed), ACTIVE ----------------------------------------
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'INCENTIVE_TYPE', 'CAMPAIGN', 'Sales campaign (seed)', 10, null, date '2020-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
where not exists (select 1 from lov_value where type_code = 'INCENTIVE_TYPE' and code = 'CAMPAIGN');

insert into cat_incentive_criteria (company_id, code, name, incentive_type, value_basis, value,
    rule_params, description, effective_from, effective_to, record_status, authorized_by,
    authorized_at, created_at, created_by)
select id, 'CPC2', 'CPC2 (seed)', 'CAMPAIGN', 'RATE', 1.00, '{"minimumPremium": 5000}',
       'Seed criterion: CBG motor and property packages sold in 2026 (definition pending, PQ04)',
       date '2026-01-01', null, 'ACTIVE', 'approver', now(), now(), 'mbs'
from org_company where code = 'FVI';

insert into cat_incentive_criteria_product (criteria_id, product_code, cover_type_code, market_segment,
    source_channel, insurer_code)
select c.id, p.product_code, null, p.segment, null, null
from cat_incentive_criteria c
cross join (values ('MTR10', 'CBG'), ('MTR12', null), ('PAR19', null), ('PAR25', null))
    as p(product_code, segment)
where c.code = 'CPC2';

-- ---------- Seed booking incentive rules copied like V871 (pending authorisation) -----------
insert into cat_incentive_criteria (company_id, code, name, incentive_type, value_basis, value,
    description, effective_from, effective_to, record_status, created_at, created_by)
select r.company_id, 'MIG-' || r.id, left(r.description, 150), 'MIGRATED', 'RULE', null,
       'Migrated from booking incentive rule ' || r.id, r.period_from, r.period_to,
       'PENDING_AUTHORIZATION', now(), 'SYSTEM'
from bkg_incentive_rule r
where r.product_code is not null
  and not exists (select 1 from cat_incentive_criteria c
                  where c.company_id = r.company_id and c.code = 'MIG-' || r.id);

insert into cat_incentive_criteria_product (criteria_id, product_code, market_segment, source_channel)
select c.id, r.product_code, r.market_segment, r.source_channel
from bkg_incentive_rule r
join cat_incentive_criteria c on c.company_id = r.company_id and c.code = 'MIG-' || r.id
where not exists (select 1 from cat_incentive_criteria_product x where x.criteria_id = c.id);

-- ---------- A package ending in 45 days (Package Expiry list, BRPM.017) ---------------------
update cat_product_version
set package_start_date = current_date - 320,
    package_end_date = current_date + 45
where product_code = 'PAR25' and version_no = 1;
