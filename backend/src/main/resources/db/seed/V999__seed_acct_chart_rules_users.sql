-- =====================================================================================
-- iNXT BrokerVerse - V999 Seed Accounting, Disbursement and ACSL (BRD-5) chart, rules and users
-- (seed profile only; password for all users: Brokerverse@2026). SEED DATA ONLY - never load in
-- production.
--   * Seed chart (docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section 3): the Operations
--     placeholder accounts take the names BDOI uses in its reports, and the accounts the BRD
--     names without a seed account are added. The real chart is uploaded by FRBS (FRBS 2.3.1,
--     AQ01); no code refers to an account code.
--   * Seed accounting rules of the event types seeded in V890 (design section 6). The real rules
--     come from Comptrollership (AQ02).
--   * SIT/UAT users of the BRD-5 roles of V890 (design section 8.2). disb (DISBURSEMENT) exists
--     since V990 and received its new grants through its role.
--   The seed transactions (payees, DVs, requests, cases, service-fee runs) are created at start-up
--   by the Java seed runners of the modules (A1 waves), after the Operations storyline.
-- =====================================================================================

-- ---------- Seed chart: BDOI names of the Operations accounts (design section 3) --------------
update coa_account a set name = v.name, updated_at = now(), updated_by = 'SYSTEM'
from (values ('1210', 'Premium Receivable'),
             ('1211', 'Premiums Receivable 2307'),
             ('1225', 'AR Insurer''s Refund'),
             ('2205', 'AP - Others (Unapplied Collections)'),
             ('2210', 'Payable to Insurance Companies')) as v(code, name)
where a.code = v.code
  and a.company_id = (select id from org_company where code = 'FVI');

-- ---------- Seed chart: accounts named by the BRD (design section 3) ---------------------------
create temporary table acct_seed_gl (
    code varchar(30), name varchar(120), account_class varchar(20), level varchar(10),
    parent varchar(30), category varchar(10), postable boolean, control boolean,
    sub_ledger varchar(20), cost_center boolean, report_group varchar(120), seq integer
) on commit drop;

insert into acct_seed_gl values
    ('1610', 'AR-BIR on Commission',                         'ASSET',     'SUB',  '1600', 'RECV', true, false, 'NONE',         false, 'Other Assets', 1),
    ('1611', 'AR-BIR on Incentives',                         'ASSET',     'SUB',  '1600', 'RECV', true, false, 'NONE',         false, 'Other Assets', 2),
    ('1612', 'AR-BIR on Hand (Certificates Received)',       'ASSET',     'SUB',  '1600', 'RECV', true, false, 'NONE',         false, 'Other Assets', 3),
    ('2217', 'A/P Refund from Insurer',                      'LIABILITY', 'SUB',  '2200', 'PAYB', true, true,  'POLICYHOLDER', false, 'Insurance Payables', 4),
    ('2240', 'Miscellaneous Liability - Stale Checks',       'LIABILITY', 'SUB',  '2500', 'PAYB', true, false, 'NONE',         false, 'Accounts Payable and Accrued Expenses', 5),
    ('2241', 'Checks Outstanding',                           'LIABILITY', 'SUB',  '2500', 'PAYB', true, false, 'NONE',         false, 'Accounts Payable and Accrued Expenses', 6),
    ('2250', 'Service Fee Payable',                          'LIABILITY', 'SUB',  '2500', 'PAYB', true, false, 'NONE',         false, 'Accounts Payable and Accrued Expenses', 7),
    ('2260', 'AP - Officers and Employees',                  'LIABILITY', 'SUB',  '2500', 'PAYB', true, false, 'NONE',         false, 'Accounts Payable and Accrued Expenses', 8),
    ('4131', 'CPC2 Incentive Income',                        'INCOME',    'MAIN', '4000', 'PREM', true, false, 'NONE',         false, 'Other Income', 9),
    ('5614', 'Service Fee Expense',                          'EXPENSE',   'SUB',  '5600', 'OPEX', true, false, 'NONE',         true,  'General and Administrative Expenses', 10);

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, g.code, g.name, g.account_class, g.level,
       (select p.id from coa_account p where p.company_id = c.id and p.code = g.parent),
       (select id from coa_category where code = g.category), g.postable, g.control, g.sub_ledger,
       true, g.cost_center, false, false, false, false, g.report_group, date '2026-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (select * from acct_seed_gl order by seq) g
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = g.code)
order by g.seq;

