-- SIT/UAT seed data (seed profile only) for the reports that returned no rows with their standard
-- variant: the processes behind them now have records in the seed story of the run date. Every record
-- belongs to the existing seed clients, insurers, accounts and users; the dates follow the run date
-- (the standard variants read this month, the year to date or today). The records use number ranges
-- of their own (Cebu branch receipt series, 0009xx running numbers) so that the start-up seed and
-- the processes of SIT and UAT never meet them.

-- =====================================================================================================
-- 1. Cashiering (Cebu branch): cancelled and reinstated receipts, minimal balances, payment reversal,
--    re-application, certification of payment, 2307 batch, payment upload batch and the legacy
--    unapplied payments reclassified to income.
-- =====================================================================================================
insert into csh_receipt_series (company_id, branch_id, kind, atp_no, prefix, from_no, to_no, next_no, warn_at,
    record_status, authorized_by, authorized_at, created_at, created_by, origin)
select c.id, b.id, 'OR', 'ATP-' || to_char(current_date, 'YYYY') || '-OR-CEB', 'OR-CEB-', 200001, 200600, 200003, 100,
       'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM', 'BIBS'
from org_company c join org_branch b on b.company_id = c.id and b.code = 'CEB'
where c.code = 'FVI'
  and not exists (select 1 from csh_receipt_series s where s.branch_id = b.id and s.kind = 'OR');

create temporary table seed_csh_receipt (
    receipt_no varchar(40), kind varchar(2), receipt_class varchar(30), days_ago integer, payor_code varchar(30),
    payor_name varchar(250), assured_name varchar(250), sales_unit varchar(40), amount numeric(19, 2),
    gross numeric(19, 2), vat numeric(19, 2), wtax numeric(19, 2), payment_mode varchar(20), check_no varchar(40),
    check_bank varchar(60), status varchar(20), reinstated numeric(19, 2), remarks varchar(250)
) on commit drop;

insert into seed_csh_receipt values
('AR-CEB-000001', 'AR', 'OTC', 34, 'CL-2026-000003', 'Pacific Harbor Logistics Inc.', 'Pacific Harbor Logistics Inc.',
 'T-CBG1', 35840.00, 0, 0, 0, 'CHECK', '0004417', 'BDO Unibank', 'CANCELLED', null,
 'Cancelled: check drawn on the wrong account; the client paid again by bank transfer'),
('AR-CEB-000002', 'AR', 'OTC', 27, 'CL-2026-000006', 'Villanueva, Carmela Isabel Santos',
 'Villanueva, Carmela Isabel Santos', 'T-CBG1', 13361.25, 0, 0, 0, 'CASH', null, null, 'CANCELLED', null,
 'Cancelled: issued to the wrong payor name'),
('AR-CEB-000003', 'AR', 'OTC', 21, 'CL-2026-000004', 'Luzon Agri-Industrial Corp.', 'Luzon Agri-Industrial Corp.',
 'T-CBG1', 48250.00, 0, 0, 0, 'CHECK', '0118823', 'Metrobank', 'REINSTATED', 48250.00,
 'Reinstated in full after the check cleared on re-deposit'),
('AR-CEB-000004', 'AR', 'OTC', 12, 'CL-2026-000002', 'Reyes, Jose Miguel Lopez', 'Reyes, Jose Miguel Lopez',
 'T-CBG1', 19351.13, 0, 0, 0, 'CASH', null, null, 'ISSUED', null, null),
('AR-CEB-000005', 'AR', 'OTC', 9, 'CL-2026-000005', 'Garcia, Antonio Luis Dizon', 'Garcia, Antonio Luis Dizon',
 'T-CBG1', 19087.50, 0, 0, 0, 'CHECK', '0220145', 'BPI', 'ISSUED', null, null),
('AR-CEB-000006', 'AR', 'OTC', 6, 'CL-2026-000001', 'Santos, Maria Clara Reyes', 'Santos, Maria Clara Reyes',
 'T-CBG1', 28506.63, 0, 0, 0, 'CASH', null, null, 'REINSTATED', 12000.00,
 'Partly reinstated: PHP 12,000.00 of the cancelled receipt applies to the renewal premium'),
('OR-CEB-200001', 'OR', 'SERVICE_FEE', 30, 'CL-2026-000004', 'Luzon Agri-Industrial Corp.', null, null,
 16800.00, 15000.00, 1800.00, 0, 'CHECK', '0330912', 'Security Bank', 'CANCELLED', null,
 'Cancelled: service fee billed twice for the same risk survey'),
('OR-CEB-200002', 'OR', 'COMMISSION', 16, 'INS-LAC', 'Luzon Assurance Co.', null, null,
 9847.43, 9654.34, 1158.52, 965.43, 'NON_CASH', null, null, 'CANCELLED', null,
 'Cancelled: commission receipt issued before the insurer confirmed the net amount');

insert into csh_receipt (company_id, branch_id, receipt_no, kind, receipt_class, series_id, receipt_date, payor_code,
    payor_name, assured_name, sales_unit, currency, book_rate, amount, base_amount, gross, vat, wtax, applied_amount,
    payment_mode, check_no, check_bank, check_date, source, status, reinstated_amount, printed_count,
    remarks, created_at, created_by)
select c.id, b.id, r.receipt_no, r.kind, r.receipt_class, s.id, current_date - r.days_ago, r.payor_code,
       r.payor_name, r.assured_name, r.sales_unit, 'PHP', 1.00, r.amount, r.amount, r.gross, r.vat, r.wtax, 0,
       r.payment_mode, r.check_no, r.check_bank, case when r.check_no is null then null else current_date - r.days_ago end,
       'OTC', r.status, r.reinstated, 1, r.remarks,
       (current_date - r.days_ago) + time '09:30' at time zone 'Asia/Manila', 'cashbr'
from seed_csh_receipt r
join org_company c on c.code = 'FVI'
join org_branch b on b.company_id = c.id and b.code = 'CEB'
join csh_receipt_series s on s.branch_id = b.id and s.kind = r.kind
where not exists (select 1 from csh_receipt x where x.receipt_no = r.receipt_no);

update csh_receipt_series s set next_no = 7
from org_branch b where b.id = s.branch_id and b.code = 'CEB' and s.kind = 'AR' and s.next_no < 7;

