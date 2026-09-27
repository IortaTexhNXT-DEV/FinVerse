-- =====================================================================================
-- iNXT BrokerVerse - V1982 Seed data of Data Migration (BRD-13, wave DM1-A): legacy GL chart.
-- SEED DATA ONLY - never load in production.
--   Accounts:   the legacy control accounts of DATA_MIGRATION_DESIGN 14.2 for SIT/UAT - premium
--               receivable (1215.01-.06), PR 2307 (1216), due to insurers (LGC-DTIP), commission
--               receivable (LGC-COMM), unrealised commission (2222), deferred output VAT (2223),
--               unapplied collections (2206) and the migration clearing account (LGC-CLR). The real
--               codes come from Comptrollership.
--   Rules:      seed rules of the opening events MIG_LEGACY_INVOICE_OPENING and MIG_UPP_OPENING and
--               of the year-end adjustment MIG_LEGACY_POSITION_TRUEUP, balanced on LGC-CLR; the
--               legacy lines of the Operations events are added in the Operations waves.
--   Parameter:  the opening value date of the rehearsals, inside the open seed year (production
--               keeps 1-Jan-2028).
-- =====================================================================================

create temporary table mig_seed_gl (
    code varchar(30), name varchar(120), account_class varchar(20), level varchar(10),
    parent varchar(30), category varchar(10), postable boolean, control boolean,
    sub_ledger varchar(20), report_group varchar(120), seq integer
) on commit drop;

insert into mig_seed_gl values
    ('1215',     'Premium Receivable - Legacy',                   'ASSET',     'MAIN', '1200', 'RECV', false, false, 'NONE',         'Insurance Receivables', 1),
    ('1215.01',  'Premium Receivable - Legacy - Basic Premium',   'ASSET',     'SUB',  '1215', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 2),
    ('1215.02',  'Premium Receivable - Legacy - DST',             'ASSET',     'SUB',  '1215', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 3),
    ('1215.03',  'Premium Receivable - Legacy - Premium Tax / VAT', 'ASSET',   'SUB',  '1215', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 4),
    ('1215.04',  'Premium Receivable - Legacy - LGT',             'ASSET',     'SUB',  '1215', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 5),
    ('1215.05',  'Premium Receivable - Legacy - Fire Service Tax', 'ASSET',    'SUB',  '1215', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 6),
    ('1215.06',  'Premium Receivable - Legacy - Other Charges',   'ASSET',     'SUB',  '1215', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 7),
    ('1216',     'PR 2307 - Legacy',                              'ASSET',     'SUB',  '1200', 'RECV', true,  true,  'POLICYHOLDER', 'Insurance Receivables', 8),
    ('LGC-COMM', 'Commission Receivable - Legacy',                'ASSET',     'SUB',  '1200', 'RECV', true,  true,  'INSURER',      'Insurance Receivables', 9),
    ('LGC-CLR',  'Migration Clearing',                            'ASSET',     'SUB',  '1600', 'RECV', true,  false, 'NONE',         'Other Assets', 10),
    ('LGC-DTIP', 'Due to Insurers - Legacy',                      'LIABILITY', 'SUB',  '2200', 'PAYB', true,  true,  'INSURER',      'Insurance Payables', 11),
    ('2206',     'Unapplied Collections - Legacy',                'LIABILITY', 'SUB',  '2200', 'PAYB', true,  false, 'NONE',         'Insurance Payables', 12),
    ('2222',     'Unrealized Commission - Legacy',                'LIABILITY', 'SUB',  '2200', 'PAYB', true,  false, 'NONE',         'Insurance Payables', 13),
    ('2223',     'Deferred Output VAT - Legacy',                  'LIABILITY', 'SUB',  '2500', 'TAXP', true,  false, 'NONE',         'Accounts Payable and Accrued Expenses', 14);

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
cross join (select * from mig_seed_gl order by seq) g
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = g.code)
order by g.seq;