-- ---------- Seed accounting rules of the BRD-5 events (design section 6) -----------------------
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('DISB_VOUCHER', 'Disbursement voucher - payable settled by type (seed)'),
    ('DISB_CHECK_NEGOTIATED', 'Check negotiated - checks outstanding cleared (seed)'),
    ('DISB_CHECK_STALE', 'Check staled - to miscellaneous liability (seed)'),
    ('DISB_FUND_TRANSFER', 'Account funding - bank to bank (seed)'),
    ('TAX_CWT_CERT_RECEIVED', 'Insurer 2307 received - AR-BIR on hand (seed)'),
    ('OPS_REMIT_CPC2', 'CPC2 incentive deducted from the remittance (seed)'),
    ('OPS_REMIT_DEDUCTION', 'AR insurer''s refund deducted from the remittance (seed)'),
    ('FRBS_SERVICE_FEE_ACCRUE', 'Service fee accrual (seed)'),
    ('PRQ_CA_LIQUIDATION', 'Cash advance liquidation (seed)')
) as e(code, name)
where c.code = 'FVI'
  and not exists (select 1 from acc_rule r where r.company_id = c.id and r.event_type = e.code);

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr
from acc_rule r
join (values
  -- DV: the payable of the DV type is settled; the paying account is given by Disbursement
  ('DISB_VOUCHER', 1, 'DEBIT', '2211', 'REMITTANCE', true, 'Due to insurer - remittance paid'),
  ('DISB_VOUCHER', 2, 'DEBIT', '2216', 'REFUND', true, 'Refund payable to client paid'),
  ('DISB_VOUCHER', 3, 'DEBIT', '2217', 'REFUND_FROM_INSURER', true, 'Refund from insurer paid to client'),
  ('DISB_VOUCHER', 4, 'DEBIT', '2501', 'SUPPLIER', true, 'Accounts payable - supplier paid'),
  ('DISB_VOUCHER', 5, 'DEBIT', '2501', 'GOVERNMENT', true, 'Accounts payable - government agency paid'),
  ('DISB_VOUCHER', 6, 'DEBIT', '2501', 'OTHER_BANK_UNIT', true, 'Accounts payable - other bank unit paid'),
  ('DISB_VOUCHER', 7, 'DEBIT', '1604', 'EMPLOYEE', false, 'Employee-related payment'),
  ('DISB_VOUCHER', 8, 'DEBIT', '1604', 'CASH_ADVANCE', false, 'Cash advance to employee'),
  ('DISB_VOUCHER', 9, 'DEBIT', '2250', 'SERVICE_FEE', false, 'Service fee payable paid'),
  ('DISB_VOUCHER', 10, 'DEBIT', '2230', 'PASS_ON', false, 'Incentive pass-on paid'),
  ('DISB_VOUCHER', 11, 'DEBIT', '2240', 'STALE_REISSUE', false, 'Stale check re-issued'),
  ('DISB_VOUCHER', 12, 'DEBIT', '@EXPENSE', 'OTHER', false, 'Other disbursement'),
  ('DISB_VOUCHER', 13, 'CREDIT', '@PAY_ACCOUNT', 'PAID', false, 'Paid'),
  ('DISB_VOUCHER', 14, 'CREDIT', '2508', 'EWT', false, 'Expanded withholding tax'),
  ('DISB_CHECK_NEGOTIATED', 1, 'DEBIT', '2241', 'AMOUNT', false, 'Checks outstanding cleared'),
  ('DISB_CHECK_NEGOTIATED', 2, 'CREDIT', '@BANK', 'AMOUNT', false, 'Check negotiated'),
  ('DISB_CHECK_STALE', 1, 'DEBIT', '2241', 'AMOUNT', false, 'Checks outstanding'),
  ('DISB_CHECK_STALE', 2, 'CREDIT', '2240', 'AMOUNT', false, 'Stale check'),
  ('DISB_FUND_TRANSFER', 1, 'DEBIT', '@TARGET_BANK', 'AMOUNT', false, 'Account funding received'),
  ('DISB_FUND_TRANSFER', 2, 'CREDIT', '@SOURCE_BANK', 'AMOUNT', false, 'Account funding sent'),
  ('TAX_CWT_CERT_RECEIVED', 1, 'DEBIT', '1612', 'COMMISSION_CWT', false, 'Certificate on commission received'),
  ('TAX_CWT_CERT_RECEIVED', 2, 'CREDIT', '1610', 'COMMISSION_CWT', false, 'AR-BIR on commission'),
  ('TAX_CWT_CERT_RECEIVED', 3, 'DEBIT', '1612', 'INCENTIVE_CWT', false, 'Certificate on incentives received'),
  ('TAX_CWT_CERT_RECEIVED', 4, 'CREDIT', '1611', 'INCENTIVE_CWT', false, 'AR-BIR on incentives'),
  ('OPS_REMIT_CPC2', 1, 'DEBIT', '2211', 'GROSS', true, 'CPC2 deducted from the remittance'),
  ('OPS_REMIT_CPC2', 2, 'CREDIT', '4131', 'CPC2_INCOME', false, 'CPC2 incentive income'),
  ('OPS_REMIT_CPC2', 3, 'CREDIT', '2504', 'OUTPUT_VAT', false, 'Output VAT on CPC2'),
  ('OPS_REMIT_DEDUCTION', 1, 'DEBIT', '2211', 'AMOUNT', true, 'Deducted from the remittance'),
  ('OPS_REMIT_DEDUCTION', 2, 'CREDIT', '1225', 'AMOUNT', true, 'AR insurer''s refund settled'),
  ('FRBS_SERVICE_FEE_ACCRUE', 1, 'DEBIT', '5614', 'AMOUNT', false, 'Service fee expense'),
  ('FRBS_SERVICE_FEE_ACCRUE', 2, 'CREDIT', '2250', 'AMOUNT', false, 'Service fee payable'),
  ('PRQ_CA_LIQUIDATION', 1, 'DEBIT', '@PER_DIEM', 'PER_DIEM', false, 'Per diem'),
  ('PRQ_CA_LIQUIDATION', 2, 'DEBIT', '@REPRESENTATION', 'REPRESENTATION', false, 'Representation'),
  ('PRQ_CA_LIQUIDATION', 3, 'DEBIT', '@TRANSPORT', 'TRANSPORT', false, 'Transportation'),
  ('PRQ_CA_LIQUIDATION', 4, 'DEBIT', '@LODGING', 'LODGING', false, 'Lodging'),
  ('PRQ_CA_LIQUIDATION', 5, 'DEBIT', '@OTHER', 'OTHER', false, 'Other expenses'),
  ('PRQ_CA_LIQUIDATION', 6, 'DEBIT', '@CASH', 'CASH_RETURNED', false, 'Excess cash returned'),
  ('PRQ_CA_LIQUIDATION', 7, 'CREDIT', '2260', 'SHORTAGE', false, 'Shortage payable to employee'),
  ('PRQ_CA_LIQUIDATION', 8, 'CREDIT', '1604', 'ADVANCE', false, 'Cash advance liquidated')
) as l(event, n, side, acc, comp, party, narr) on l.event = r.event_type
where r.company_id = (select id from org_company where code = 'FVI')
  and not exists (select 1 from acc_rule_line x where x.rule_id = r.id);