insert into csh_receipt_line (receipt_id, line_no, invoice_no, insurer_code, gross, vat, wtax, net, description)
select r.id, 0, case when r.receipt_class = 'COMMISSION' then 'BI-HO-2026-000004' end,
       case when r.receipt_class = 'COMMISSION' then 'INS-LAC' end, r.gross, r.vat, r.wtax, r.amount,
       case when r.receipt_class = 'COMMISSION' then 'Commission on the general liability policy of Pacific Harbor'
            else 'Risk survey and loss control report' end
from csh_receipt r
where r.receipt_no in ('OR-CEB-200001', 'OR-CEB-200002')
  and not exists (select 1 from csh_receipt_line l where l.receipt_id = r.id);

-- Cancellations and reinstatements with the approval of the Cashiering Lead (posted) and one waiting.
insert into csh_receipt_action (company_id, receipt_id, transaction_no, action, reason_code, reason_text, amount,
    invoice_no, document_no, payor_name, account_officer, unit_head, team_leader, stage, approved_by, approved_at,
    journal_batch_no, created_at, created_by)
select r.company_id, r.id, a.tx || to_char(current_date, 'YYYY') || a.seq, a.action, a.reason, a.reason_text, a.amount,
       a.invoice_no, a.document_no, r.payor_name, 'ao', 'Marites Marketing Lead', 'Marites Marketing Lead', a.stage,
       case when a.stage = 'POSTED' then 'cashtl' end,
       case when a.stage = 'POSTED' then (current_date - a.posted_ago) + time '15:10' at time zone 'Asia/Manila' end,
       case when a.stage = 'POSTED' then 'RCT-CEB-' || to_char(current_date, 'YYYY') || a.seq end,
       (current_date - a.requested_ago) + time '11:20' at time zone 'Asia/Manila', 'cashbr'
from (values
    ('AR-CEB-000001', 'CAN-', '-000901', 'CANCEL', 'PRM_INCORRECT_CHECK', 'Check drawn on the wrong account',
     35840.00, null, null, 'POSTED', 33, 33),
    ('AR-CEB-000002', 'CAN-', '-000902', 'CANCEL', 'PRM_INCORRECT_PAYEE', 'Wrong payor name on the receipt',
     13361.25, null, null, 'POSTED', 26, 26),
    ('AR-CEB-000003', 'CAN-', '-000903', 'CANCEL', 'GEN_BOUNCED_CHECK', 'Check returned for insufficient funds',
     48250.00, null, null, 'POSTED', 20, 20),
    ('AR-CEB-000003', 'RIN-', '-000901', 'REINSTATE_FULL', 'PRM_OTHERS', 'Check cleared on re-deposit',
     48250.00, 'BI-HO-2026-000004', 'DEP-CEB-0412', 'POSTED', 14, 13),
    ('AR-CEB-000006', 'CAN-', '-000904', 'CANCEL', 'GEN_ISSUANCE_ERROR', 'Amount keyed twice',
     28506.63, null, null, 'POSTED', 5, 5),
    ('AR-CEB-000006', 'RIN-', '-000902', 'REINSTATE_PARTIAL', 'PRM_MISAPPLICATION',
     'Part of the amount applies to the renewal premium', 12000.00,
     'BI-HO-2026-000003', null, 'FOR_APPROVAL', 3, null),
    ('OR-CEB-200001', 'CAN-', '-000905', 'CANCEL', 'GEN_DOUBLE_ISSUANCE', 'Service fee billed twice',
     16800.00, null, null, 'POSTED', 29, 28),
    ('OR-CEB-200002', 'CAN-', '-000906', 'CANCEL', 'COM_INCORRECT_DETAILS', 'Issued before the insurer confirmed',
     9847.43, null, null, 'POSTED', 15, 15)
) as a(receipt_no, tx, seq, action, reason, reason_text, amount, invoice_no, document_no, stage, requested_ago,
       posted_ago)
join csh_receipt r on r.receipt_no = a.receipt_no
where not exists (select 1 from csh_receipt_action x where x.transaction_no = a.tx || to_char(current_date, 'YYYY') || a.seq);

-- Minimal balances swept by the month-end sweep (premium, commission and excess payments).
insert into csh_minimal_balance (company_id, kind, subject_ref, invoice_no, client_code, sales_unit, amount,
    components, journal_batch_no, swept_on, swept_at, swept_by)
select c.id, m.kind, m.subject_ref, m.invoice_no, m.client_code, m.sales_unit, m.amount, m.components,
       'MBS-' || to_char(current_date, 'YYYY') || m.seq, current_date - m.days_ago,
       (current_date - m.days_ago) + time '18:05' at time zone 'Asia/Manila', 'cashtl'
from (values
    ('PREMIUM', 'BI-HO-2026-000001:BASIC', 'BI-HO-2026-000001', 'CL-2026-000001', 'T-CBG1', 0.86,
     'Basic premium 0.86', '-000901', 8),
    ('PREMIUM', 'BI-HO-2026-000003:BASIC', 'BI-HO-2026-000003', 'CL-2026-000001', 'T-CBG1', 0.63,
     'Basic premium 0.50; documentary stamp tax 0.13', '-000901', 8),
    ('PREMIUM', 'BI-HO-2026-000004:BASIC', 'BI-HO-2026-000004', 'CL-2026-000003', 'T-CBG2', 0.25,
     'Basic premium 0.25', '-000902', 2),
    ('COMMISSION', 'BI-HO-2026-000002:COMMISSION', 'BI-HO-2026-000002', 'CL-2026-000005', 'T-CBG1', 0.42,
     'Commission 0.38; VAT on commission 0.04', '-000901', 8),
    ('COMMISSION', 'BI-HO-2026-000004:COMMISSION', 'BI-HO-2026-000004', 'CL-2026-000003', 'T-CBG2', 0.17,
     'Commission 0.17', '-000902', 2),
    ('EXCESS', 'UNP-2026-000901', null, 'CL-2026-000006', 'T-CBG1', 0.75, 'Excess payment 0.75', '-000901', 8),
    ('EXCESS', 'UNP-2026-000902', null, 'CL-2026-000002', 'T-CBG1', 0.40, 'Excess payment 0.40', '-000902', 2)
) as m(kind, subject_ref, invoice_no, client_code, sales_unit, amount, components, seq, days_ago)
join org_company c on c.code = 'FVI'
where not exists (select 1 from csh_minimal_balance x where x.kind = m.kind and x.subject_ref = m.subject_ref);

