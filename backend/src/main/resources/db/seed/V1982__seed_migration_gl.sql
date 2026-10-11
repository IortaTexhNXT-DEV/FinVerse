-- =====================================================================================
-- iNXT BrokerVerse - V1982 Seed data of Data Migration (BRD-13, wave DM1-A): legacy GL chart.
-- SEED DATA ONLY - never load in production.
--   Accounts:   the legacy control accounts of DATA_MIGRATION_DESIGN 14.2 for SIT/UAT - premium
--               receivable (1215.01-.06), PR 2307 (1216), due to insurers (LGC-DTIP), commission
--               receivable (LGC-COMM), unrealised commission (2222), deferred output VAT (2223),
--               unapplied collections (2206) and the migration clearing account (LGC-CLR). The real
--               codes come from Comptrollership.
--   Rules:      seed rules of the opening events MIG_LEGACY_INVOICE_OPENING and MIG_UPP_OPENING and
--               of the year-end adjustment MIG_LEGACY_POSITION_TRUEUP, balanced on LGC-CLR, and the
--               legacy lines (LG_ components) added to the seed rules of the Operations events:
--               payment application, unapplied refund and reclass, remittance, and the booking of
--               the endorsements of legacy invoices (BROKER_BOOKING, V873), the 2307 reclass and offset,
--               minimal balances, write-offs, commission changes and direct payment events; the seed
--               rule of OPS_UNAPPLIED_TO_INCOME (other income 4190).
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
    ('MIG_LEGACY_POSITION_TRUEUP', 'Year-end adjustment of a legacy open item (seed)'),
    ('OPS_UNAPPLIED_TO_INCOME', 'Unapplied payment to other income (seed)')
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

-- ---------- GL-SL reconciliation of the legacy control accounts (ACSL, context LEGACY) --------
insert into acsl_glsl_control (company_id, account_code, source, components, document_types, currency, active,
    ledger_context, created_at, created_by)
select c.id, x.code, 'OPS_LEDGER', x.components, null, null, true, 'LEGACY', now(), 'SYSTEM'
from org_company c
cross join (values ('1215.01', 'BASIC'), ('1215.02', 'DST'), ('1215.03', 'PREMIUM_TAX_VAT'),
                   ('1215.04', 'LGT'), ('1215.05', 'FST'), ('1215.06', 'OTHER'), ('1216', 'PR2307'),
                   ('LGC-DTIP', 'DTIP')) as x(code, components)
where c.code = 'FVI'
  and not exists (select 1 from acsl_glsl_control g where g.company_id = c.id and g.account_code = x.code);

