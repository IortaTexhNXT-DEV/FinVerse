-- =====================================================================================
-- iNXT BrokerVerse - SEED DATA (loaded only with the 'seed' Spring profile).
-- Fictitious company "BDO Insurance and Reinsurance Brokers, Inc." with three branches,
-- a Philippine non-life insurance chart of accounts, dimensions, FY 2026 calendar,
-- exchange rates and SIT/UAT users (password: Brokerverse@2026 - seed only).
-- =====================================================================================
insert into org_company (code, name, base_currency, tax_id, address, fiscal_year_start_month,
    back_value_days, forward_value_days, retained_earnings_account, record_status,
    authorized_by, authorized_at, created_at, created_by)
values ('FVI', 'BDO Insurance and Reinsurance Brokers, Inc.', 'PHP', '000-123-456-000',
    '8th Floor, Ayala Avenue, Makati City, Metro Manila', 1, 45, 5, '3500',
    'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM');

insert into org_branch (company_id, code, name, region, address, opening_date, head_office,
    forex_authorized, contact_phone, contact_email, manager_name, weekly_holidays,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, b.code, b.name, b.region, b.address, date '2010-01-04', b.ho, b.fx, b.phone,
       b.email, b.mgr, '6,7', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c,
(values ('HO',  'Head Office - Makati', 'NCR',      'Ayala Avenue, Makati City',        true,  true,  '+63 2 8888 0001', 'ho@brokerverse-seed.ph',   'Maria Santos'),
        ('CEB', 'Cebu Branch',          'Visayas',  'Cebu Business Park, Cebu City',    false, true,  '+63 32 888 0002', 'cebu@brokerverse-seed.ph', 'Jose Reyes'),
        ('DVO', 'Davao Branch',         'Mindanao', 'J.P. Laurel Avenue, Davao City',   false, false, '+63 82 888 0003', 'davao@brokerverse-seed.ph','Ana Cruz'))
     as b(code, name, region, address, ho, fx, phone, email, mgr)
where c.code = 'FVI';

insert into org_holiday (company_id, branch_id, holiday_date, description, created_at, created_by)
select c.id, null, h.d, h.n, now(), 'SYSTEM' from org_company c,
(values (date '2026-01-01','New Year''s Day'), (date '2026-04-02','Maundy Thursday'),
        (date '2026-04-03','Good Friday'), (date '2026-04-09','Araw ng Kagitingan'),
        (date '2026-05-01','Labor Day'), (date '2026-06-12','Independence Day'),
        (date '2026-08-31','National Heroes Day'), (date '2026-11-30','Bonifacio Day'),
        (date '2026-12-25','Christmas Day'), (date '2026-12-30','Rizal Day')) as h(d, n)
where c.code = 'FVI';

insert into coa_category (code, name, account_class, bank_category, created_at, created_by) values
    ('CASH', 'Cash and Bank',            'ASSET',     true,  now(), 'SYSTEM'),
    ('BANK', 'Bank Accounts',            'ASSET',     true,  now(), 'SYSTEM'),
    ('RECV', 'Receivables',              'ASSET',     false, now(), 'SYSTEM'),
    ('INVT', 'Investments',              'ASSET',     false, now(), 'SYSTEM'),
    ('FIXA', 'Fixed Assets',             'ASSET',     false, now(), 'SYSTEM'),
    ('TECH', 'Technical Reserves',       'LIABILITY', false, now(), 'SYSTEM'),
    ('PAYB', 'Payables',                 'LIABILITY', false, now(), 'SYSTEM'),
    ('TAXP', 'Taxes Payable',            'LIABILITY', false, now(), 'SYSTEM'),
    ('CAPT', 'Capital',                  'EQUITY',    false, now(), 'SYSTEM'),
    ('PREM', 'Premium Income',           'INCOME',    false, now(), 'SYSTEM'),
    ('INVI', 'Investment Income',        'INCOME',    false, now(), 'SYSTEM'),
    ('CLMS', 'Claims Expense',           'EXPENSE',   false, now(), 'SYSTEM'),
    ('ACQC', 'Acquisition Costs',        'EXPENSE',   false, now(), 'SYSTEM'),
    ('OPEX', 'Operating Expenses',       'EXPENSE',   false, now(), 'SYSTEM'),
    ('MEMO', 'Memorandum',               'MEMORANDUM',false, now(), 'SYSTEM');

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1000', 'ASSETS', 'ASSET', 'GROUP', null, (select id from coa_category where code = 'RECV'), false,
    false, 'NONE', true, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1100', 'Cash and Cash Equivalents', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'CASH'), false,
    false, 'NONE', true, false, false, false, false, false, 'Cash and Cash Equivalents', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1101', 'Cash on Hand', 'ASSET', 'SUB', (select id from coa_account where code = '1100'), (select id from coa_category where code = 'CASH'), true,
    false, 'NONE', true, false, false, false, false, false, 'Cash and Cash Equivalents', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1102', 'Petty Cash Fund', 'ASSET', 'SUB', (select id from coa_account where code = '1100'), (select id from coa_category where code = 'CASH'), true,
    false, 'NONE', true, false, false, false, false, false, 'Cash and Cash Equivalents', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1111', 'Cash in Bank - BDO Current Account (PHP)', 'ASSET', 'SUB', (select id from coa_account where code = '1100'), (select id from coa_category where code = 'BANK'), true,
    false, 'NONE', true, false, false, false, true, false, 'Cash and Cash Equivalents', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1112', 'Cash in Bank - BPI Savings Account (PHP)', 'ASSET', 'SUB', (select id from coa_account where code = '1100'), (select id from coa_category where code = 'BANK'), true,
    false, 'NONE', true, false, false, false, true, false, 'Cash and Cash Equivalents', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1113', 'Cash in Bank - BDO Dollar Account (USD)', 'ASSET', 'SUB', (select id from coa_account where code = '1100'), (select id from coa_category where code = 'BANK'), true,
    false, 'NONE', true, false, false, true, true, false, 'Cash and Cash Equivalents', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account_currency (account_id, currency_code) select id, 'USD' from coa_account where code = '1113';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1120', 'Short-term Placements', 'ASSET', 'SUB', (select id from coa_account where code = '1100'), (select id from coa_category where code = 'CASH'), true,
    false, 'NONE', true, false, false, false, false, false, 'Cash and Cash Equivalents', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1200', 'Insurance Receivables', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'RECV'), false,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1201', 'Premiums Receivable - Policyholders', 'ASSET', 'SUB', (select id from coa_account where code = '1200'), (select id from coa_category where code = 'RECV'), true,
    true, 'POLICYHOLDER', false, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1202', 'Premiums Receivable - Agents and Brokers', 'ASSET', 'SUB', (select id from coa_account where code = '1200'), (select id from coa_category where code = 'RECV'), true,
    true, 'INTERMEDIARY', false, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1203', 'Due from Ceding Companies', 'ASSET', 'SUB', (select id from coa_account where code = '1200'), (select id from coa_category where code = 'RECV'), true,
    true, 'REINSURER', true, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1204', 'Funds Held by Ceding Companies', 'ASSET', 'SUB', (select id from coa_account where code = '1200'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1205', 'Amounts Recoverable from Reinsurers', 'ASSET', 'SUB', (select id from coa_account where code = '1200'), (select id from coa_category where code = 'RECV'), true,
    true, 'REINSURER', false, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1206', 'Due from Coinsurers', 'ASSET', 'SUB', (select id from coa_account where code = '1200'), (select id from coa_category where code = 'RECV'), true,
    true, 'COINSURER', true, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1209', 'Allowance for Impairment - Insurance Receivables', 'ASSET', 'SUB', (select id from coa_account where code = '1200'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Receivables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1300', 'Reinsurance Assets', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'RECV'), false,
    false, 'NONE', true, false, false, false, false, false, 'Reinsurance Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1301', 'Reinsurers Share of Unearned Premiums', 'ASSET', 'SUB', (select id from coa_account where code = '1300'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', false, false, false, false, false, false, 'Reinsurance Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1302', 'Reinsurers Share of Claims Reserves', 'ASSET', 'SUB', (select id from coa_account where code = '1300'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', false, false, false, false, false, false, 'Reinsurance Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1303', 'Reinsurers Share of IBNR', 'ASSET', 'SUB', (select id from coa_account where code = '1300'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', false, false, false, false, false, false, 'Reinsurance Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1400', 'Deferred Acquisition Costs', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', false, false, false, false, false, false, 'Deferred Acquisition Costs', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1500', 'Financial Assets', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'INVT'), false,
    false, 'NONE', true, false, false, false, false, false, 'Financial Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1501', 'Financial Assets at FVPL', 'ASSET', 'SUB', (select id from coa_account where code = '1500'), (select id from coa_category where code = 'INVT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Financial Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1502', 'Financial Assets at FVOCI', 'ASSET', 'SUB', (select id from coa_account where code = '1500'), (select id from coa_category where code = 'INVT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Financial Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1503', 'Financial Assets at Amortized Cost - Government Securities', 'ASSET', 'SUB', (select id from coa_account where code = '1500'), (select id from coa_category where code = 'INVT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Financial Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1504', 'Accrued Interest Receivable', 'ASSET', 'SUB', (select id from coa_account where code = '1500'), (select id from coa_category where code = 'INVT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Financial Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1505', 'Security Fund Deposit', 'ASSET', 'SUB', (select id from coa_account where code = '1500'), (select id from coa_category where code = 'INVT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Financial Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1600', 'Other Assets', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'RECV'), false,
    false, 'NONE', true, false, false, false, false, false, 'Other Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1601', 'Prepaid Expenses', 'ASSET', 'SUB', (select id from coa_account where code = '1600'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, false, false, 'Other Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1602', 'Creditable Withholding Tax', 'ASSET', 'SUB', (select id from coa_account where code = '1600'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, false, false, 'Other Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1603', 'Input VAT', 'ASSET', 'SUB', (select id from coa_account where code = '1600'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, false, false, 'Other Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1604', 'Advances to Employees', 'ASSET', 'SUB', (select id from coa_account where code = '1600'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, true, false, false, false, false, 'Other Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1605', 'Inter-branch Clearing', 'ASSET', 'SUB', (select id from coa_account where code = '1600'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, false, true, 'Other Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1606', 'Suspense Account', 'ASSET', 'SUB', (select id from coa_account where code = '1600'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, true, false, 'Other Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1700', 'Property and Equipment', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'FIXA'), false,
    false, 'NONE', true, false, false, false, false, false, 'Property and Equipment', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1701', 'Office Equipment', 'ASSET', 'SUB', (select id from coa_account where code = '1700'), (select id from coa_category where code = 'FIXA'), true,
    false, 'NONE', true, false, false, false, false, false, 'Property and Equipment', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1702', 'IT Equipment', 'ASSET', 'SUB', (select id from coa_account where code = '1700'), (select id from coa_category where code = 'FIXA'), true,
    false, 'NONE', true, false, false, false, false, false, 'Property and Equipment', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1703', 'Transportation Equipment', 'ASSET', 'SUB', (select id from coa_account where code = '1700'), (select id from coa_category where code = 'FIXA'), true,
    false, 'NONE', true, false, false, false, false, false, 'Property and Equipment', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1709', 'Accumulated Depreciation', 'ASSET', 'SUB', (select id from coa_account where code = '1700'), (select id from coa_category where code = 'FIXA'), true,
    false, 'NONE', true, false, false, false, false, false, 'Property and Equipment', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '1800', 'Right-of-use Assets', 'ASSET', 'MAIN', (select id from coa_account where code = '1000'), (select id from coa_category where code = 'RECV'), true,
    false, 'NONE', true, false, false, false, false, false, 'Right-of-use Assets', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2000', 'LIABILITIES', 'LIABILITY', 'GROUP', null, (select id from coa_category where code = 'PAYB'), false,
    false, 'NONE', true, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2100', 'Insurance Contract Liabilities', 'LIABILITY', 'MAIN', (select id from coa_account where code = '2000'), (select id from coa_category where code = 'TECH'), false,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Contract Liabilities', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2101', 'Reserve for Unearned Premiums', 'LIABILITY', 'SUB', (select id from coa_account where code = '2100'), (select id from coa_category where code = 'TECH'), true,
    false, 'NONE', false, false, false, false, false, false, 'Insurance Contract Liabilities', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2102', 'Claims Reserve - Outstanding Claims', 'LIABILITY', 'SUB', (select id from coa_account where code = '2100'), (select id from coa_category where code = 'TECH'), true,
    false, 'NONE', false, false, false, false, false, false, 'Insurance Contract Liabilities', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2103', 'Claims Reserve - IBNR', 'LIABILITY', 'SUB', (select id from coa_account where code = '2100'), (select id from coa_category where code = 'TECH'), true,
    false, 'NONE', false, false, false, false, false, false, 'Insurance Contract Liabilities', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2104', 'Premium Deficiency Reserve', 'LIABILITY', 'SUB', (select id from coa_account where code = '2100'), (select id from coa_category where code = 'TECH'), true,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Contract Liabilities', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2200', 'Insurance Payables', 'LIABILITY', 'MAIN', (select id from coa_account where code = '2000'), (select id from coa_category where code = 'PAYB'), false,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Payables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2201', 'Due to Reinsurers', 'LIABILITY', 'SUB', (select id from coa_account where code = '2200'), (select id from coa_category where code = 'PAYB'), true,
    true, 'REINSURER', false, false, false, false, false, false, 'Insurance Payables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2202', 'Funds Held for Reinsurers', 'LIABILITY', 'SUB', (select id from coa_account where code = '2200'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Payables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2203', 'Due to Coinsurers', 'LIABILITY', 'SUB', (select id from coa_account where code = '2200'), (select id from coa_category where code = 'PAYB'), true,
    true, 'COINSURER', true, false, false, false, false, false, 'Insurance Payables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2204', 'Claims Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2200'), (select id from coa_category where code = 'PAYB'), true,
    true, 'POLICYHOLDER', false, false, false, false, false, false, 'Insurance Payables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2205', 'Premium Deposits / Unapplied Collections', 'LIABILITY', 'SUB', (select id from coa_account where code = '2200'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', true, false, false, false, true, false, 'Insurance Payables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2206', 'Return Premiums Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2200'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', true, false, false, false, false, false, 'Insurance Payables', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2300', 'Commissions Payable', 'LIABILITY', 'MAIN', (select id from coa_account where code = '2000'), (select id from coa_category where code = 'PAYB'), true,
    true, 'INTERMEDIARY', false, false, false, false, false, false, 'Commissions Payable', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2400', 'Deferred Reinsurance Commissions', 'LIABILITY', 'MAIN', (select id from coa_account where code = '2000'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', false, false, false, false, false, false, 'Deferred Reinsurance Commissions', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2500', 'Accounts Payable and Accrued Expenses', 'LIABILITY', 'MAIN', (select id from coa_account where code = '2000'), (select id from coa_category where code = 'PAYB'), false,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2501', 'Accounts Payable - Suppliers', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'PAYB'), true,
    true, 'VENDOR', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2502', 'Accrued Expenses', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2503', 'Documentary Stamp Tax Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'TAXP'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2504', 'Output VAT Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'TAXP'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2505', 'Local Government Tax Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'TAXP'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2506', 'Fire Service Tax Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'TAXP'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2507', 'Premium Tax Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'TAXP'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2508', 'Expanded Withholding Tax Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'TAXP'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2509', 'Income Tax Payable', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'TAXP'), true,
    false, 'NONE', true, false, false, false, false, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2600', 'Lease Liabilities', 'LIABILITY', 'MAIN', (select id from coa_account where code = '2000'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', true, false, false, false, false, false, 'Lease Liabilities', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2700', 'Retirement Benefit Obligation', 'LIABILITY', 'MAIN', (select id from coa_account where code = '2000'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', true, false, false, false, false, false, 'Retirement Benefit Obligation', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '3000', 'EQUITY', 'EQUITY', 'GROUP', null, (select id from coa_category where code = 'CAPT'), false,
    false, 'NONE', true, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '3100', 'Capital Stock', 'EQUITY', 'MAIN', (select id from coa_account where code = '3000'), (select id from coa_category where code = 'CAPT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Capital Stock', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '3200', 'Contributed Surplus', 'EQUITY', 'MAIN', (select id from coa_account where code = '3000'), (select id from coa_category where code = 'CAPT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Contributed Surplus', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '3300', 'Contingency Surplus', 'EQUITY', 'MAIN', (select id from coa_account where code = '3000'), (select id from coa_category where code = 'CAPT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Contingency Surplus', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '3400', 'Revaluation Reserve on FVOCI', 'EQUITY', 'MAIN', (select id from coa_account where code = '3000'), (select id from coa_category where code = 'CAPT'), true,
    false, 'NONE', true, false, false, false, false, false, 'Reserves', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '3500', 'Retained Earnings', 'EQUITY', 'MAIN', (select id from coa_account where code = '3000'), (select id from coa_category where code = 'CAPT'), true,
    false, 'NONE', false, false, false, false, false, false, 'Retained Earnings', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4000', 'INCOME', 'INCOME', 'GROUP', null, (select id from coa_category where code = 'PREM'), false,
    false, 'NONE', true, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4100', 'Gross Premiums Written', 'INCOME', 'MAIN', (select id from coa_account where code = '4000'), (select id from coa_category where code = 'PREM'), true,
    false, 'NONE', false, false, true, false, false, false, 'Gross Premiums Written', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4200', 'Reinsurance Premiums Ceded', 'INCOME', 'MAIN', (select id from coa_account where code = '4000'), (select id from coa_category where code = 'PREM'), true,
    false, 'NONE', false, false, true, false, false, false, 'Reinsurance Premiums Ceded', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4300', 'Decrease / (Increase) in Unearned Premiums', 'INCOME', 'MAIN', (select id from coa_account where code = '4000'), (select id from coa_category where code = 'PREM'), true,
    false, 'NONE', false, false, false, false, false, false, 'Change in Unearned Premiums', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4400', 'Commission Income', 'INCOME', 'MAIN', (select id from coa_account where code = '4000'), (select id from coa_category where code = 'PREM'), true,
    false, 'NONE', true, false, true, false, false, false, 'Commission Income', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4500', 'Investment Income', 'INCOME', 'MAIN', (select id from coa_account where code = '4000'), (select id from coa_category where code = 'INVI'), false,
    false, 'NONE', true, false, false, false, false, false, 'Investment and Other Income', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4501', 'Interest Income', 'INCOME', 'SUB', (select id from coa_account where code = '4500'), (select id from coa_category where code = 'INVI'), true,
    false, 'NONE', true, false, false, false, false, false, 'Investment and Other Income', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4502', 'Dividend Income', 'INCOME', 'SUB', (select id from coa_account where code = '4500'), (select id from coa_category where code = 'INVI'), true,
    false, 'NONE', true, false, false, false, false, false, 'Investment and Other Income', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4503', 'Realized Gain on Sale of Investments', 'INCOME', 'SUB', (select id from coa_account where code = '4500'), (select id from coa_category where code = 'INVI'), true,
    false, 'NONE', true, false, false, false, false, false, 'Investment and Other Income', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4600', 'Foreign Exchange Gain / (Loss)', 'INCOME', 'MAIN', (select id from coa_account where code = '4000'), (select id from coa_category where code = 'PREM'), false,
    false, 'NONE', true, false, false, false, false, false, 'Foreign Exchange Gain / (Loss)', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4601', 'Realized Foreign Exchange Gain / (Loss)', 'INCOME', 'SUB', (select id from coa_account where code = '4600'), (select id from coa_category where code = 'PREM'), true,
    false, 'NONE', true, false, false, false, false, false, 'Foreign Exchange Gain / (Loss)', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4602', 'Unrealized Foreign Exchange Gain / (Loss)', 'INCOME', 'SUB', (select id from coa_account where code = '4600'), (select id from coa_category where code = 'PREM'), true,
    false, 'NONE', false, false, false, false, false, false, 'Foreign Exchange Gain / (Loss)', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4700', 'Other Income', 'INCOME', 'MAIN', (select id from coa_account where code = '4000'), (select id from coa_category where code = 'PREM'), true,
    false, 'NONE', true, false, false, false, false, false, 'Investment and Other Income', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5000', 'EXPENSES', 'EXPENSE', 'GROUP', null, (select id from coa_category where code = 'OPEX'), false,
    false, 'NONE', true, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5100', 'Gross Claims and Losses Paid', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'CLMS'), true,
    false, 'NONE', false, false, true, false, false, false, 'Gross Claims Incurred', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5200', 'Change in Claims Reserves', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'CLMS'), true,
    false, 'NONE', false, false, true, false, false, false, 'Gross Claims Incurred', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5300', 'Reinsurers Share of Claims', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'CLMS'), true,
    false, 'NONE', false, false, true, false, false, false, 'Reinsurers Share of Claims', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5400', 'Commission Expense', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'ACQC'), true,
    false, 'NONE', false, false, true, false, false, false, 'Commission Expense', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5500', 'Change in Deferred Acquisition Costs', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'ACQC'), true,
    false, 'NONE', false, false, false, false, false, false, 'Commission Expense', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5600', 'General and Administrative Expenses', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'OPEX'), false,
    false, 'NONE', true, false, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5601', 'Salaries and Wages', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5602', 'Employee Benefits', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5603', 'Rent and Utilities', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5604', 'Communication', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5605', 'Professional Fees', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5606', 'Transportation and Travel', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5607', 'Repairs and Maintenance', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5608', 'Taxes and Licenses', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5609', 'Advertising and Promotion', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5610', 'IT and Software', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5611', 'Depreciation', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5612', 'Bank Charges', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5613', 'Miscellaneous Expenses', 'EXPENSE', 'SUB', (select id from coa_account where code = '5600'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, true, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5700', 'Provision for Impairment Losses', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, false, false, false, false, false, 'General and Administrative Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '5800', 'Income Tax Expense', 'EXPENSE', 'MAIN', (select id from coa_account where code = '5000'), (select id from coa_category where code = 'OPEX'), true,
    false, 'NONE', true, false, false, false, false, false, 'Income Tax Expense', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '9000', 'MEMORANDUM ACCOUNTS', 'MEMORANDUM', 'GROUP', null, (select id from coa_category where code = 'MEMO'), false,
    false, 'NONE', true, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '9100', 'Sums Insured in Force', 'MEMORANDUM', 'MAIN', (select id from coa_account where code = '9000'), (select id from coa_category where code = 'MEMO'), true,
    false, 'NONE', false, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '9200', 'Sums Insured in Force - Contra', 'MEMORANDUM', 'MAIN', (select id from coa_account where code = '9000'), (select id from coa_category where code = 'MEMO'), true,
    false, 'NONE', false, false, false, false, false, false, null, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';

insert into dim_value (company_id, dimension_type, code, name, created_at, created_by)
select c.id, d.t, d.code, d.name, now(), 'SYSTEM' from org_company c,
(values ('COST_CENTER', 'EXEC', 'Executive Office'), ('COST_CENTER', 'FIN', 'Finance and Accounting'),
        ('COST_CENTER', 'UW', 'Underwriting'), ('COST_CENTER', 'CLM', 'Claims'),
        ('COST_CENTER', 'RI', 'Reinsurance'), ('COST_CENTER', 'IT', 'Information Technology'),
        ('COST_CENTER', 'HR', 'Human Resources'), ('COST_CENTER', 'MKT', 'Sales and Marketing'),
        ('BUSINESS_LINE', 'FIRE', 'Fire and Allied Perils'), ('BUSINESS_LINE', 'MOTOR', 'Motor Car'),
        ('BUSINESS_LINE', 'MARINE', 'Marine Cargo and Hull'), ('BUSINESS_LINE', 'ENGG', 'Engineering'),
        ('BUSINESS_LINE', 'CASUALTY', 'Casualty and Liability'), ('BUSINESS_LINE', 'PA', 'Personal Accident'),
        ('BUSINESS_LINE', 'HEALTH', 'Health'), ('BUSINESS_LINE', 'BONDS', 'Surety Bonds'),
        ('DEPARTMENT', 'UWD', 'Underwriting Department'), ('DEPARTMENT', 'CLD', 'Claims Department'),
        ('DEPARTMENT', 'RID', 'Reinsurance Department'), ('DEPARTMENT', 'FND', 'Finance Department'))
     as d(t, code, name)
where c.code = 'FVI';

-- Fiscal year 2026 (calendar year): January-September open, remainder future.
insert into per_fiscal_year (company_id, year_code, start_date, end_date, status, created_at, created_by)
select id, 2026, date '2026-01-01', date '2026-12-31', 'OPEN', now(), 'SYSTEM' from org_company where code = 'FVI';

insert into per_period (fiscal_year_id, company_id, period_no, name, start_date, end_date, status,
    created_at, created_by)
select y.id, y.company_id, m, to_char(make_date(2026, m, 1), 'YYYY-MM'), make_date(2026, m, 1),
       (make_date(2026, m, 1) + interval '1 month - 1 day')::date,
       case when m <= 9 then 'OPEN' else 'FUTURE' end, now(), 'SYSTEM'
from per_fiscal_year y, generate_series(1, 12) as m
where y.year_code = 2026;

insert into cur_exchange_rate (currency_code, rate_type, effective_date, rate, created_at, created_by)
select r.ccy, t.rt, d::date, round((r.base * (1 + 0.004 * sin(extract(doy from d) / 9.0)))::numeric, 6), now(), 'SYSTEM'
from (values ('USD', 57.85), ('EUR', 62.40), ('GBP', 73.10), ('JPY', 0.3850), ('SGD', 43.20),
             ('HKD', 7.41), ('AUD', 37.60), ('CNY', 8.02), ('INR', 0.6890)) as r(ccy, base),
     (values ('SPOT'), ('CLOSING')) as t(rt),
     generate_series(date '2026-01-01', date '2026-09-30', interval '1 day') as d;

-- SIT/UAT users (password for all: Brokerverse@2026). SEED DATA ONLY - never load in production.
insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', u.lim,
       (select id from org_branch where code = 'HO'), now(), 'SYSTEM'
from (values ('admin',      'SIT System Administrator', 'admin@brokerverse-seed.ph',      null::numeric),
             ('fmanager',   'Fiona Manager',             'fmanager@brokerverse-seed.ph',   null),
             ('accountant', 'Andy Accountant',           'accountant@brokerverse-seed.ph', null),
             ('checker',    'Carla Checker',             'checker@brokerverse-seed.ph',    5000000),
             ('uw',         'Uriel Underwriter',         'uw@brokerverse-seed.ph',         null),
             ('claims',     'Clara Claims',              'claims@brokerverse-seed.ph',     null),
             ('reinsurer',  'Ramon Reinsurance',         'ri@brokerverse-seed.ph',         null),
             ('auditor',    'Audrey Auditor',            'auditor@brokerverse-seed.ph',    null))
     as u(username, full_name, email, lim);

insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u join sec_role r on r.code = case u.username
    when 'admin' then 'SYSADMIN' when 'fmanager' then 'FIN_MANAGER'
    when 'accountant' then 'ACCOUNTANT' when 'checker' then 'AUTHORIZER'
    when 'uw' then 'UNDERWRITER' when 'claims' then 'CLAIMS_OFFICER'
    when 'reinsurer' then 'RI_OFFICER' when 'auditor' then 'AUDITOR' end;
-- Finance manager also administers finance masters.
insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u, sec_role r where u.username = 'fmanager' and r.code = 'FIN_ADMIN';

