-- =====================================================================================
-- iNXT BrokerVerse - V992 Demo Remittance (demo profile only). DEMO ONLY.
--   * Demo accounting rules of OPS_REMITTANCE and OPS_REMIT_INCENTIVE (OPERATIONS_DESIGN 5
--     rows 12 and 13) on the demo chart; the real accounts come from Comptrollership (OQ07).
--   * One sample early-remittance incentive rule (RMTID.023, PRCID.028): 2% of the basic
--     premium for MGIC CBG fire accounts remitted within 30 days of inception. The real rates
--     are parked (OQ23).
--   The demo storyline itself (hold, special remittance request, extraction run) is created at
--   start-up by remittance.demo.RemittanceDemoData on the invoices of the booked demo accounts.
-- =====================================================================================

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'OPS_REMITTANCE', 'Remittance to insurer - DTIP, CWT, commission and net due (demo)', 100,
       date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr from acc_rule r,
(values
  (1, 'DEBIT',  '2210', 'DTIP',                  true,  'Due to insurer - paid AR remitted'),
  (2, 'DEBIT',  '1602', 'CWT',                   false, 'Withholding tax withheld by the insurer on commission'),
  (3, 'CREDIT', '1220', 'COMMISSION_RECEIVABLE', true,  'Commission and VAT retained from the remittance'),
  (4, 'CREDIT', '2211', 'DUE_FOR_DISBURSEMENT',  true,  'Net due to insurer - for disbursement')
) as l(n, side, acc, comp, party, narr)
where r.event_type = 'OPS_REMITTANCE';

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select id, 'OPS_REMIT_INCENTIVE', 'Early remittance incentive (demo)', 100,
       date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company where code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr from acc_rule r,
(values
  (1, 'DEBIT',  '2211', 'DUE_FOR_DISBURSEMENT', true,  'Incentive deducted from the remittance'),
  (2, 'CREDIT', '4130', 'INCENTIVE_INCOME',     false, 'Early remittance incentive income'),
  (3, 'CREDIT', '2504', 'OUTPUT_VAT',           false, 'Output VAT on the incentive')
) as l(n, side, acc, comp, party, narr)
where r.event_type = 'OPS_REMIT_INCENTIVE';

insert into rem_incentive_rule (company_id, insurer_code, product_line, segment, rate, window_days, basis,
    effective_from, effective_to, active, description, created_at, created_by)
select id, 'INS-MGIC', 'PROPERTY', 'CBG', 2.0000, 30, 'INCEPTION', date '2026-01-01', null, true,
       'Demo: 2% early remittance incentive, CBG fire, within 30 days of inception (rates parked, OQ23)',
       now(), 'SYSTEM'
from org_company where code = 'FVI';
