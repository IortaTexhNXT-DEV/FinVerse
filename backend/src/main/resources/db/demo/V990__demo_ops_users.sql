-- =====================================================================================
-- iNXT BrokerVerse - V990 Demo Operations (BRD-2) users, GL accounts and BOOK rates
-- (demo profile only; password for all users: Brokerverse@2026). DEMO ONLY.
--   * Users of the Operations roles (OPERATIONS_DESIGN 6.2). adjust (ADJUSTMENT) and mkttl
--     (MKT_TL) exist since V980 and received their Operations grants in V760.
--   * Demo GL accounts of the Operations events (OPERATIONS_DESIGN section 5), only where
--     missing; the real accounts come from Comptrollership (OQ07). Each Operations module
--     seeds its own event types and demo rules (V764-V789, V991-V995).
--   * BOOK exchange rates (Comptrollership rate, 2 decimals, CSHID.012-014, OQ08) derived from
--     the demo SPOT rates.
--   The invoice ledger itself needs no seed: opsledger copies every booking of
--   booking.demo.BookingDemoData when it is committed, and OpsLedgerDemoReplay replays the
--   bookings of a database created before Operations.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select id from org_branch where code = u.branch order by id limit 1), now(), 'SYSTEM'
from (values ('cashier',  'Carmela Cashier',           'cashier@brokerverse-demo.ph',  'HO'),
             ('cashbr',   'Benjie Branch Cashier',     'cashbr@brokerverse-demo.ph',   'CEB'),
             ('cashtl',   'Cecilia Cashiering Lead',   'cashtl@brokerverse-demo.ph',   'HO'),
             ('remit',    'Rafael Remittance',         'remit@brokerverse-demo.ph',    'HO'),
             ('remittl',  'Rosario Remittance Lead',   'remittl@brokerverse-demo.ph',  'HO'),
             ('recon',    'Regina Reconciliation',     'recon@brokerverse-demo.ph',    'HO'),
             ('adjtl',    'Alfredo Adjustment Lead',   'adjtl@brokerverse-demo.ph',    'HO'),
             ('commrec',  'Corazon Commission',        'commrec@brokerverse-demo.ph',  'HO'),
             ('commtl',   'Cristina Commission Lead',  'commtl@brokerverse-demo.ph',   'HO'),
             ('mktcoll',  'Miguel Marketing Collection', 'mktcoll@brokerverse-demo.ph', 'HO'),
             ('comptrol', 'Consuelo Comptroller',      'comptrol@brokerverse-demo.ph', 'HO'),
             ('disb',     'Dante Disbursement',        'disb@brokerverse-demo.ph',     'HO'))
     as u(username, full_name, email, branch);

insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u join sec_role r on r.code = case u.username
    when 'cashier' then 'CASHIER' when 'cashbr' then 'CASHIER' when 'cashtl' then 'CASHIER_TL'
    when 'remit' then 'REMIT_PROCESSOR' when 'remittl' then 'REMIT_TL' when 'recon' then 'RECON_HANDLER'
    when 'adjtl' then 'ADJUSTMENT_TL' when 'commrec' then 'COMMREC_HANDLER' when 'commtl' then 'COMMREC_TL'
    when 'mktcoll' then 'MKT_COLLECTION' when 'comptrol' then 'COMPTROLLERSHIP' when 'disb' then 'DISBURSEMENT'
    end
where u.username in ('cashier', 'cashbr', 'cashtl', 'remit', 'remittl', 'recon', 'adjtl', 'commrec', 'commtl',
                     'mktcoll', 'comptrol', 'disb');

-- ---------- Demo GL accounts of the Operations events --------------------------------------
create temporary table ops_demo_gl (
    code varchar(30), name varchar(120), account_class varchar(20), level varchar(10),
    parent varchar(30), category varchar(10), postable boolean, control boolean,
    sub_ledger varchar(20), report_group varchar(120), seq integer
) on commit drop;

insert into ops_demo_gl values
    ('1211', 'Premium Receivable - CWT 2307 (PR2307)',       'ASSET',     'SUB',  '1200', 'RECV', true, true,  'POLICYHOLDER', 'Insurance Receivables', 1),
    ('1225', 'AR Insurer',                                   'ASSET',     'SUB',  '1200', 'RECV', true, true,  'INSURER',      'Insurance Receivables', 2),
    ('1230', 'Incentive Receivable',                         'ASSET',     'SUB',  '1200', 'RECV', true, true,  'INSURER',      'Insurance Receivables', 3),
    ('2211', 'Due to Insurers - for Disbursement',           'LIABILITY', 'SUB',  '2200', 'PAYB', true, true,  'INSURER',      'Insurance Payables', 4),
    ('2215', 'AP Overages',                                  'LIABILITY', 'SUB',  '2200', 'PAYB', true, false, 'NONE',         'Insurance Payables', 5),
    ('2216', 'AP Refund Payable - Clients',                  'LIABILITY', 'SUB',  '2200', 'PAYB', true, true,  'POLICYHOLDER', 'Insurance Payables', 6),
    ('2230', 'Due to Branches - Incentive Pass-on',          'LIABILITY', 'SUB',  '2200', 'PAYB', true, false, 'NONE',         'Insurance Payables', 7),
    ('4110', 'Service Fee Income',                           'INCOME',    'MAIN', '4000', 'PREM', true, false, 'NONE',         'Commission Income', 8),
    ('4120', 'Profit Share Income',                          'INCOME',    'MAIN', '4000', 'PREM', true, false, 'NONE',         'Commission Income', 9),
    ('4130', 'Incentive Income',                             'INCOME',    'MAIN', '4000', 'PREM', true, false, 'NONE',         'Commission Income', 10),
    ('6510', 'Minimal Balance / Write-off',                  'EXPENSE',   'SUB',  '5600', 'OPEX', true, false, 'NONE',         'General and Administrative Expenses', 11);

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, g.code, g.name, g.account_class, g.level,
       (select p.id from coa_account p where p.company_id = c.id and p.code = g.parent),
       (select id from coa_category where code = g.category), g.postable, g.control, g.sub_ledger,
       false, false, false, false, false, false, g.report_group, date '2026-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (select * from ops_demo_gl order by seq) g
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = g.code)
order by g.seq;

-- ---------- BOOK rates (2 decimals) from the demo SPOT rates -------------------------------
insert into cur_exchange_rate (currency_code, rate_type, effective_date, rate, created_at, created_by)
select s.currency_code, 'BOOK', s.effective_date, round(s.rate, 2), now(), 'SYSTEM'
from cur_exchange_rate s
where s.rate_type = 'SPOT'
  and round(s.rate, 2) > 0
  and not exists (select 1 from cur_exchange_rate b
                  where b.currency_code = s.currency_code and b.rate_type = 'BOOK'
                    and b.effective_date = s.effective_date);
