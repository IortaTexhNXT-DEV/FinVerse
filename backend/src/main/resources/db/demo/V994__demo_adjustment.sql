-- =====================================================================================
-- iNXT BrokerVerse - V994 Demo Adjustment (demo profile only). DEMO ONLY.
--   * Demo GL account 4190 Other Income (credit balances of the minimal balance file,
--     OPERATIONS_DESIGN section 5 row 21), only where missing.
--   * Demo accounting rules of the Adjustment events (rows 18, 21 and the commission-only
--     adjustment of ADJID.014). The real accounts come from Comptrollership (OQ07). A credit
--     balance of the file is cleared against the overpaid premium receivable components (the
--     PR_ lines with negative amounts debit 1210.x) and taken to 4190.
--   The demo requests themselves are raised at start-up on the booked demo invoices by
--   adjustment.demo.AdjustmentDemoData, through the real services.
-- =====================================================================================

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4190', 'Other Income - Minimal Balance Credits', 'INCOME', 'MAIN',
       (select p.id from coa_account p where p.company_id = c.id and p.code = '4000'),
       (select id from coa_category where code = 'PREM'), true, false, 'NONE',
       false, false, false, false, false, false, 'Commission Income', date '2026-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = '4190');

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, r.event_type, r.name, 100, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('OPS_AR_INSURER_SETUP', 'AR Insurer set-up after a remitted decrease (demo)'),
    ('OPS_WRITE_OFF', 'Minimal balance write-off / credit (demo)'),
    ('OPS_ADJ_COMMISSION', 'Commission adjustment without premium change (demo)')
) as r(event_type, name)
where c.code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr
from acc_rule r
join (values
  ('OPS_AR_INSURER_SETUP', 1, 'DEBIT',  '1225',    'AR_INSURER',            true,  'AR Insurer - return premium already remitted'),
  ('OPS_AR_INSURER_SETUP', 2, 'CREDIT', '2210',    'AR_INSURER',            true,  'Due to insurer - negative DTIP reclassified'),
  ('OPS_WRITE_OFF',        1, 'DEBIT',  '6510',    'WRITE_OFF',             false, 'Minimal balance written off'),
  ('OPS_WRITE_OFF',        2, 'CREDIT', '1210.01', 'PR_BASIC',              true,  'Premium receivable - basic premium'),
  ('OPS_WRITE_OFF',        3, 'CREDIT', '1210.02', 'PR_DST',                true,  'Premium receivable - DST'),
  ('OPS_WRITE_OFF',        4, 'CREDIT', '1210.03', 'PR_PTX_VAT',            true,  'Premium receivable - premium tax / VAT'),
  ('OPS_WRITE_OFF',        5, 'CREDIT', '1210.04', 'PR_LGT',                true,  'Premium receivable - LGT'),
  ('OPS_WRITE_OFF',        6, 'CREDIT', '1210.05', 'PR_FST',                true,  'Premium receivable - fire service tax'),
  ('OPS_WRITE_OFF',        7, 'CREDIT', '1210.06', 'PR_OTHER',              true,  'Premium receivable - other charges'),
  ('OPS_WRITE_OFF',        8, 'CREDIT', '4190',    'CREDIT_BALANCE',        false, 'Minimal credit balance taken to income'),
  ('OPS_ADJ_COMMISSION',   1, 'DEBIT',  '1220',    'COMMISSION_RECEIVABLE', true,  'Commission receivable (with VAT)'),
  ('OPS_ADJ_COMMISSION',   2, 'CREDIT', '2220',    'UNREALIZED_COMMISSION', false, 'Unrealized commission'),
  ('OPS_ADJ_COMMISSION',   3, 'CREDIT', '2221',    'DEFERRED_OUTPUT_VAT',   false, 'Deferred output VAT on commission')
) as l(event_type, n, side, acc, comp, party, narr) on l.event_type = r.event_type
join org_company c on c.id = r.company_id and c.code = 'FVI';
