-- =====================================================================================
-- SEED DATA (seed profile only): payables masters.
-- PDC-issued clearing account and its accounting rule, company bank accounts with cheque
-- books and one imprest petty cash fund per branch. Transactions (supplier invoices,
-- payments, PDCs, petty cash vouchers) are created by PayablesSeedDataRunner through the
-- services, so that every document carries a real journal.
-- =====================================================================================

-- Liability for post-dated cheques issued but not yet presented (Dr on maturity).
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2511', 'Post-dated Cheques Issued - Clearing', 'LIABILITY', 'SUB', (select id from coa_account where code = '2500'), (select id from coa_category where code = 'PAYB'), true,
    false, 'NONE', false, false, false, false, true, false, 'Accounts Payable and Accrued Expenses', date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'PDC_ISSUED_PRESENTED', 'Standard issued PDC presentation', 100, date '2010-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, l.party from acc_rule r,
(values
  (1, 'DEBIT', '@PDC_CLEARING', 'AMOUNT', false),
  (2, 'CREDIT', '@BANK', 'AMOUNT', false)
) as l(n, side, acc, comp, party)
where r.event_type = 'PDC_ISSUED_PRESENTED';

insert into pay_bank_account (company_id, code, name, bank_name, account_no, currency, gl_account_code,
    pdc_clearing_account_code, branch_id, notification_format, record_status, authorized_by, authorized_at,
    created_at, created_by)
select c.id, b.code, b.name, b.bank, b.acct, b.ccy, b.gl, '2511', (select id from org_branch where code = 'HO'),
       b.fmt, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c, (values
  ('BDO-CA',  'BDO Current Account (PHP)', 'BDO Unibank, Inc.',                     '0012-3456-7890', 'PHP', '1111', 'FIXED_WIDTH'),
  ('BPI-SA',  'BPI Savings Account (PHP)', 'Bank of the Philippine Islands',        '3141-5926-53',   'PHP', '1112', 'CSV'),
  ('BDO-USD', 'BDO Dollar Account (USD)',  'BDO Unibank, Inc.',                     '0098-7654-3210', 'USD', '1113', 'FIXED_WIDTH')
) as b(code, name, bank, acct, ccy, gl, fmt)
where c.code = 'FVI';

insert into pay_cheque_book (bank_account_id, first_no, last_no, next_no, received_on, status, created_at, created_by)
select a.id, k.first_no, k.last_no, k.first_no, date '2026-01-02', 'ACTIVE', now(), 'SYSTEM'
from pay_bank_account a, (values
  ('BDO-CA',  100001, 100500),
  ('BPI-SA',  500001, 500200),
  ('BDO-USD', 900001, 900100)
) as k(code, first_no, last_no)
where a.code = k.code;

insert into pay_petty_cash_fund (company_id, branch_id, code, name, custodian, gl_account_code,
    replenish_bank_account_id, currency, imprest_amount, cash_balance, record_status, authorized_by,
    authorized_at, created_at, created_by)
select c.id, br.id, f.code, f.name, f.custodian, '1102', (select id from pay_bank_account where code = 'BDO-CA'),
       'PHP', f.imprest, 0, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c join org_branch br on br.company_id = c.id, (values
  ('HO',  'PCF-HO',  'Head Office Petty Cash', 'Liza Custodio',   50000.00),
  ('CEB', 'PCF-CEB', 'Cebu Petty Cash',        'Ramon Villanueva', 30000.00),
  ('DVO', 'PCF-DVO', 'Davao Petty Cash',       'Grace Dumalagan',  30000.00)
) as f(branch, code, name, custodian, imprest)
where c.code = 'FVI' and br.code = f.branch;