-- The excess payments behind the swept excess balances.
insert into csh_unapplied (company_id, branch_id, reference, origin, receipt_id, invoice_no, client_code, payor_name,
    sales_unit, currency, amount, balance, stage, disposition_hint, source_module, source_ref, remarks, created_at,
    created_by, ledger_context)
select r.company_id, r.branch_id, u.reference, 'EXCESS', r.id, u.invoice_no, r.payor_code, r.payor_name, 'T-CBG1',
       'PHP', u.amount, 0, 'COMPLETED', 'Minimal balance', 'CASHIERING', 'SEED-RPT:' || u.reference,
       'Swept to miscellaneous income by the minimal balance sweep', r.created_at, 'cashbr', 'NEW'
from (values
    ('UNP-2026-000901', 'AR-CEB-000002', null, 0.75),
    ('UNP-2026-000902', 'AR-CEB-000004', 'BI-HO-2026-000008', 0.40)
) as u(reference, receipt_no, invoice_no, amount)
join csh_receipt r on r.receipt_no = u.receipt_no
where not exists (select 1 from csh_unapplied x where x.reference = u.reference);

-- Applications of the Cebu receipts; two were reversed (bounced check, wrong invoice).
insert into csh_application (company_id, invoice_no, arn, client_code, receipt_id, source, source_ref, amount,
    realized_commission, realized_vat, value_date, status, reversed_at, reversal_ref, reversal_reason,
    journal_batch_no, reversal_journal_no, created_at, created_by)
select r.company_id, a.invoice_no, a.arn, r.payor_code, r.id, 'PAYMENT', a.source_ref, a.amount, a.commission, a.vat,
       current_date - a.days_ago, a.status,
       case when a.status = 'REVERSED' then (current_date - a.reversed_ago) + time '16:40' at time zone 'Asia/Manila' end,
       case when a.status = 'REVERSED' then 'REV-' || to_char(current_date, 'YYYY') || a.seq end,
       a.reason, 'RCT-CEB-' || to_char(current_date, 'YYYY') || a.seq,
       case when a.status = 'REVERSED' then 'REV-CEB-' || to_char(current_date, 'YYYY') || a.seq end,
       (current_date - a.days_ago) + time '10:00' at time zone 'Asia/Manila', 'cashbr'
from (values
    ('AR-CEB-000003', 'BI-HO-2026-000004', 'ARN-2026-940004', 'SEED-RPT:APP-1', 28281.25, 4064.45, 487.73,
     21, 'REVERSED', 20, 'Check returned for insufficient funds', '-000911'),
    ('AR-CEB-000004', 'BI-HO-2026-000008', 'ARN-2026-940008', 'SEED-RPT:APP-2', 19350.73, 2703.69, 324.44,
     12, 'ACTIVE', null, null, '-000912'),
    ('AR-CEB-000005', 'BI-HO-2026-000003', 'ARN-2026-940003', 'SEED-RPT:APP-3', 19087.50, 2668.45, 320.21,
     9, 'REVERSED', 7, 'Applied to the wrong invoice; re-applied to the client''s fire policy', '-000913'),
    ('AR-CEB-000005', 'BI-HO-2026-000007', 'ARN-2026-000002', 'SEED-RPT:APP-4', 19087.50, 2668.45, 320.21,
     7, 'ACTIVE', null, null, '-000914')
) as a(receipt_no, invoice_no, arn, source_ref, amount, commission, vat, days_ago, status, reversed_ago, reason, seq)
join csh_receipt r on r.receipt_no = a.receipt_no
where not exists (select 1 from csh_application x where x.source_ref = a.source_ref);

-- Re-applications of payments released by an adjustment or a cancellation.
insert into csh_reapplication (company_id, invoice_no, source_module, source_ref, reason, unapplied, excess,
    receipt_nos, unapplied_ref, created_at, created_by)
select c.id, ra.invoice_no, ra.module, ra.source_ref, ra.reason, ra.unapplied, ra.excess, ra.receipts, ra.unapplied_ref,
       (current_date - ra.days_ago) + time '14:15' at time zone 'Asia/Manila', 'cashbr'
from (values
    ('BI-HO-2026-000007', 'ADJUSTMENT', 'SEED-RPT:ADJ-2026-000901',
     'Refund of the cancelled fire policy re-applied to the new fire policy of the client', 10095.49, 0.00, null,
     'UNP-2026-000903', 8),
    ('BI-HO-2026-000007', 'CASHIERING', 'SEED-RPT:REV-2026-000913', 'Payment reversed from the wrong invoice and re-applied',
     19087.50, 0.00, 'AR-CEB-000005', null, 7),
    ('BI-HO-2026-000004', 'CASHIERING', 'SEED-RPT:RIN-2026-000901', 'Reinstated receipt re-applied to the invoice',
     28281.25, 0.00, 'AR-CEB-000003', null, 13)
) as ra(invoice_no, module, source_ref, reason, unapplied, excess, receipts, unapplied_ref, days_ago)
join org_company c on c.code = 'FVI'
where not exists (select 1 from csh_reapplication x where x.source_module = ra.module and x.source_ref = ra.source_ref
                    and x.invoice_no = ra.invoice_no);

-- Certificates of payment issued on request of the units.
insert into csh_certificate_of_payment (company_id, receipt_id, receipt_no, policy_no, requesting_unit, issued_at,
    issued_by)
select r.company_id, r.id, r.receipt_no, cp.policy_no, cp.unit,
       (current_date - cp.days_ago) + time '13:30' at time zone 'Asia/Manila', 'cashbr'
from (values
    ('AR-CEB-000004', 'MGIC-MC-2026-98808', 'Marketing - Corporate Banking Group 1', 10),
    ('AR-CEB-000005', 'MGIC-FI-2026-98802', 'Claims', 6),
    ('AR-CEB-000003', 'LAC-GL-2026-98804', 'Marketing - Corporate Banking Group 2', 12)
) as cp(receipt_no, policy_no, unit, days_ago)
join csh_receipt r on r.receipt_no = cp.receipt_no
where not exists (select 1 from csh_certificate_of_payment x where x.receipt_id = r.id and x.policy_no = cp.policy_no);

