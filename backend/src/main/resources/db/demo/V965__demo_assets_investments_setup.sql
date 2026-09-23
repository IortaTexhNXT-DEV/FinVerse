-- =====================================================================================
-- DEMO DATA (demo profile only): fixed asset categories, investment portfolios, issuer /
-- depository parties and the accounting rules of the fixed asset and investment events
-- for the demo chart of accounts. Assets, holdings and their postings are created by the
-- demo data runners (FixedAssetDemoData, InvestmentDemoData) through the services.
-- =====================================================================================

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, a.code, a.name, a.cls, 'SUB', (select id from coa_account where code = a.parent),
    (select id from coa_category where code = a.cat), true,
    false, 'NONE', true, false, false, false, false, false, a.grp, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c,
(values ('1704', 'Furniture and Fixtures', 'ASSET', '1700', 'FIXA', 'Property and Equipment'),
        ('4504', 'Unrealized Fair Value Gain / (Loss) - FVPL', 'INCOME', '4500', 'INVI', 'Investment Income'))
     as a(code, name, cls, parent, cat, grp)
where c.code = 'FVI' and not exists (select 1 from coa_account x where x.company_id = c.id and x.code = a.code);

insert into fa_category (company_id, code, name, asset_account, accumulated_depreciation_account,
    depreciation_expense_account, depreciation_method, useful_life_months, residual_percent,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, k.code, k.name, k.asset, '1709', '5611', k.method, k.life, k.residual,
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c,
(values ('OFFEQ', 'Office Equipment',         '1701', 'STRAIGHT_LINE',     60, 5.0),
        ('ITEQ',  'IT Equipment',             '1702', 'DECLINING_BALANCE', 36, 0.0),
        ('VEH',   'Transportation Equipment', '1703', 'STRAIGHT_LINE',     60, 10.0),
        ('FURN',  'Furniture and Fixtures',   '1704', 'STRAIGHT_LINE',     84, 0.0))
     as k(code, name, asset, method, life, residual)
where c.code = 'FVI';

insert into inv_portfolio (company_id, code, name, classification, investment_account,
    accrued_interest_account, interest_income_account, realized_gain_account, fair_value_account,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, p.code, p.name, p.cls, p.inv, '1504', p.income, '4503', p.fv,
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c,
(values ('AC-GOVT',    'Government securities at amortized cost',        'AMORTIZED_COST', '1503', '4501', null),
        ('AC-TD',      'Time deposits and short-term placements',        'AMORTIZED_COST', '1120', '4501', null),
        ('SEC-DEP',    'Security deposit with the Insurance Commission', 'AMORTIZED_COST', '1505', '4501', null),
        ('FVOCI-DEBT', 'Debt securities at FVOCI',                       'FVOCI',          '1502', '4501', '3400'),
        ('FVPL-EQ',    'Listed equities at FVPL',                        'FVPL',           '1501', '4502', '4504'))
     as p(code, name, cls, inv, income, fv)
where c.code = 'FVI';

insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, p.code, p.name, p.t, p.tin, p.addr, p.email, p.phone, 'PHP', 0,
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('IS-BTR', 'Bureau of the Treasury (Demo)', 'BANK', '000-000-001-000', 'Intramuros, Manila', 'ross@treasury.example', '+63 2 5000 0001'),
  ('BK-BDO', 'BDO Unibank Inc. (Demo)', 'BANK', '000-000-002-000', 'Makati City', 'treasury@bdo.example', '+63 2 5000 0002'),
  ('BK-BPI', 'Bank of the Philippine Islands (Demo)', 'BANK', '000-000-003-000', 'Makati City', 'treasury@bpi.example', '+63 2 5000 0003'),
  ('BK-LBP', 'Land Bank of the Philippines (Demo)', 'BANK', '000-000-004-000', 'Manila', 'treasury@lbp.example', '+63 2 5000 0004'),
  ('IS-ALI', 'Ayala Land Inc. (Demo issuer)', 'CORPORATE_CLIENT', '000-000-005-000', 'Makati City', 'ir@ali.example', '+63 2 5000 0005'),
  ('IS-SMIC', 'SM Investments Corporation (Demo issuer)', 'CORPORATE_CLIENT', '000-000-006-000', 'Pasay City', 'ir@smic.example', '+63 2 5000 0006')
) as p(code, name, t, tin, addr, email, phone)
where c.code = 'FVI' and not exists (select 1 from pty_party x where x.company_id = c.id and x.code = p.code);

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c,
(values ('ASSET_ACQUISITION', 'Standard asset acquisition'),
        ('ASSET_TAKE_ON', 'Standard asset opening balance'),
        ('ASSET_DEPRECIATION', 'Standard depreciation'),
        ('ASSET_DISPOSAL', 'Standard asset disposal'),
        ('ASSET_TRANSFER', 'Standard inter-branch asset transfer'),
        ('INVESTMENT_PURCHASE', 'Standard investment purchase'),
        ('INVESTMENT_TAKE_ON', 'Standard investment opening balance'),
        ('INVESTMENT_INTEREST_ACCRUAL', 'Standard interest accrual'),
        ('INVESTMENT_AMORTIZATION', 'Standard premium / discount amortization'),
        ('INVESTMENT_INTEREST_RECEIPT', 'Standard coupon receipt'),
        ('INVESTMENT_MATURITY', 'Standard investment maturity'),
        ('INVESTMENT_SALE', 'Standard investment sale'),
        ('INVESTMENT_FAIR_VALUE', 'Standard fair value remeasurement')) as e(code, name)
where c.code = 'FVI';

-- Disposal gain to Other Income (4700), loss to Miscellaneous Expenses (5613); asset opening
-- balances against Retained Earnings (3500); transfers through Inter-branch Clearing (1605);
-- final tax withheld on interest reduces interest income (income shown net of final tax).
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party
from acc_rule r join org_company c on c.id = r.company_id and c.code = 'FVI'
join (values
  ('ASSET_ACQUISITION', 1, 'DEBIT', '@ASSET', 'COST', false),
  ('ASSET_ACQUISITION', 2, 'CREDIT', '@SETTLEMENT', 'COST', true),
  ('ASSET_TAKE_ON', 1, 'DEBIT', '@ASSET', 'COST', false),
  ('ASSET_TAKE_ON', 2, 'CREDIT', '@ACCUM_DEPRECIATION', 'ACCUMULATED_DEPRECIATION', false),
  ('ASSET_TAKE_ON', 3, 'CREDIT', '3500', 'NET_BOOK_VALUE', false),
  ('ASSET_DEPRECIATION', 1, 'DEBIT', '@DEPRECIATION_EXPENSE', 'DEPRECIATION', false),
  ('ASSET_DEPRECIATION', 2, 'CREDIT', '@ACCUM_DEPRECIATION', 'DEPRECIATION', false),
  ('ASSET_DISPOSAL', 1, 'DEBIT', '@ACCUM_DEPRECIATION', 'ACCUMULATED_DEPRECIATION', false),
  ('ASSET_DISPOSAL', 2, 'DEBIT', '@BANK', 'PROCEEDS', false),
  ('ASSET_DISPOSAL', 3, 'DEBIT', '5613', 'LOSS', false),
  ('ASSET_DISPOSAL', 4, 'CREDIT', '@ASSET', 'COST', false),
  ('ASSET_DISPOSAL', 5, 'CREDIT', '4700', 'GAIN', false),
  ('ASSET_TRANSFER', 1, 'DEBIT', '@ASSET', 'COST', false),
  ('ASSET_TRANSFER', 2, 'CREDIT', '@ACCUM_DEPRECIATION', 'ACCUMULATED_DEPRECIATION', false),
  ('ASSET_TRANSFER', 3, 'CREDIT', '1605', 'NET_BOOK_VALUE', false),
  ('INVESTMENT_PURCHASE', 1, 'DEBIT', '@INVESTMENT', 'COST', false),
  ('INVESTMENT_PURCHASE', 2, 'DEBIT', '@ACCRUED_INTEREST', 'PURCHASED_INTEREST', false),
  ('INVESTMENT_PURCHASE', 3, 'CREDIT', '@BANK', 'TOTAL', false),
  ('INVESTMENT_TAKE_ON', 1, 'DEBIT', '@INVESTMENT', 'CARRYING_AMOUNT', false),
  ('INVESTMENT_TAKE_ON', 2, 'DEBIT', '@ACCRUED_INTEREST', 'ACCRUED_INTEREST', false),
  ('INVESTMENT_TAKE_ON', 3, 'CREDIT', '3500', 'TOTAL', false),
  ('INVESTMENT_INTEREST_ACCRUAL', 1, 'DEBIT', '@ACCRUED_INTEREST', 'INTEREST', false),
  ('INVESTMENT_INTEREST_ACCRUAL', 2, 'CREDIT', '@INTEREST_INCOME', 'INTEREST', false),
  ('INVESTMENT_AMORTIZATION', 1, 'DEBIT', '@INVESTMENT', 'AMORTIZATION', false),
  ('INVESTMENT_AMORTIZATION', 2, 'CREDIT', '@INTEREST_INCOME', 'AMORTIZATION', false),
  ('INVESTMENT_INTEREST_RECEIPT', 1, 'DEBIT', '@BANK', 'CASH', false),
  ('INVESTMENT_INTEREST_RECEIPT', 2, 'DEBIT', '@INTEREST_INCOME', 'FINAL_TAX', false),
  ('INVESTMENT_INTEREST_RECEIPT', 3, 'CREDIT', '@ACCRUED_INTEREST', 'ACCRUED_INTEREST', false),
  ('INVESTMENT_INTEREST_RECEIPT', 4, 'CREDIT', '@INTEREST_INCOME', 'INCOME_ADJUSTMENT', false),
  ('INVESTMENT_MATURITY', 1, 'DEBIT', '@BANK', 'PROCEEDS', false),
  ('INVESTMENT_MATURITY', 2, 'DEBIT', '@INTEREST_INCOME', 'FINAL_TAX', false),
  ('INVESTMENT_MATURITY', 3, 'DEBIT', '@FAIR_VALUE', 'FV_RESERVE', false),
  ('INVESTMENT_MATURITY', 4, 'CREDIT', '@INVESTMENT', 'CARRYING_AMOUNT', false),
  ('INVESTMENT_MATURITY', 5, 'CREDIT', '@ACCRUED_INTEREST', 'ACCRUED_INTEREST', false),
  ('INVESTMENT_MATURITY', 6, 'CREDIT', '@REALIZED_GAIN', 'REALIZED_GAIN', false),
  ('INVESTMENT_SALE', 1, 'DEBIT', '@BANK', 'PROCEEDS', false),
  ('INVESTMENT_SALE', 2, 'DEBIT', '@INTEREST_INCOME', 'FINAL_TAX', false),
  ('INVESTMENT_SALE', 3, 'DEBIT', '@FAIR_VALUE', 'FV_RESERVE', false),
  ('INVESTMENT_SALE', 4, 'CREDIT', '@INVESTMENT', 'CARRYING_AMOUNT', false),
  ('INVESTMENT_SALE', 5, 'CREDIT', '@ACCRUED_INTEREST', 'ACCRUED_INTEREST', false),
  ('INVESTMENT_SALE', 6, 'CREDIT', '@REALIZED_GAIN', 'REALIZED_GAIN', false),
  ('INVESTMENT_FAIR_VALUE', 1, 'DEBIT', '@INVESTMENT', 'FAIR_VALUE_CHANGE', false),
  ('INVESTMENT_FAIR_VALUE', 2, 'CREDIT', '@FAIR_VALUE', 'FAIR_VALUE_CHANGE', false)
) as l(ev, n, side, acc, comp, party) on l.ev = r.event_type;