-- ---------- Legacy lines of the Operations and booking events (DATA_MIGRATION_DESIGN 14.4) --------
insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr
from acc_rule r
join (values
  ('OPS_PAYMENT_APPLY', 101, 'DEBIT', '2206', 'LG_APPLIED', true, 'Legacy unapplied collections applied'),
  ('OPS_PAYMENT_APPLY', 102, 'CREDIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('OPS_PAYMENT_APPLY', 103, 'CREDIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('OPS_PAYMENT_APPLY', 104, 'CREDIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('OPS_PAYMENT_APPLY', 105, 'CREDIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('OPS_PAYMENT_APPLY', 106, 'CREDIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('OPS_PAYMENT_APPLY', 107, 'CREDIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('OPS_PAYMENT_APPLY', 108, 'DEBIT', '2222', 'LG_REALIZED_COMMISSION', false, 'Legacy commission realized on collection'),
  ('OPS_PAYMENT_APPLY', 109, 'CREDIT', '4101', 'LG_REALIZED_COMMISSION', false, 'Commission income'),
  ('OPS_PAYMENT_APPLY', 110, 'DEBIT', '2223', 'LG_REALIZED_VAT', false, 'Legacy deferred output VAT made due'),
  ('OPS_PAYMENT_APPLY', 111, 'CREDIT', '2504', 'LG_REALIZED_VAT', false, 'Output VAT on commission'),
  ('OPS_UNAPPLIED_REFUND', 101, 'DEBIT', '2206', 'LG_AMOUNT', true, 'Legacy unapplied collections refunded'),
  ('OPS_UNAPPLIED_REFUND', 102, 'CREDIT', '2216', 'LG_AMOUNT', true, 'Refund payable to client'),
  ('OPS_UNAPPLIED_RECLASS', 101, 'DEBIT', '2206', 'LG_RELEASED', true, 'Legacy unapplied collections released'),
  ('OPS_UNAPPLIED_RECLASS', 102, 'CREDIT', '2206', 'LG_ASSIGNED', true, 'Legacy unapplied collections assigned'),
  ('OPS_REMITTANCE', 101, 'DEBIT', 'LGC-DTIP', 'LG_DTIP', true, 'Legacy due to insurer - paid AR remitted'),
  ('OPS_REMITTANCE', 102, 'CREDIT', 'LGC-COMM', 'LG_COMMISSION_RECEIVABLE', true, 'Legacy commission and VAT retained'),
  ('BROKER_BOOKING', 101, 'DEBIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('BROKER_BOOKING', 102, 'DEBIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('BROKER_BOOKING', 103, 'DEBIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('BROKER_BOOKING', 104, 'DEBIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('BROKER_BOOKING', 105, 'DEBIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('BROKER_BOOKING', 106, 'DEBIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('BROKER_BOOKING', 107, 'CREDIT', 'LGC-DTIP', 'LG_DTIP', true, 'Legacy due to insurer'),
  ('BROKER_BOOKING', 108, 'DEBIT', 'LGC-COMM', 'LG_COMMISSION_RECEIVABLE', true, 'Legacy commission receivable (with VAT)'),
  ('BROKER_BOOKING', 109, 'CREDIT', '2222', 'LG_UNREALIZED_COMMISSION', false, 'Legacy unrealized commission'),
  ('BROKER_BOOKING', 110, 'CREDIT', '2223', 'LG_DEFERRED_OUTPUT_VAT', false, 'Legacy deferred output VAT'),
  ('BROKER_BOOKING', 111, 'CREDIT', '4101', 'LG_COMMISSION_INCOME', false, 'Commission income'),
  ('BROKER_BOOKING', 112, 'CREDIT', '2504', 'LG_OUTPUT_VAT', false, 'Output VAT on commission'),
  ('OPS_UNAPPLIED_TO_INCOME', 1, 'DEBIT', '2205', 'AMOUNT', true, 'Unapplied collections taken to income'),
  ('OPS_UNAPPLIED_TO_INCOME', 2, 'CREDIT', '4190', 'AMOUNT', false, 'Other income - unclaimed collections'),
  ('OPS_UNAPPLIED_TO_INCOME', 3, 'DEBIT', '2206', 'LG_AMOUNT', true, 'Legacy unapplied collections taken to income'),
  ('OPS_UNAPPLIED_TO_INCOME', 4, 'CREDIT', '4190', 'LG_AMOUNT', false, 'Other income - unclaimed collections'),
  ('OPS_CWT_RECLASS', 101, 'DEBIT', '1216', 'LG_PR2307', true, 'Legacy PR 2307'),
  ('OPS_CWT_RECLASS', 102, 'CREDIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('OPS_CWT_RECLASS', 103, 'CREDIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('OPS_CWT_RECLASS', 104, 'CREDIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('OPS_CWT_RECLASS', 105, 'CREDIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('OPS_CWT_RECLASS', 106, 'CREDIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('OPS_CWT_RECLASS', 107, 'CREDIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('OPS_CWT_DTIP_OFFSET', 101, 'DEBIT', 'LGC-DTIP', 'LG_DTIP', true, 'Legacy due to insurer settled by 2307'),
  ('OPS_CWT_DTIP_OFFSET', 102, 'CREDIT', '1216', 'LG_PR2307', true, 'Legacy PR 2307 settled'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 101, 'CREDIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 102, 'CREDIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 103, 'CREDIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 104, 'CREDIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 105, 'CREDIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 106, 'CREDIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('OPS_WRITE_OFF', 101, 'CREDIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('OPS_WRITE_OFF', 102, 'CREDIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('OPS_WRITE_OFF', 103, 'CREDIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('OPS_WRITE_OFF', 104, 'CREDIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('OPS_WRITE_OFF', 105, 'CREDIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('OPS_WRITE_OFF', 106, 'CREDIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('OPS_ADJ_COMMISSION', 101, 'DEBIT', 'LGC-COMM', 'LG_COMMISSION_RECEIVABLE', true, 'Legacy commission receivable (with VAT)'),
  ('OPS_ADJ_COMMISSION', 102, 'CREDIT', '2222', 'LG_UNREALIZED_COMMISSION', false, 'Legacy unrealized commission'),
  ('OPS_ADJ_COMMISSION', 103, 'CREDIT', '2223', 'LG_DEFERRED_OUTPUT_VAT', false, 'Legacy deferred output VAT'),
  ('OPS_DP_PR_REVERSAL', 101, 'DEBIT', 'LGC-DTIP', 'LG_DTIP', true, 'Legacy due to insurer - paid directly by the client'),
  ('OPS_DP_PR_REVERSAL', 102, 'CREDIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('OPS_DP_PR_REVERSAL', 103, 'CREDIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('OPS_DP_PR_REVERSAL', 104, 'CREDIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('OPS_DP_PR_REVERSAL', 105, 'CREDIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('OPS_DP_PR_REVERSAL', 106, 'CREDIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('OPS_DP_PR_REVERSAL', 107, 'CREDIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('OPS_DP_REINSTATE', 101, 'DEBIT', 'LGC-DTIP', 'LG_DTIP', true, 'Legacy due to insurer - paid directly by the client'),
  ('OPS_DP_REINSTATE', 102, 'CREDIT', '1215.01', 'LG_PR_BASIC', true, 'Legacy premium receivable - basic premium'),
  ('OPS_DP_REINSTATE', 103, 'CREDIT', '1215.02', 'LG_PR_DST', true, 'Legacy premium receivable - DST'),
  ('OPS_DP_REINSTATE', 104, 'CREDIT', '1215.03', 'LG_PR_PTX_VAT', true, 'Legacy premium receivable - premium tax / VAT'),
  ('OPS_DP_REINSTATE', 105, 'CREDIT', '1215.04', 'LG_PR_LGT', true, 'Legacy premium receivable - LGT'),
  ('OPS_DP_REINSTATE', 106, 'CREDIT', '1215.05', 'LG_PR_FST', true, 'Legacy premium receivable - fire service tax'),
  ('OPS_DP_REINSTATE', 107, 'CREDIT', '1215.06', 'LG_PR_OTHER', true, 'Legacy premium receivable - other charges'),
  ('OPS_DP_COMMISSION_COLLECT', 101, 'CREDIT', 'LGC-COMM', 'LG_COMMISSION_RECEIVABLE', true, 'Legacy commission receivable (with VAT)'),
  ('OPS_DP_COMMISSION_COLLECT', 102, 'DEBIT', '2222', 'LG_REALIZED_COMMISSION', false, 'Legacy unrealized commission realized'),
  ('OPS_DP_COMMISSION_COLLECT', 103, 'CREDIT', '4101', 'LG_REALIZED_COMMISSION', false, 'Commission income'),
  ('OPS_DP_COMMISSION_COLLECT', 104, 'DEBIT', '2223', 'LG_REALIZED_VAT', false, 'Legacy deferred output VAT due'),
  ('OPS_DP_COMMISSION_COLLECT', 105, 'CREDIT', '2504', 'LG_REALIZED_VAT', false, 'Output VAT on commission')
) as l(event_type, n, side, acc, comp, party, narr) on l.event_type = r.event_type
join org_company c on c.id = r.company_id and c.code = 'FVI'
where not exists (select 1 from acc_rule_line x where x.rule_id = r.id and x.line_no = l.n);