-- BIR 2307 batch: certificates received from clients, reported and routed to Disbursement.
insert into csh_cwt_batch (company_id, batch_no, insurer_code, tag_count, total_amount, status, routed_at,
    disbursement_request_no, released_at, created_at, created_by)
select c.id, b.batch_no, b.insurer, b.tags, b.total, b.status,
       (current_date - b.routed_ago) + time '10:00' at time zone 'Asia/Manila', b.request_no,
       case when b.status = 'RELEASED' then (current_date - b.routed_ago + 3) + time '10:00' at time zone 'Asia/Manila' end,
       (current_date - b.days_ago) + time '09:00' at time zone 'Asia/Manila', 'cashtl'
from (values
    ('CWB-2026-000901', 'INS-LAC', 2, 2465.63, 'WITH_DISBURSEMENT', 'DRQ-2026-000901', 18, 19),
    ('CWB-2026-000902', 'INS-MGIC', 2, 618.79, 'REPORT_POSTED', null, 4, 4)
) as b(batch_no, insurer, tags, total, status, request_no, routed_ago, days_ago)
join org_company c on c.code = 'FVI'
where not exists (select 1 from csh_cwt_batch x where x.batch_no = b.batch_no);

insert into csh_cwt_tag (company_id, reference, invoice_no, arn, client_code, insurer_code, amount, path,
    certificate_no, period_from, period_to, cwt_copy_received, remitted, stage, batch_id, receipt_no,
    reclass_journal_no, offset_journal_no, remarks, created_at, created_by)
select b.company_id, t.reference, t.invoice_no, t.arn, t.client_code, b.insurer_code, t.amount, 'CERTIFICATE',
       t.certificate_no, date_trunc('quarter', current_date - 92)::date,
       (date_trunc('quarter', current_date) - interval '1 day')::date, true, false, t.stage, b.id, t.receipt_no,
       'CWR-' || to_char(current_date, 'YYYY') || t.seq, t.offset_journal,
       'Client certificate 2307 received', b.created_at, 'mktcoll'
from (values
    ('CWB-2026-000901', 'CWT-2026-000901', 'BI-HO-2026-000004', 'ARN-2026-940004', 'CL-2026-000003', 1565.63,
     '2307-2026-900011', 'BATCHED', 'AR-CEB-000003', '-000901', null),
    ('CWB-2026-000901', 'CWT-2026-000902', 'BI-HO-2026-000004', 'ARN-2026-940004', 'CL-2026-000003', 900.00,
     '2307-2026-900012', 'BATCHED', 'AR-CEB-000003', '-000902', null),
    ('CWB-2026-000902', 'CWT-2026-000903', 'BI-HO-2026-000008', 'ARN-2026-940008', 'CL-2026-000002', 309.40,
     '2307-2026-900013', 'BATCHED', 'AR-CEB-000004', '-000903', null),
    ('CWB-2026-000902', 'CWT-2026-000904', 'BI-HO-2026-000007', 'ARN-2026-000002', 'CL-2026-000005', 309.39,
     '2307-2026-900014', 'BATCHED', 'AR-CEB-000005', '-000904', null)
) as t(batch_no, reference, invoice_no, arn, client_code, amount, certificate_no, stage, receipt_no, seq,
       offset_journal)
join csh_cwt_batch b on b.batch_no = t.batch_no
where not exists (select 1 from csh_cwt_tag x where x.reference = t.reference);

-- Payment upload batches (bills payment and direct credit) with their matching results.
insert into csh_payment (company_id, branch_id, payment_no, channel, batch_ref, source_key, row_no, reference,
    payor_name, assured_name, amount, currency, value_date, paid_time, late_deposit, payment_mode, match_category,
    matched_ref, receipt_id, applied_amount, unapplied_amount, message, created_at, created_by)
select c.id, b.id, p.payment_no, p.channel, p.batch_ref, 'SEED-RPT:' || p.payment_no, p.row_no, p.reference,
       p.payor, p.payor, p.amount, 'PHP', current_date - p.days_ago, '10:15', false, p.mode, p.category, p.matched,
       null, p.applied, p.amount - p.applied, p.message,
       (current_date - p.days_ago) + time '17:30' at time zone 'Asia/Manila', 'cashbr'
from (values
    ('PAY-2026-000901', 'BILLS_PAYMENT', 'BLK-2026-000901', 1, 'ARN-2026-940005', 'Reyes, Jose Miguel Lopez',
     17027.86, 'BILLS_PAYMENT', 'APPLIED', 'BI-HO-2026-000008', 17027.86, 'Applied to the invoice', 11),
    ('PAY-2026-000902', 'BILLS_PAYMENT', 'BLK-2026-000901', 2, 'ARN-2026-940006', 'Villanueva, Carmela Isabel Santos',
     13361.25, 'BILLS_PAYMENT', 'PREBOOKED', 'ARN-2026-940006', 0, 'Held for the account not yet booked', 11),
    ('PAY-2026-000903', 'BILLS_PAYMENT', 'BLK-2026-000901', 3, 'ARN-2026-940003', 'Santos, Maria Clara Reyes',
     30000.00, 'BILLS_PAYMENT', 'EXCESS', 'BI-HO-2026-000003', 28506.63, 'Paid more than the balance', 11),
    ('PAY-2026-000904', 'BILLS_PAYMENT', 'BLK-2026-000901', 4, 'REF 55120', 'R. Santos', 2500.00,
     'BILLS_PAYMENT', 'UNAPPLIED_NO_MATCH', null, 0, 'No account or invoice matches the reference', 11),
    ('PAY-2026-000905', 'DIRECT_CREDIT', 'BLK-2026-000902', 1, 'ARN-2026-940004', 'Pacific Harbor Logistics Inc.',
     28281.25, 'DIRECT_CREDIT', 'APPLIED', 'BI-HO-2026-000004', 28281.25, 'Applied to the invoice', 4),
    ('PAY-2026-000906', 'DIRECT_CREDIT', 'BLK-2026-000902', 2, 'ARN-2026-940002', 'Garcia, Antonio Luis Dizon',
     5000.00, 'DIRECT_CREDIT', 'CANCELLED_REFERENCE', 'ARN-2026-940002', 0,
     'The account of the reference is cancelled', 4)
) as p(payment_no, channel, batch_ref, row_no, reference, payor, amount, mode, category, matched, applied, message,
       days_ago)
