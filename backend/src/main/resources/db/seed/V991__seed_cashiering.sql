-- =====================================================================================
-- iNXT BrokerVerse - V991 Seed Cashiering (seed profile only). SEED DATA ONLY.
--   * Seed accounting rules of the Cashiering events (OPERATIONS_DESIGN section 5, rows 1-11 and
--     14-15). The real accounts come from Comptrollership (OQ07).
--   * Seed account 4190 Other Operating Income for "Others" official receipts.
--   * Receipt series (BIR ATP ranges parked, OQ05): AR for Head Office and Cebu, OR for Head
--     Office only (CSHID.006), authorized.
--   * Two check pick-up requests from Collection (CSHID.009) on seed accounts.
--   The payments, receipts, applications and 2307 tags of the storyline are posted at start-up
--   by cashiering.seed.CashieringSeedData through the real services, after the ledger replay.
-- =====================================================================================

insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, '4190', 'Other Operating Income - Operations', 'INCOME', 'MAIN',
       (select p.id from coa_account p where p.company_id = c.id and p.code = '4000'),
       (select id from coa_category where code = 'PREM'), true, false, 'NONE', false, false, false, false,
       false, false, 'Commission Income', date '2026-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = '4190');

-- ---------- Seed accounting rules ---------------------------------------------------------------
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('OPS_AR_RECEIPT', 'AR - cash in to unapplied collections (seed)'),
    ('OPS_PAYMENT_APPLY', 'Payment application by PR component (seed)'),
    ('OPS_CWT_RECLASS', 'PR to PR2307 reclass (seed)'),
    ('OPS_CWT_DTIP_OFFSET', 'PR2307 offset against DTIP (seed)'),
    ('OPS_EXCESS_TO_OVERAGES', 'Minimal excess to AP overages (seed)'),
    ('OPS_MINIMAL_BALANCE_REVERSAL', 'PR minimal balance reversal (seed)'),
    ('OPS_UNAPPLIED_REFUND', 'Unapplied refund payable (seed)'),
    ('OPS_UNAPPLIED_RECLASS', 'Unapplied reclass or transfer (seed)'),
    ('OPS_RECEIPT_REINSTATE', 'Receipt reinstatement (seed)'),
    ('OPS_AR_INSURANCE_RECEIPT', 'Insurer non-premium receipt to AR Insurer (seed)'),
    ('OPS_OR_ISSUE', 'Official receipt - income, VAT and CWT (seed)')
) as e(code, name)
where c.code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr
from acc_rule r
join (values
  ('OPS_AR_RECEIPT', 1, 'DEBIT', '@BANK', 'AMOUNT', false, 'Collection'),
  ('OPS_AR_RECEIPT', 2, 'CREDIT', '2205', 'AMOUNT', true, 'Unapplied collections'),
  ('OPS_PAYMENT_APPLY', 1, 'DEBIT', '2205', 'APPLIED', true, 'Unapplied collections applied'),
  ('OPS_PAYMENT_APPLY', 2, 'CREDIT', '1210.02', 'PR_DST', true, 'Premium receivable - DST'),
  ('OPS_PAYMENT_APPLY', 3, 'CREDIT', '1210.03', 'PR_PTX_VAT', true, 'Premium receivable - premium tax / VAT'),
  ('OPS_PAYMENT_APPLY', 4, 'CREDIT', '1210.04', 'PR_LGT', true, 'Premium receivable - LGT'),
  ('OPS_PAYMENT_APPLY', 5, 'CREDIT', '1210.05', 'PR_FST', true, 'Premium receivable - fire service tax'),
  ('OPS_PAYMENT_APPLY', 6, 'CREDIT', '1210.06', 'PR_OTHER', true, 'Premium receivable - other charges'),
  ('OPS_PAYMENT_APPLY', 7, 'CREDIT', '1210.01', 'PR_BASIC', true, 'Premium receivable - basic premium'),
  ('OPS_PAYMENT_APPLY', 8, 'DEBIT', '2220', 'REALIZED_COMMISSION', false, 'Commission realized on collection'),
  ('OPS_PAYMENT_APPLY', 9, 'CREDIT', '4101', 'REALIZED_COMMISSION', false, 'Commission income'),
  ('OPS_PAYMENT_APPLY', 10, 'DEBIT', '2221', 'REALIZED_VAT', false, 'Deferred output VAT made due'),
  ('OPS_PAYMENT_APPLY', 11, 'CREDIT', '2504', 'REALIZED_VAT', false, 'Output VAT on commission'),
  ('OPS_CWT_RECLASS', 1, 'DEBIT', '1211', 'PR2307', true, 'PR - CWT 2307'),
  ('OPS_CWT_RECLASS', 2, 'CREDIT', '1210.02', 'PR_DST', true, 'Premium receivable - DST'),
  ('OPS_CWT_RECLASS', 3, 'CREDIT', '1210.03', 'PR_PTX_VAT', true, 'Premium receivable - premium tax / VAT'),
  ('OPS_CWT_RECLASS', 4, 'CREDIT', '1210.04', 'PR_LGT', true, 'Premium receivable - LGT'),
  ('OPS_CWT_RECLASS', 5, 'CREDIT', '1210.05', 'PR_FST', true, 'Premium receivable - fire service tax'),
  ('OPS_CWT_RECLASS', 6, 'CREDIT', '1210.06', 'PR_OTHER', true, 'Premium receivable - other charges'),
  ('OPS_CWT_RECLASS', 7, 'CREDIT', '1210.01', 'PR_BASIC', true, 'Premium receivable - basic premium'),
  ('OPS_CWT_DTIP_OFFSET', 1, 'DEBIT', '2210', 'DTIP', true, 'Due to insurer settled by 2307'),
  ('OPS_CWT_DTIP_OFFSET', 2, 'CREDIT', '1211', 'PR2307', true, 'PR - CWT 2307 settled'),
  ('OPS_EXCESS_TO_OVERAGES', 1, 'DEBIT', '2205', 'AMOUNT', true, 'Unapplied collections'),
  ('OPS_EXCESS_TO_OVERAGES', 2, 'CREDIT', '2215', 'AMOUNT', false, 'AP overages'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 1, 'DEBIT', '6510', 'TOTAL', false, 'Minimal balance reversed'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 2, 'CREDIT', '1210.02', 'PR_DST', true, 'Premium receivable - DST'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 3, 'CREDIT', '1210.03', 'PR_PTX_VAT', true, 'Premium receivable - premium tax / VAT'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 4, 'CREDIT', '1210.04', 'PR_LGT', true, 'Premium receivable - LGT'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 5, 'CREDIT', '1210.05', 'PR_FST', true, 'Premium receivable - fire service tax'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 6, 'CREDIT', '1210.06', 'PR_OTHER', true, 'Premium receivable - other charges'),
  ('OPS_MINIMAL_BALANCE_REVERSAL', 7, 'CREDIT', '1210.01', 'PR_BASIC', true, 'Premium receivable - basic premium'),
  ('OPS_UNAPPLIED_REFUND', 1, 'DEBIT', '2205', 'AMOUNT', true, 'Unapplied collections refunded'),
  ('OPS_UNAPPLIED_REFUND', 2, 'CREDIT', '2216', 'AMOUNT', true, 'Refund payable to client'),
  ('OPS_UNAPPLIED_RECLASS', 1, 'DEBIT', '2205', 'RELEASED', true, 'Unapplied collections released'),
  ('OPS_UNAPPLIED_RECLASS', 2, 'CREDIT', '2205', 'ASSIGNED', true, 'Unapplied collections assigned'),
  ('OPS_RECEIPT_REINSTATE', 1, 'DEBIT', '@BANK', 'AMOUNT', false, 'Reinstated collection'),
  ('OPS_RECEIPT_REINSTATE', 2, 'CREDIT', '2205', 'AMOUNT', true, 'Unapplied collections'),
  ('OPS_AR_INSURANCE_RECEIPT', 1, 'DEBIT', '@BANK', 'AMOUNT', false, 'Insurer payment'),
  ('OPS_AR_INSURANCE_RECEIPT', 2, 'CREDIT', '1225', 'AMOUNT', true, 'AR Insurer'),
  ('OPS_OR_ISSUE', 1, 'DEBIT', '@BANK', 'CASH', false, 'Collection'),
  ('OPS_OR_ISSUE', 2, 'DEBIT', '1602', 'CWT', false, 'Creditable withholding tax'),
  ('OPS_OR_ISSUE', 3, 'CREDIT', '4110', 'SERVICE_FEE_INCOME', false, 'Service fee income'),
  ('OPS_OR_ISSUE', 4, 'CREDIT', '4120', 'PROFIT_SHARE_INCOME', false, 'Profit share income'),
  ('OPS_OR_ISSUE', 5, 'CREDIT', '4130', 'INCENTIVE_INCOME', false, 'Incentive income'),
  ('OPS_OR_ISSUE', 6, 'CREDIT', '4190', 'OTHER_INCOME', false, 'Other operating income'),
  ('OPS_OR_ISSUE', 7, 'CREDIT', '2504', 'OUTPUT_VAT', false, 'Output VAT'),
  ('OPS_OR_ISSUE', 8, 'CREDIT', '1220', 'COMMISSION_COLLECTED', true, 'Commission receivable collected'),
  ('OPS_OR_ISSUE', 9, 'DEBIT', '2221', 'VAT_DUE', false, 'Deferred output VAT made due'),
  ('OPS_OR_ISSUE', 10, 'CREDIT', '2504', 'VAT_DUE', false, 'Output VAT on commission')
) as l(event_type, n, side, acc, comp, party, narr) on l.event_type = r.event_type
join org_company c on c.id = r.company_id and c.code = 'FVI';

