-- =====================================================================================
-- DEMO DATA (demo profile only): MIS statement formats for the demo chart of accounts,
-- in the layout of the Insurance Commission annual statement (schedules numbered).
-- =====================================================================================
insert into fin_statement_format (company_id, code, description, statement_type, created_at, created_by)
select id, 'IC-BS', 'Balance Sheet - Insurance Commission layout', 'BS', now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into fin_statement_format (company_id, code, description, statement_type, created_at, created_by)
select id, 'IC-IE', 'Income and Expense Statement - Insurance Commission layout', 'IE', now(), 'SYSTEM'
from org_company where code = 'FVI';

insert into fin_statement_format_line (format_id, line_no, caption, line_type, schedule_ref,
    account_from, account_to, sign, total_terms)
select f.id, l.n, l.caption, l.t, l.sch, l.af, l.at, l.sign, l.terms
from fin_statement_format f, (values
  (10,  'ASSETS',                                'HEADING',  null, null,   null,   1,  null),
  (20,  'Cash and Cash Equivalents',             'ACCOUNTS', '1',  '1100', '1199', 1,  null),
  (30,  'Insurance Receivables',                 'ACCOUNTS', '2',  '1200', '1299', 1,  null),
  (40,  'Reinsurance Assets',                    'ACCOUNTS', '3',  '1300', '1399', 1,  null),
  (50,  'Deferred Acquisition Costs',            'ACCOUNTS', '4',  '1400', '1499', 1,  null),
  (60,  'Investments',                           'ACCOUNTS', '5',  '1500', '1599', 1,  null),
  (70,  'Other Assets',                          'ACCOUNTS', '6',  '1600', '1699', 1,  null),
  (80,  'Property, Equipment and ROU Assets',    'ACCOUNTS', '7',  '1700', '1899', 1,  null),
  (90,  'TOTAL ASSETS',                          'TOTAL',    null, null,   null,   1,  '20,30,40,50,60,70,80'),
  (100, 'LIABILITIES AND PROVISIONS',            'HEADING',  null, null,   null,   1,  null),
  (110, 'Unearned Premium, Outstanding Losses and IBNR Reserves', 'ACCOUNTS', '8', '2100', '2199', -1, null),
  (120, 'Insurance Payables',                    'ACCOUNTS', '9',  '2200', '2299', -1, null),
  (130, 'Commissions and Deferred RI Commissions', 'ACCOUNTS', '10', '2300', '2499', -1, null),
  (140, 'Creditors and Accrued Expenses',        'ACCOUNTS', '11', '2500', '2599', -1, null),
  (150, 'Other Liabilities',                     'ACCOUNTS', '12', '2600', '2799', -1, null),
  (160, 'TOTAL LIABILITIES AND PROVISIONS',      'TOTAL',    null, null,   null,   1,  '110,120,130,140,150'),
  (170, 'NET ASSETS',                            'TOTAL',    null, null,   null,   1,  '90,-160'),
  (180, 'REPRESENTED BY',                        'HEADING',  null, null,   null,   1,  null),
  (190, 'Share Capital',                         'ACCOUNTS', '13', '3100', '3199', -1, null),
  (200, 'Contributed and Contingency Surplus, Reserves', 'ACCOUNTS', '14', '3200', '3499', -1, null),
  (210, 'Retained Earnings',                     'ACCOUNTS', '15', '3500', '3599', -1, null),
  (220, 'Surplus / (Deficit) Not Yet Appropriated', 'RESULT', null, null,  null,   1,  null),
  (230, 'SHAREHOLDERS'' FUNDS',                  'TOTAL',    null, null,   null,   1,  '190,200,210,220')
) as l(n, caption, t, sch, af, at, sign, terms)
where f.code = 'IC-BS';

insert into fin_statement_format_line (format_id, line_no, caption, line_type, schedule_ref,
    account_from, account_to, sign, total_terms)
select f.id, l.n, l.caption, l.t, l.sch, l.af, l.at, l.sign, l.terms
from fin_statement_format f, (values
  (10,  'UNDERWRITING INCOME',                   'HEADING',  null, null,   null,   1,  null),
  (20,  'Gross Premiums Written',                'ACCOUNTS', '1',  '4100', '4199', -1, null),
  (30,  'Reinsurance Premiums Ceded',            'ACCOUNTS', '2',  '4200', '4299', -1, null),
  (40,  'Change in Unearned Premiums',           'ACCOUNTS', '3',  '4300', '4399', -1, null),
  (50,  'Commission Income',                     'ACCOUNTS', '4',  '4400', '4499', -1, null),
  (60,  'NET PREMIUMS EARNED AND COMMISSIONS',   'TOTAL',    null, null,   null,   1,  '20,30,40,50'),
  (70,  'UNDERWRITING EXPENSES',                 'HEADING',  null, null,   null,   1,  null),
  (80,  'Net Claims Incurred',                   'ACCOUNTS', '5',  '5100', '5399', 1,  null),
  (90,  'Acquisition Costs',                     'ACCOUNTS', '6',  '5400', '5599', 1,  null),
  (100, 'TOTAL UNDERWRITING EXPENSES',           'TOTAL',    null, null,   null,   1,  '80,90'),
  (110, 'NET UNDERWRITING PROFIT',               'TOTAL',    null, null,   null,   1,  '60,-100'),
  (120, 'Investment and Other Income',           'ACCOUNTS', '7',  '4500', '4799', -1, null),
  (130, 'General and Administrative Expenses',   'ACCOUNTS', '8',  '5600', '5799', 1,  null),
  (140, 'PROFIT BEFORE TAX',                     'TOTAL',    null, null,   null,   1,  '110,120,-130'),
  (150, 'Income Tax Expense',                    'ACCOUNTS', '9',  '5800', '5899', 1,  null),
  (160, 'NET SURPLUS / (DEFICIT) RETAINED',      'TOTAL',    null, null,   null,   1,  '140,-150')
) as l(n, caption, t, sch, af, at, sign, terms)
where f.code = 'IC-IE';