join org_company c on c.code = 'FVI'
join org_branch b on b.company_id = c.id and b.code = 'CEB'
where not exists (select 1 from csh_payment x where x.payment_no = p.payment_no);

-- Legacy unapplied payments over the holding period, reclassified to income with two approvals.
insert into csh_unapplied (company_id, branch_id, reference, origin, invoice_no, client_code, payor_name, sales_unit,
    currency, amount, balance, stage, disposition_hint, source_module, source_ref, remarks, created_at, created_by,
    source_system, legacy_ref, migration_batch, ledger_context, legacy_ar_no, legacy_ar_date)
select c.id, b.id, u.reference, 'MIGRATED', null, u.client_code, u.payor, 'T-CBG1', 'PHP', u.amount, 0, 'COMPLETED',
       'Reclassify to income', 'MIGRATION', 'MIG:UPP:EBIX:' || u.legacy_ref,
       'Unapplied for more than the holding period; reclassified to income', now() - interval '20 days', 'mig-loader',
       'EBIX', u.legacy_ref, 'MIG-2026-UPP-01', 'LEGACY', u.legacy_ar, u.legacy_date
from (values
    ('UNP-2026-000911', 'CL-2026-000101', 'Abad, Adrian', 850.00, 'UPP970911', 'AR-0098812', date '2024-03-14'),
    ('UNP-2026-000912', 'CL-2026-000004', 'Luzon Agri-Industrial Corp.', 3120.50, 'UPP970912', 'AR-0101457',
     date '2024-06-02'),
    ('UNP-2026-000913', null, 'Walk-in payor (no reference)', 600.00, 'UPP970913', 'AR-0104420', date '2024-08-19')
) as u(reference, client_code, payor, amount, legacy_ref, legacy_ar, legacy_date)
join org_company c on c.code = 'FVI'
join org_branch b on b.company_id = c.id and b.code = 'HO'
where not exists (select 1 from csh_unapplied x where x.reference = u.reference);

insert into csh_legacy_batch (company_id, batch_no, kind, status, reason, currency, total, line_count, submitted_by,
    submitted_at, first_approved_by, first_approved_at, final_approved_by, final_approved_at, executed_at,
    posted_count, failed_count, created_at, created_by)
select c.id, 'LUB-2026-000901', 'INCOME_RECLASS', 'EXECUTED',
       'Legacy unapplied payments held beyond the holding period without a claimant', 'PHP', 4570.50, 3,
       'upphandler', now() - interval '12 days', 'cashtl', now() - interval '11 days', 'topmgmt',
       now() - interval '10 days', now() - interval '10 days', 3, 0, now() - interval '13 days', 'upphandler'
from org_company c
where c.code = 'FVI' and not exists (select 1 from csh_legacy_batch x where x.batch_no = 'LUB-2026-000901');

insert into csh_legacy_batch_line (batch_id, line_no, unapplied_id, reference, ledger_context, amount, age_days, reason,
    status, journal_batch_no)
select lb.id, row_number() over (order by u.reference), u.id, u.reference, 'LEGACY', u.amount,
       current_date - u.legacy_ar_date, 'Unclaimed beyond the holding period', 'POSTED',
       'LUI-' || to_char(current_date, 'YYYY') || '-00090' || row_number() over (order by u.reference)
from csh_legacy_batch lb
join csh_unapplied u on u.reference in ('UNP-2026-000911', 'UNP-2026-000912', 'UNP-2026-000913')
where lb.batch_no = 'LUB-2026-000901'
  and not exists (select 1 from csh_legacy_batch_line x where x.batch_id = lb.id);

-- =====================================================================================================
-- 2. Disbursement: vouchers approved and posted this year (remittance, employee, other), checks not
--    released or stale, a debit authority, 2307 certificates of commission, a voucher whose posting
--    failed and an upload of payment requests with rows refused.
-- =====================================================================================================
insert into dsb_payee (company_id, payee_code, payee_class, name, address, email, tin, default_mode, allowed_modes,
    disbursement_types, currency, source, stage, used, created_at, created_by, origin)
select c.id, p.code, p.klass, p.name, p.address, p.email, p.tin, p.mode, p.modes, p.types, 'PHP', 'MANUAL', 'ACTIVE',
       true, now() - interval '200 days', 'disb', 'BIBS'
from (values
    ('INS-LAC', 'INSURER', 'Luzon Assurance Co.', '6788 Ayala Avenue, Makati City', 'treasury@luzonassurance.ph',
     '000-123-456-000', 'CHECK', 'CHECK,ATD', 'REMITTANCE,REFUND'),
    ('INS-VMI', 'INSURER', 'Visayas Mutual Insurance', 'Osmena Boulevard, Cebu City', 'payments@visayasmutual.ph',
     '000-234-567-000', 'ATD', 'ATD,CHECK', 'REMITTANCE'),
    ('S-0002', 'SUPPLIER', 'Cloud Systems Philippines Inc.', 'BGC, Taguig City', 'billing@cloudsystems.ph',
     '009-876-543-000', 'CHECK', 'CHECK', 'SUPPLIER'),
    ('EMP-0412', 'EMPLOYEE', 'Dela Paz, Kristine Mae', 'Cebu Branch', 'kdelapaz@bdoi.com.ph', null, 'CHECK',
     'CHECK', 'EMPLOYEE,CASH_ADVANCE'),
    ('EMP-0388', 'EMPLOYEE', 'Bautista, Ramon Luis', 'Head Office - Makati', 'rbautista@bdoi.com.ph', null, 'CHECK',
     'CHECK', 'EMPLOYEE,CASH_ADVANCE'),
    ('G-0002', 'SUPPLIER', 'Cebu Motor Works', 'Mandaue City, Cebu', 'accounts@cebumotorworks.ph',
     '004-555-121-000', 'CHECK', 'CHECK', 'SUPPLIER,OTHER')
) as p(code, klass, name, address, email, tin, mode, modes, types)
join org_company c on c.code = 'FVI'
where not exists (select 1 from dsb_payee x where x.company_id = c.id and x.payee_code = p.code);

