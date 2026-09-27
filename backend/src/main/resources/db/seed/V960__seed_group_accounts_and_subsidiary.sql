-- =====================================================================================
-- SEED DATA (seed profile only): group accounts of the parent and a foreign subsidiary so
-- inter-company postings, FX revaluation and consolidation can be shown.
--   * FVI (parent, PHP): investment in subsidiaries, due from / due to related companies
--     (foreign currency, revalued), currency translation reserve, non-controlling interest,
--     goodwill.
--   * FVS (subsidiary, USD): "BDO Insurance and Reinsurance Brokers (Singapore) Pte. Ltd." with a head office
--     branch, a copy of the FVI chart of accounts and dimensions, and fiscal year 2026.
-- Journals of the subsidiary are posted by PlanningSeedData (seed profile start-up runner).
-- =====================================================================================

-- ---------- Parent: group accounts ------------------------------------------------------
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, a.code, a.name, a.cls, a.lvl,
       (select p.id from coa_account p where p.company_id = c.id and p.code = a.parent),
       (select g.id from coa_category g where g.code = a.cat), true,
       false, 'NONE', true, false, false, a.reval, a.reval, false, a.grp, date '2010-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company c,
(values ('1506', 'Investment in Subsidiaries',         'ASSET',     'SUB',  '1500', 'INVT', false, 'Financial Assets'),
        ('1607', 'Due from Related Companies',         'ASSET',     'SUB',  '1600', 'RECV', true,  'Other Assets'),
        ('1850', 'Goodwill on Consolidation',          'ASSET',     'MAIN', '1000', 'INVT', false, 'Goodwill'),
        ('2510', 'Due to Related Companies',           'LIABILITY', 'SUB',  '2500', 'PAYB', true,  'Accounts Payable and Accrued Expenses'),
        ('3450', 'Currency Translation Reserve',       'EQUITY',    'MAIN', '3000', 'CAPT', false, 'Currency Translation Reserve'),
        ('3600', 'Non-controlling Interest',           'EQUITY',    'MAIN', '3000', 'CAPT', false, 'Non-controlling Interest'))
     as a(code, name, cls, lvl, parent, cat, reval, grp)
where c.code = 'FVI';

-- ---------- Subsidiary company, branch and calendar ---------------------------------------
insert into org_company (code, name, base_currency, tax_id, address, fiscal_year_start_month,
    back_value_days, forward_value_days, retained_earnings_account, record_status,
    authorized_by, authorized_at, created_at, created_by)
values ('FVS', 'BDO Insurance and Reinsurance Brokers (Singapore) Pte. Ltd.', 'USD', '201912345K',
    '1 Raffles Place, Singapore 048616', 1, 45, 5, '3500', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM');

insert into org_branch (company_id, code, name, region, address, opening_date, head_office,
    forex_authorized, contact_phone, contact_email, manager_name, weekly_holidays,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, 'HO', 'Head Office - Singapore', 'Singapore', '1 Raffles Place, Singapore',
       date '2019-07-01', true, true, '+65 6000 1000', 'sg@brokerverse-seed.ph', 'Tan Wei Ming', '6,7',
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVS';

-- Chart of accounts: copy of the parent's chart (same codes = group chart of accounts).
insert into coa_account (company_id, code, name, short_name, account_class, level, parent_id, category_id,
    postable, control_account, sub_ledger_type, allow_manual_posting, cost_center_required,
    business_line_required, revaluation_required, reconcilable, inter_branch, contra_account_code,
    report_group, frozen, opened_on, record_status, authorized_by, authorized_at, created_at, created_by)
select t.id, s.code, s.name, s.short_name, s.account_class, s.level, null, s.category_id,
       s.postable, s.control_account, s.sub_ledger_type, s.allow_manual_posting, s.cost_center_required,
       s.business_line_required, false, s.reconcilable, s.inter_branch, s.contra_account_code,
       s.report_group, false, date '2019-07-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from coa_account s
join org_company f on f.id = s.company_id and f.code = 'FVI'
cross join org_company t
where t.code = 'FVS';

update coa_account t set parent_id = tp.id
from coa_account s
join org_company f on f.id = s.company_id and f.code = 'FVI'
join coa_account sp on sp.id = s.parent_id,
     coa_account tp, org_company sub
where sub.code = 'FVS'
  and t.company_id = sub.id and t.code = s.code
  and tp.company_id = sub.id and tp.code = sp.code;

update coa_account a set name = v.name
from org_company c,
(values ('1111', 'Cash in Bank - DBS Current Account (USD)'),
        ('1112', 'Cash in Bank - OCBC Savings Account (USD)'),
        ('1113', 'Cash in Bank - DBS Premium Collection Account (USD)')) as v(code, name)
where c.code = 'FVS' and a.company_id = c.id and a.code = v.code;

insert into dim_value (company_id, dimension_type, code, name, created_at, created_by)
select t.id, d.dimension_type, d.code, d.name, now(), 'SYSTEM'
from dim_value d
join org_company f on f.id = d.company_id and f.code = 'FVI'
cross join org_company t
where t.code = 'FVS';

-- Fiscal year 2026: January-September open, remainder future (same calendar as the parent).
insert into per_fiscal_year (company_id, year_code, start_date, end_date, status, created_at, created_by)
select id, 2026, date '2026-01-01', date '2026-12-31', 'OPEN', now(), 'SYSTEM'
from org_company where code = 'FVS';

insert into per_period (fiscal_year_id, company_id, period_no, name, start_date, end_date, status,
    created_at, created_by)
select y.id, y.company_id, m, to_char(make_date(2026, m, 1), 'YYYY-MM'), make_date(2026, m, 1),
       (make_date(2026, m, 1) + interval '1 month - 1 day')::date,
       case when m <= 9 then 'OPEN' else 'FUTURE' end, now(), 'SYSTEM'
from per_fiscal_year y
join org_company c on c.id = y.company_id and c.code = 'FVS',
     generate_series(1, 12) as m
where y.year_code = 2026;