-- ---------- SIT/UAT users of the BRD-5 roles (design section 8.2) --------------------------------
insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('glofficer', 'Gloria GL Officer',            'glofficer@brokerverse-seed.ph'),
             ('gltl',      'Gerardo GL Team Lead',         'gltl@brokerverse-seed.ph'),
             ('glhead',    'Graciela GL Section Head',     'glhead@brokerverse-seed.ph'),
             ('disbtl',    'Diego Disbursement Lead',      'disbtl@brokerverse-seed.ph'),
             ('disbtl2',   'Dolores Disbursement Verifier', 'disbtl2@brokerverse-seed.ph'),
             ('disbappr',  'Daniel Disbursement Approver', 'disbappr@brokerverse-seed.ph'),
             ('disbappr2', 'Divina Disbursement Approver', 'disbappr2@brokerverse-seed.ph'),
             ('mktao',     'Marco Marketing Processor',    'mktao@brokerverse-seed.ph'),
             ('mktrev',    'Maricel Marketing Reviewer',   'mktrev@brokerverse-seed.ph'),
             ('mktappr',   'Manolo Marketing Approver',    'mktappr@brokerverse-seed.ph'),
             ('hrappr',    'Helena HR Approver',           'hrappr@brokerverse-seed.ph'),
             ('acsl',      'Arturo ACSL Processor',        'acsl@brokerverse-seed.ph'),
             ('acsltl',    'Andrea ACSL Team Leader',      'acsltl@brokerverse-seed.ph'),
             ('acslhead',  'Antonio ACSL Head',            'acslhead@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('glofficer', 'FRBS_PROCESSOR'), ('gltl', 'FRBS_TL'), ('glhead', 'FRBS_HEAD'),
             ('disbtl', 'DISB_TL'), ('disbtl2', 'DISB_TL'), ('disbappr', 'DISB_APPROVER'),
             ('disbappr2', 'DISB_APPROVER'), ('mktao', 'PRQ_PROCESSOR'), ('mktrev', 'PRQ_REVIEWER'),
             ('mktappr', 'PRQ_APPROVER'), ('hrappr', 'HR_APPROVER'), ('acsl', 'ACSL_PROCESSOR'),
             ('acsltl', 'ACSL_TL'), ('acslhead', 'ACSL_HEAD'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);