create temporary table seed_dsb (
    seq varchar(6), payee_code varchar(30), dtype varchar(30), mode varchar(20), gross numeric(19, 2),
    ewt numeric(19, 2), purpose varchar(250), stage varchar(20), posting varchar(20), posting_error varchar(250),
    days_ago integer, inst_status varchar(20), inst_no varchar(40), printed_ago integer, source_module varchar(30),
    source_ref varchar(80), request_status varchar(20)
) on commit drop;

insert into seed_dsb values
('000901', 'INS-LAC', 'REMITTANCE', 'CHECK', 64215.80, 0, 'Net premium remittance batch RMB-INS-LAC-2026-000901',
 'APPROVED', 'POSTED', null, 196, 'STALE', '100003', 195, 'REMITTANCE', 'SEED-RPT:RMB-INS-LAC-2026-000901', 'RELEASED'),
('000902', 'INS-LAC', 'REMITTANCE', 'CHECK', 48250.00, 0, 'Net premium remittance batch RMB-INS-LAC-2026-000902',
 'APPROVED', 'POSTED', null, 62, 'RELEASED', '100011', 61, 'REMITTANCE', 'SEED-RPT:RMB-INS-LAC-2026-000902', 'RELEASED'),
('000903', 'INS-VMI', 'REMITTANCE', 'ATD', 132600.00, 0, 'Net premium remittance batch RMB-INS-VMI-2026-000901',
 'APPROVED', 'POSTED', null, 18, 'DEBITED', 'ATD-2026-000901', null, 'REMITTANCE', 'SEED-RPT:RMB-INS-VMI-2026-000901',
 'RELEASED'),
('000904', 'INS-VMI', 'REMITTANCE', 'ATD', 28125.00, 0, 'Net premium remittance batch RMB-INS-VMI-2026-000902',
 'APPROVED', 'POSTED', null, 3, 'EMAILED', 'ATD-2026-000902', null, 'REMITTANCE', 'SEED-RPT:RMB-INS-VMI-2026-000902',
 'RELEASED'),
('000905', 'EMP-0412', 'CASH_ADVANCE', 'CHECK', 15000.00, 0, 'Cash advance for the Cebu client service visits',
 'APPROVED', 'POSTED', null, 25, 'PRINTED', '100014', 24, 'PAYREQUEST', 'SEED-RPT:PRQ-2026-000901', 'RELEASED'),
('000906', 'EMP-0388', 'EMPLOYEE', 'CHECK', 8640.00, 0, 'Reimbursement of transportation and meals, client visits',
 'APPROVED', 'POSTED', null, 11, 'PRINTED', '100015', 10, 'DISBURSEMENT', 'SEED-RPT:DSR-2026-000906', 'RELEASED'),
('000907', 'S-0002', 'OTHER', 'CHECK', 56000.00, 1120.00, 'Annual licence of the document scanning software',
 'APPROVED', 'POSTED', null, 15, 'RELEASED', '100013', 14, 'DISBURSEMENT', 'SEED-RPT:DSR-2026-000907', 'RELEASED'),
('000908', 'G-0002', 'OTHER', 'CHECK', 12500.00, 250.00, 'Repair of the Cebu branch service vehicle', 'APPROVED',
 'FAILED', 'The expense account 6105 is closed for posting in the period', 6, null, null, null, 'DISBURSEMENT',
 'SEED-RPT:DSR-2026-000908', 'IN_VOUCHER');

insert into dsb_request (company_id, request_no, source, source_module, source_ref, disbursement_type, payee_class,
    payee_code, payee_name, payee_id, currency, amount, purpose, received_at, status, created_at, created_by)
select c.id, 'DSR-2026-' || d.seq, case when d.source_module in ('REMITTANCE') then 'GATEWAY'
       when d.source_module = 'PAYREQUEST' then 'PAYREQUEST' else 'ENCODED' end, d.source_module, d.source_ref,
       d.dtype, p.payee_class, p.payee_code, p.name, p.id, 'PHP', d.gross, d.purpose,
       (current_date - d.days_ago - 1) + time '09:00' at time zone 'Asia/Manila', d.request_status,
       (current_date - d.days_ago - 1) + time '09:00' at time zone 'Asia/Manila', 'disb'
from seed_dsb d
join org_company c on c.code = 'FVI'
join dsb_payee p on p.company_id = c.id and p.payee_code = d.payee_code
where not exists (select 1 from dsb_request x where x.request_no = 'DSR-2026-' || d.seq);

insert into dsb_voucher (company_id, branch_id, dv_no, request_id, payee_id, payee_code, payee_name, payee_class,
    disbursement_type, mode, bank_account_id, currency, exchange_rate, gross, ewt, net, purpose, value_date,
    stage, auto_created, proforma_edited, posting_status, posting_error, journal_no, submitted_by, reviewed_by,
    approved_by, approved_at, created_at, created_by)
select r.company_id, b.id, 'DV-2026-' || d.seq, r.id, r.payee_id, r.payee_code, r.payee_name, r.payee_class,
       d.dtype, d.mode, a.id, 'PHP', 1, d.gross, d.ewt, d.gross - d.ewt, d.purpose, current_date - d.days_ago,
       d.stage, d.source_module = 'REMITTANCE', false, d.posting, d.posting_error,
       case when d.posting = 'POSTED' then 'PAY-HO-2026-' || d.seq end, 'disb', 'disbtl2',
       case when d.posting = 'POSTED' then case when d.gross > 50000 then 'disbappr2' else 'disbappr' end else 'disbappr' end,
       (current_date - d.days_ago) + time '16:00' at time zone 'Asia/Manila',
       (current_date - d.days_ago - 1) + time '10:00' at time zone 'Asia/Manila', 'disb'
from seed_dsb d
join dsb_request r on r.request_no = 'DSR-2026-' || d.seq
join org_branch b on b.company_id = r.company_id and b.code = 'HO'
join pay_bank_account a on a.code = 'BDO-CA'
where not exists (select 1 from dsb_voucher x where x.dv_no = 'DV-2026-' || d.seq);

update dsb_request r set voucher_id = v.id
from dsb_voucher v where v.request_id = r.id and r.voucher_id is null and v.dv_no like 'DV-2026-0009%';