-- ---------- Seed rules of the opening events ---------------------------------------------------
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('MIG_LEGACY_INVOICE_OPENING', 'Legacy invoice opening on the legacy control accounts (seed)'),
    ('MIG_UPP_OPENING', 'Legacy unapplied payment opening (seed)'),
    ('MIG_LEGACY_POSITION_TRUEUP', 'Year-end adjustment of a legacy open item (seed)')
) as e(code, name)
where c.code = 'FVI'
  and not exists (select 1 from acc_rule r where r.company_id = c.id and r.event_type = e.code);

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr
from acc_rule r
join (values
  ('MIG_LEGACY_INVOICE_OPENING', 1, 'DEBIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('MIG_LEGACY_INVOICE_OPENING', 2, 'DEBIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('MIG_LEGACY_INVOICE_OPENING', 3, 'DEBIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('MIG_LEGACY_INVOICE_OPENING', 4, 'DEBIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('MIG_LEGACY_INVOICE_OPENING', 5, 'DEBIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('MIG_LEGACY_INVOICE_OPENING', 6, 'DEBIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('MIG_LEGACY_INVOICE_OPENING', 7, 'DEBIT', '1216', 'LG_PR2307', true, 'Legacy PR 2307'),
  ('MIG_LEGACY_INVOICE_OPENING', 8, 'CREDIT', 'LGC-DTIP', 'LG_DTIP', true, 'Legacy due to insurer'),
  ('MIG_LEGACY_INVOICE_OPENING', 9, 'DEBIT', 'LGC-COMM', 'LG_COMMISSION', true, 'Legacy commission receivable'),
  ('MIG_LEGACY_INVOICE_OPENING', 10, 'DEBIT', 'LGC-COMM', 'LG_COMMISSION_VAT', true, 'Legacy VAT on commission receivable'),
  ('MIG_LEGACY_INVOICE_OPENING', 11, 'CREDIT', '2222', 'LG_UNREALIZED', false, 'Legacy unrealized commission'),
  ('MIG_LEGACY_INVOICE_OPENING', 12, 'CREDIT', '2223', 'LG_DEFERRED_VAT', false, 'Legacy deferred output VAT'),
  ('MIG_LEGACY_INVOICE_OPENING', 13, 'CREDIT', 'LGC-CLR', 'CLEARING', false, 'Migration clearing'),
  ('MIG_UPP_OPENING', 1, 'CREDIT', '2206', 'LG_AMOUNT', false, 'Legacy unapplied collections'),
  ('MIG_UPP_OPENING', 2, 'CREDIT', 'LGC-CLR', 'CLEARING', false, 'Migration clearing'),
  ('MIG_LEGACY_POSITION_TRUEUP', 1, 'DEBIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('MIG_LEGACY_POSITION_TRUEUP', 2, 'DEBIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('MIG_LEGACY_POSITION_TRUEUP', 3, 'DEBIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('MIG_LEGACY_POSITION_TRUEUP', 4, 'DEBIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('MIG_LEGACY_POSITION_TRUEUP', 5, 'DEBIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('MIG_LEGACY_POSITION_TRUEUP', 6, 'DEBIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('MIG_LEGACY_POSITION_TRUEUP', 7, 'DEBIT', '1216', 'LG_PR2307', true, 'Legacy PR 2307'),
  ('MIG_LEGACY_POSITION_TRUEUP', 8, 'CREDIT', 'LGC-DTIP', 'LG_DTIP', true, 'Legacy due to insurer'),
  ('MIG_LEGACY_POSITION_TRUEUP', 9, 'DEBIT', 'LGC-COMM', 'LG_COMMISSION', true, 'Legacy commission receivable'),
  ('MIG_LEGACY_POSITION_TRUEUP', 10, 'DEBIT', 'LGC-COMM', 'LG_COMMISSION_VAT', true, 'Legacy VAT on commission receivable'),
  ('MIG_LEGACY_POSITION_TRUEUP', 11, 'CREDIT', '2222', 'LG_UNREALIZED', false, 'Legacy unrealized commission'),
  ('MIG_LEGACY_POSITION_TRUEUP', 12, 'CREDIT', '2223', 'LG_DEFERRED_VAT', false, 'Legacy deferred output VAT'),
  ('MIG_LEGACY_POSITION_TRUEUP', 13, 'CREDIT', '2206', 'LG_AMOUNT', false, 'Legacy unapplied collections'),
  ('MIG_LEGACY_POSITION_TRUEUP', 14, 'CREDIT', 'LGC-CLR', 'CLEARING', false, 'Migration clearing')
) as l(event_type, n, side, acc, comp, party, narr) on l.event_type = r.event_type
join org_company c on c.id = r.company_id and c.code = 'FVI'
where not exists (select 1 from acc_rule_line x where x.rule_id = r.id);

update sys_parameter set param_value = '2026-09-01', updated_at = now(), updated_by = 'SYSTEM'
where param_key = 'MIG_OPENING_VALUE_DATE';