-- ---------- Receipt series (CSHID.006/015; ATP ranges parked, OQ05) --------------------------
insert into csh_receipt_series (company_id, branch_id, kind, atp_no, prefix, from_no, to_no, next_no, warn_at,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, b.id, s.kind, s.atp, s.prefix, s.from_no, s.to_no, s.from_no, s.warn, 'ACTIVE', 'SYSTEM', now(),
       now(), 'SYSTEM'
from org_company c
join org_branch b on b.company_id = c.id
join (values
    ('HO', 'AR', 'ATP-2026-AR-HO', 'AR-HO-', 1, 999999, 500),
    ('CEB', 'AR', 'ATP-2026-AR-CEB', 'AR-CEB-', 1, 99999, 200),
    ('HO', 'OR', 'ATP-2026-OR-HO', 'OR-HO-', 100001, 100600, 100)
) as s(branch, kind, atp, prefix, from_no, to_no, warn) on s.branch = b.code
where c.code = 'FVI';

-- ---------- Check pick-up requests from Collection (CSHID.009) --------------------------------
insert into csh_pickup_request (company_id, branch_id, collection_ref, reference, client_code, payor_name,
    assured_name, pickup_date, requested_at, requestor, amount, currency, check_no, check_bank, status,
    created_at, created_by)
select c.id, b.id, p.ref, p.arn, p.client, p.payor, p.payor, p.pickup, now(), 'Miguel Marketing Collection',
       p.amount, 'PHP', p.check_no, 'BDO', 'FOR_PICKUP', now(), 'SYSTEM'
from org_company c
join org_branch b on b.company_id = c.id and b.code = 'HO'
cross join (values
    ('COL-PU-2026-0001', 'ARN-2026-940004', 'CL-2026-000003', 'Pacific Harbor Logistics Inc.', date '2026-09-28',
     20000.00, '0012345'),
    ('COL-PU-2026-0002', 'ARN-2026-940006', 'CL-2026-000006', 'Carmela Isabel Villanueva', date '2026-10-05',
     13361.25, '0012399')
) as p(ref, arn, client, payor, pickup, amount, check_no)
where c.code = 'FVI';

-- ---------- Collection accounts of the seed chart ---------------------------------------------
update sys_parameter set param_value = '1111' where param_key = 'CASH_BANK_ACCOUNT' and param_value = '';
update sys_parameter set param_value = '1101' where param_key = 'CASH_ON_HAND_ACCOUNT' and param_value = '';