insert into dsb_instrument (voucher_id, mode, instrument_no, status, amount, currency, bank_account_id, reference,
    printed_on, printed_at, released_at, released_to, emailed_at, debited_at, stale_at, created_at, created_by)
select v.id, d.mode, d.inst_no, d.inst_status, v.net, 'PHP', v.bank_account_id,
       case when d.mode = 'ATD' then 'Debit authority ' || d.inst_no end,
       case when d.printed_ago is not null then current_date - d.printed_ago end,
       case when d.printed_ago is not null then (current_date - d.printed_ago) + time '11:00' at time zone 'Asia/Manila' end,
       case when d.inst_status in ('RELEASED', 'STALE') then (current_date - d.printed_ago + 2) + time '14:00' at time zone 'Asia/Manila' end,
       case when d.inst_status in ('RELEASED', 'STALE') then 'Insurer messenger' end,
       case when d.mode = 'ATD' then (current_date - d.days_ago) + time '17:00' at time zone 'Asia/Manila' end,
       case when d.inst_status = 'DEBITED' then (current_date - d.days_ago + 1) + time '10:00' at time zone 'Asia/Manila' end,
       case when d.inst_status = 'STALE' then (current_date - d.printed_ago + 181) + time '00:30' at time zone 'Asia/Manila' end,
       (current_date - d.days_ago) + time '16:30' at time zone 'Asia/Manila', 'disbtl'
from seed_dsb d
join dsb_voucher v on v.dv_no = 'DV-2026-' || d.seq
where d.inst_status is not null
  and not exists (select 1 from dsb_instrument x where x.voucher_id = v.id);

-- Certificates 2307 of the commission: received from the insurers on the remittance vouchers.
insert into dsb_voucher_tag (voucher_id, kind, direction, doc_no, doc_date, received_on, period_from, period_to,
    amount, certificate_ref, remarks, created_at, created_by)
select v.id, 'CWT', 'RECEIVED', t.doc_no, current_date - t.days_ago, current_date - t.days_ago,
       date_trunc('quarter', current_date - 92)::date, (date_trunc('quarter', current_date) - interval '1 day')::date,
       t.amount, t.doc_no, 'Withholding on the commission of the quarter',
       (current_date - t.days_ago) + time '15:00' at time zone 'Asia/Manila', 'disb'
from (values
    ('DV-2026-000902', '2307-LAC-2026-0418', 965.43, 40),
    ('DV-2026-000903', '2307-VMI-2026-0112', 1406.25, 12)
) as t(dv_no, doc_no, amount, days_ago)
join dsb_voucher v on v.dv_no = t.dv_no
where not exists (select 1 from dsb_voucher_tag x where x.voucher_id = v.id and x.kind = 'CWT');

-- Upload of payment requests with two rows refused.
insert into bulk_job (company_id, job_no, handler_code, file_name, status, total_rows, valid_rows, invalid_rows,
    committed_rows, failed_rows, completed_at, created_at, created_by)
select c.id, 'BLK-2026-000911', 'DISB_REQUESTS', 'payment-requests-cebu.xlsx', 'COMPLETED', 4, 2, 2, 2, 0,
       now() - interval '9 days', now() - interval '9 days', 'disb'
from org_company c
where c.code = 'FVI' and not exists (select 1 from bulk_job x where x.job_no = 'BLK-2026-000911');

insert into bulk_row (job_id, row_no, data, status, messages, result_ref)
select j.id, r.row_no, r.data, r.status, r.messages, r.result_ref
from (values
    (1, '{"Type":"SUPPLIER","Payee code":"S-0001","Currency":"PHP","Amount":"4560.00","Purpose":"Office supplies"}',
     'COMMITTED', null, 'DSR-2026-000921'),
    (2, '{"Type":"SUPPLIER","Payee code":"S-0099","Currency":"PHP","Amount":"12000.00","Purpose":"Printer toner"}',
     'INVALID', 'Payee code: S-0099 is not a payee of the company', null),
    (3, '{"Type":"EMPLOYEE","Payee code":"EMP-0412","Currency":"PHP","Amount":"","Purpose":"Meal allowance"}',
     'INVALID', 'Amount is mandatory', null),
    (4, '{"Type":"SUPPLIER","Payee code":"S-0002","Currency":"PHP","Amount":"3360.00","Purpose":"Cloud storage"}',
     'COMMITTED', null, 'DSR-2026-000922')
) as r(row_no, data, status, messages, result_ref)
join bulk_job j on j.job_no = 'BLK-2026-000911'
where not exists (select 1 from bulk_row x where x.job_id = j.id);

-- =====================================================================================================
-- 3. Remittance: a batch with incentives, a special remittance (immediate OR issuance) pushed to
--    Disbursement and one waiting for approval, and a paid AR capped at the DTIP balance.
-- =====================================================================================================
insert into rem_special_request (company_id, request_no, invoice_no, arn, insurer_code, client_code, assured_name,
    policy_no, segment, condition_code, remarks, stage, source, requested_by, validation_note, approved_by,
    approved_at, batch_no, pushed_at, created_at, created_by)
select c.id, s.request_no, s.invoice_no, s.arn, 'INS-MGIC', s.client_code, s.assured, s.policy_no, 'CBG',
       s.condition, s.remarks, s.stage, 'SCREEN', 'mktcoll', 'Paid in full; the insurer needs its OR for the claim',
       case when s.approved_ago is not null then 'remittl' end,
       case when s.approved_ago is not null then (current_date - s.approved_ago) + time '15:00' at time zone 'Asia/Manila' end,
       s.batch_no,
       case when s.pushed_ago is not null then (current_date - s.pushed_ago) + time '17:00' at time zone 'Asia/Manila' end,
       (current_date - s.days_ago) + time '09:40' at time zone 'Asia/Manila', 'mktcoll'
