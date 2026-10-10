-- =====================================================================================
-- iNXT BrokerVerse - V2751 Seed Cashiering: accounting rules of BDOI's cancellation and
-- reinstatement records (seed profile only; the real accounts come from Comptrollership).
--   * Seed account 2207 AP Unapplied Commission.
--   * Rules of OPS_AR_INSURER_REFUND, OPS_COMMISSION_PAYMENT_HELD and OPS_AP_UNAPPLIED_COMMISSION.
-- =====================================================================================

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '2207', 'AP Unapplied Commission', 'LIABILITY', 'SUB',
       (select p.id from coa_account p where p.company_id = c.id and p.code = '2200'),
       (select id from coa_category where code = 'PAYB'), true, false, 'NONE', false, false, false, false,
       true, false, 'Insurance Payables', date '2026-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = '2207');

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('OPS_AR_INSURER_REFUND', 'Reversed payment to AR Insurer Refund (seed)'),
    ('OPS_COMMISSION_PAYMENT_HELD', 'Cancelled OR payment kept for Commission Receivable (seed)'),
    ('OPS_AP_UNAPPLIED_COMMISSION', 'Reinstated OR payment to AP Unapplied Commission (seed)')
) as e(code, name)
where c.code = 'FVI'
  and not exists (select 1 from acc_rule r where r.company_id = c.id and r.event_type = e.code);

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr
from acc_rule r
join (values
  ('OPS_AR_INSURER_REFUND', 1, 'DEBIT', '1225', 'AMOUNT', true, 'AR Insurer - refund of remitted premium'),
  ('OPS_AR_INSURER_REFUND', 2, 'CREDIT', '2205', 'AMOUNT', true, 'Unapplied collections'),
  ('OPS_COMMISSION_PAYMENT_HELD', 1, 'DEBIT', '@BANK', 'AMOUNT', false, 'Payment of the cancelled OR'),
  ('OPS_COMMISSION_PAYMENT_HELD', 2, 'CREDIT', '2207', 'AMOUNT', false, 'Commission Receivable payment'),
  ('OPS_AP_UNAPPLIED_COMMISSION', 1, 'DEBIT', '1220', 'AMOUNT', true, 'Commission receivable outstanding again'),
  ('OPS_AP_UNAPPLIED_COMMISSION', 2, 'CREDIT', '2207', 'AMOUNT', false, 'AP Unapplied Commission')
) as l(event_type, n, side, acc, comp, party, narr) on l.event_type = r.event_type
join org_company c on c.id = r.company_id and c.code = 'FVI'
where not exists (select 1 from acc_rule_line x where x.rule_id = r.id);
