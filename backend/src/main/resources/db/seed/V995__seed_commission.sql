-- =====================================================================================
-- iNXT BrokerVerse - V995 Seed commission receivables (seed profile only). SEED DATA ONLY.
--   * Seed accounting rules of the commission events (OPERATIONS_DESIGN section 5 rows 22-26);
--     the real accounts come from Comptrollership (OQ07).
--   * The bank account proposed for direct payment commission collections (1111).
--   * The three incentive schemes of the BRD, inactive and without tiers: targets, rates and
--     amounts are parked with BDOI (OQ39).
--   The direct payment storyline (DP list, validation, billing) runs on the seed direct payment
--   booking ARN-2026-940003 through commission.seed.CommissionSeedData.
-- =====================================================================================

update sys_parameter set param_value = '1111'
where param_key = 'CMR_DP_COLLECTION_BANK' and param_value = '';

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, r.event_type, r.name, 100, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('OPS_DP_COMMISSION_COLLECT', 'Direct payment commission collected (seed)'),
    ('OPS_DP_PR_REVERSAL', 'Direct payment PR reversal (seed)'),
    ('OPS_DP_REINSTATE', 'Direct payment reinstatement (seed)'),
    ('OPS_INCENTIVE_ACCRUE', 'Incentive earned from insurer (seed)'),
    ('OPS_INCENTIVE_PASS_ON', 'Incentive pass-on to branches (seed)')
) as r(event_type, name)
where c.code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr from acc_rule r,
(values
  (1, 'DEBIT',  '@BANK', 'CASH',                  false, 'Commission received from the insurer'),
  (2, 'DEBIT',  '1602',  'CWT',                   false, 'Creditable withholding tax on commission'),
  (3, 'CREDIT', '1220',  'COMMISSION_RECEIVABLE', true,  'Commission receivable (with VAT)'),
  (4, 'DEBIT',  '2220',  'REALIZED_COMMISSION',   false, 'Unrealized commission realized'),
  (5, 'CREDIT', '4101',  'REALIZED_COMMISSION',   false, 'Commission income'),
  (6, 'DEBIT',  '2221',  'REALIZED_VAT',          false, 'Deferred output VAT due'),
  (7, 'CREDIT', '2504',  'REALIZED_VAT',          false, 'Output VAT on commission')
) as l(n, side, acc, comp, party, narr)
where r.event_type = 'OPS_DP_COMMISSION_COLLECT'
  and r.company_id = (select id from org_company where code = 'FVI');

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr from acc_rule r,
(values
  (1, 'DEBIT',  '2210',    'DTIP',       true, 'Due to insurer - paid directly by the client'),
  (2, 'CREDIT', '1210.01', 'PR_BASIC',   true, 'Premium receivable - basic premium'),
  (3, 'CREDIT', '1210.02', 'PR_DST',     true, 'Premium receivable - DST'),
  (4, 'CREDIT', '1210.03', 'PR_PTX_VAT', true, 'Premium receivable - premium tax / VAT'),
  (5, 'CREDIT', '1210.04', 'PR_LGT',     true, 'Premium receivable - LGT'),
  (6, 'CREDIT', '1210.05', 'PR_FST',     true, 'Premium receivable - fire service tax'),
  (7, 'CREDIT', '1210.06', 'PR_OTHER',   true, 'Premium receivable - other charges')
) as l(n, side, acc, comp, party, narr)
where r.event_type in ('OPS_DP_PR_REVERSAL', 'OPS_DP_REINSTATE')
  and r.company_id = (select id from org_company where code = 'FVI');

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr from acc_rule r,
(values
  ('OPS_INCENTIVE_ACCRUE',  1, 'DEBIT',  '1230', 'INCENTIVE', true,  'Incentive receivable from the insurer'),
  ('OPS_INCENTIVE_ACCRUE',  2, 'CREDIT', '4130', 'INCENTIVE', false, 'Incentive income'),
  ('OPS_INCENTIVE_PASS_ON', 1, 'DEBIT',  '4130', 'PASS_ON',   false, 'Incentive passed on to branches'),
  ('OPS_INCENTIVE_PASS_ON', 2, 'CREDIT', '2230', 'PASS_ON',   false, 'Due to branches - incentive pass-on')
) as l(ev, n, side, acc, comp, party, narr)
where r.event_type = l.ev
  and r.company_id = (select id from org_company where code = 'FVI');

-- ---------- Incentive schemes, seeded without rules (CMRID.005/006, OQ39) ---------------------
insert into cmr_incentive_scheme (company_id, code, name, scheme_type, calculation, period_type, beneficiary,
    active, description, created_at, created_by)
select c.id, s.code, s.name, s.scheme_type, s.calculation, s.period_type, s.beneficiary, false, s.description,
       now(), 'SYSTEM'
from org_company c
cross join (values
    ('NO_TOUCH', 'No Touch', 'NO_TOUCH', 'TARGET_TIERED', 'SEMI_ANNUAL', 'BDOI',
     'Retail / Corporate production targets with rates and multipliers per period - tiers from BDOI (OQ39)'),
    ('TOP_UP', 'Top Up', 'TOP_UP', 'TARGET_TIERED', 'SEMI_ANNUAL', 'BDOI',
     'Retail / Corporate production targets with rates and multipliers per period - tiers from BDOI (OQ39)'),
    ('MOTOR_MANIA', 'Motor Mania', 'MOTOR_MANIA', 'FIXED_PER_POLICY', 'QUARTERLY', 'BRANCH',
     'Fixed amount per policy by minimum basic premium, passed on to branches - amounts from BDOI (OQ39)')
) as s(code, name, scheme_type, calculation, period_type, beneficiary, description)
where c.code = 'FVI'
on conflict (company_id, code) do nothing;