from (values
    ('SPR-2026-000901', 'BI-HO-2026-000008', 'ARN-2026-940008', 'CL-2026-000002', 'Reyes, Jose Miguel Lopez',
     'MGIC-MC-2026-98808', 'IMMEDIATE_OR', 'Client needs the insurer''s OR for the car loan release',
     'PUSHED_TO_DISBURSEMENT', 11, 10, 9, 'RMB-INS-MGIC-2026-000903'),
    ('SPR-2026-000902', 'BI-HO-2026-000003', 'ARN-2026-940003', 'CL-2026-000001', 'Santos, Maria Clara Reyes',
     'MGIC-MC-2026-98803', 'CLAIMS', 'Claim settlement waits for the premium remittance',
     'FOR_APPROVAL', 2, null, null, null)
) as s(request_no, invoice_no, arn, client_code, assured, policy_no, condition, remarks, stage, days_ago,
       approved_ago, pushed_ago, batch_no)
join org_company c on c.code = 'FVI'
where not exists (select 1 from rem_special_request x where x.request_no = s.request_no);

create temporary table seed_rem_batch (
    batch_no varchar(40), rtype varchar(30), stage varchar(30), special_request varchar(40), days_ago integer,
    invoice_no varchar(40), arn varchar(30), policy_no varchar(60), client_code varchar(30), assured varchar(250),
    risk_code varchar(20), product_line varchar(30), inception date, expiry date, booking date, paid_on_ago integer,
    basic numeric(19, 2), paid_ar numeric(19, 2), commission numeric(19, 2), commission_vat numeric(19, 2),
    wtax numeric(19, 2), dtip numeric(19, 2), incentive numeric(19, 2), incentive_vat numeric(19, 2),
    or_no varchar(40), dv_no varchar(40)
) on commit drop;

insert into seed_rem_batch values
('RMB-INS-MGIC-2026-000902', 'WITH_INCENTIVES', 'FULLY_REMITTED', null, 6, 'BI-HO-2026-000007', 'ARN-2026-000002',
 'MGIC-FI-2026-91901', 'CL-2026-000005', 'Garcia, Antonio Luis Dizon', 'PAR01', 'FIRE', date '2026-09-01',
 date '2029-09-01', date '2026-10-09', 7, 15270.00, 19087.50, 2668.45, 320.21, 266.85, 19087.50, 381.75, 45.81,
 'MGIC-OR-2026-900911', 'DV-2026-000903'),
('RMB-INS-MGIC-2026-000903', 'SPECIAL', 'APPROVED', 'SPR-2026-000901', 9, 'BI-HO-2026-000008', 'ARN-2026-940008',
 'MGIC-MC-2026-98808', 'CL-2026-000002', 'Reyes, Jose Miguel Lopez', 'MTR10', 'MOTOR', date '2026-09-12',
 date '2027-09-12', date '2026-10-09', 12, 15450.00, 19350.73, 2703.69, 324.44, 270.37, 19351.13, 0, 0, null, null);

insert into rem_batch (company_id, batch_no, insurer_code, remittance_type, currency, special_request_no, processor,
    stage, line_count, paid_ar, commission, commission_vat, wtax, dtip, incentive, incentive_vat, net_due,
    submitted_by, submitted_at, approved_by, approved_at, dv_no, disbursed_amount, commission_or_no,
    commission_or_status, incentive_or_no, incentive_or_status, created_at, created_by, cpc2, cpc2_vat,
    deduction_amount, send_cycle)
select c.id, b.batch_no, 'INS-MGIC', b.rtype, 'PHP', b.special_request, 'remit', b.stage, 1, b.paid_ar, b.commission,
       b.commission_vat, b.wtax, b.dtip, b.incentive, b.incentive_vat,
       b.paid_ar - b.commission - b.commission_vat + b.wtax, 'remit',
       (current_date - b.days_ago) + time '11:00' at time zone 'Asia/Manila', 'remittl',
       (current_date - b.days_ago) + time '15:30' at time zone 'Asia/Manila', b.dv_no,
       case when b.dv_no is not null then b.paid_ar - b.commission - b.commission_vat + b.wtax end,
       null, null, null, null,
       (current_date - b.days_ago) + time '10:00' at time zone 'Asia/Manila', 'remit', 0, 0, 0, 1
from seed_rem_batch b
join org_company c on c.code = 'FVI'
where not exists (select 1 from rem_batch x where x.batch_no = b.batch_no);

insert into rem_batch_line (batch_id, invoice_no, arn, policy_no, client_code, assured_name, risk_code, product_line,
    segment, inception_date, expiry_date, booking_date, last_paid_on, prev_remittance_status, basic_premium, paid_ar,
    commission, commission_vat, wtax, dtip, incentive, incentive_vat, net_due, excluded, insurer_or_no,
    insurer_or_date, insurer_or_amount, or_status, remitted_status, created_at, created_by)
select rb.id, b.invoice_no, b.arn, b.policy_no, b.client_code, b.assured, b.risk_code, b.product_line, 'CBG',
       b.inception, b.expiry, b.booking, current_date - b.paid_on_ago, 'UNPROCESSED', b.basic, b.paid_ar, b.commission,
       b.commission_vat, b.wtax, b.dtip, b.incentive, b.incentive_vat,
       b.paid_ar - b.commission - b.commission_vat + b.wtax, false, b.or_no,
       case when b.or_no is not null then current_date - b.days_ago + 4 end,
       case when b.or_no is not null then b.paid_ar end, case when b.or_no is not null then 'MATCHED' end,
       case when b.stage = 'FULLY_REMITTED' then 'FULLY_REMITTED' else 'UNPROCESSED' end,
       rb.created_at, 'remit'
from seed_rem_batch b
join rem_batch rb on rb.batch_no = b.batch_no
where not exists (select 1 from rem_batch_line x where x.batch_id = rb.id);

insert into rem_extraction_tag (company_id, invoice_no, insurer_code, remittance_type, tag, reasons, remarks, paid_ar,
    dtip_balance, remittable, batch_no, created_at, created_by)
select c.id, 'BI-HO-2026-000003', 'INS-MGIC', 'NORMAL_PHP', 'UNEXTRACTED_DUE', 'PAID_AR_OVER_DTIP',
       'Paid by bills payment above the premium; the excess waits for refund or re-application', 30000.00, 28506.63,
       28506.63, null, (current_date - 10) + time '06:00' at time zone 'Asia/Manila', 'remit'
from org_company c
where c.code = 'FVI'
  and not exists (select 1 from rem_extraction_tag x where x.invoice_no = 'BI-HO-2026-000003' and x.tag = 'UNEXTRACTED_DUE');
