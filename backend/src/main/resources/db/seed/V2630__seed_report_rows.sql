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
 'T-CBG1', 19351.53, 0, 0, 0, 'CASH', null, null, 'ISSUED', null, null),
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
       'PHP', u.amount, 0, 'COMPLETED', 'Minimal balance', 'CASHIERING', 'PAY:' || u.payment_no,
       'Swept to miscellaneous income by the minimal balance sweep', r.created_at, 'cashbr', 'NEW'
from (values
    ('UNP-2026-000901', 'AR-CEB-000002', null, 0.75, 'PAY-2026-000921'),
    ('UNP-2026-000902', 'AR-CEB-000004', 'BI-HO-2026-000008', 0.40, 'PAY-2026-000922')
) as u(reference, receipt_no, invoice_no, amount, payment_no)
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
    ('AR-CEB-000003', 'BI-HO-2026-000004', 'ARN-2026-940004', 'PAY-2026-000911', 28281.25, 4064.45, 487.73,
     21, 'REVERSED', 20, 'Check returned for insufficient funds', '-000911'),
    ('AR-CEB-000004', 'BI-HO-2026-000008', 'ARN-2026-940008', 'PAY-2026-000912', 19351.13, 2703.75, 324.45,
     12, 'ACTIVE', null, null, '-000912'),
    ('AR-CEB-000005', 'BI-HO-2026-000003', 'ARN-2026-940003', 'PAY-2026-000913', 19087.50, 2668.45, 320.21,
     9, 'REVERSED', 7, 'Applied to the wrong invoice; re-applied to the client''s fire policy', '-000913'),
    ('AR-CEB-000005', 'BI-HO-2026-000007', 'ARN-2026-000002', 'RAP-2026-000914', 19087.50, 2668.45, 320.21,
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
    ('BI-HO-2026-000007', 'ADJUSTMENT', 'ADJ-2026-000901',
     'Refund of the cancelled fire policy re-applied to the new fire policy of the client', 10095.49, 0.00, null,
     'UNP-2026-000903', 8),
    ('BI-HO-2026-000007', 'CASHIERING', 'REV-2026-000913', 'Payment reversed from the wrong invoice and re-applied',
     19087.50, 0.00, 'AR-CEB-000005', null, 7),
    ('BI-HO-2026-000004', 'CASHIERING', 'RIN-2026-000901', 'Reinstated receipt re-applied to the invoice',
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
select c.id, b.id, p.payment_no, p.channel, p.batch_ref, 'UPL:' || p.batch_ref || ':' || p.row_no, p.row_no, p.reference,
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
    ('PAY-2026-000905', 'DIRECT_CREDIT', 'BLK-2026-000902', 1, 'ARN-2026-950011', 'Pacific Harbor Logistics Inc.',
     379080.00, 'DIRECT_CREDIT', 'PREBOOKED', 'ARN-2026-950011', 0, 'Held for the renewal not yet booked', 4),
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
 'APPROVED', 'POSTED', null, 196, 'STALE', '100003', 195, 'REMITTANCE', 'RMB-INS-LAC-2026-000901', 'RELEASED'),
('000902', 'INS-LAC', 'REMITTANCE', 'CHECK', 48250.00, 0, 'Net premium remittance batch RMB-INS-LAC-2026-000902',
 'APPROVED', 'POSTED', null, 62, 'RELEASED', '100011', 61, 'REMITTANCE', 'RMB-INS-LAC-2026-000902', 'RELEASED'),
('000903', 'INS-VMI', 'REMITTANCE', 'ATD', 132600.00, 0, 'Net premium remittance batch RMB-INS-VMI-2026-000901',
 'APPROVED', 'POSTED', null, 18, 'DEBITED', 'ATD-2026-000901', null, 'REMITTANCE', 'RMB-INS-VMI-2026-000901',
 'RELEASED'),
('000904', 'INS-VMI', 'REMITTANCE', 'ATD', 28125.00, 0, 'Net premium remittance batch RMB-INS-VMI-2026-000902',
 'APPROVED', 'POSTED', null, 3, 'EMAILED', 'ATD-2026-000902', null, 'REMITTANCE', 'RMB-INS-VMI-2026-000902',
 'RELEASED'),
('000905', 'EMP-0412', 'CASH_ADVANCE', 'CHECK', 15000.00, 0, 'Cash advance for the Cebu client service visits',
 'APPROVED', 'POSTED', null, 25, 'PRINTED', '100014', 24, 'PAYREQUEST', 'RFP-2026-000901', 'RELEASED'),
('000906', 'EMP-0388', 'EMPLOYEE', 'CHECK', 8640.00, 0, 'Reimbursement of transportation and meals, client visits',
 'APPROVED', 'POSTED', null, 11, 'PRINTED', '100015', 10, 'DISBURSEMENT', 'DSR-2026-000906', 'RELEASED'),
('000907', 'S-0002', 'OTHER', 'CHECK', 56000.00, 1120.00, 'Annual licence of the document scanning software',
 'APPROVED', 'POSTED', null, 15, 'RELEASED', '100013', 14, 'DISBURSEMENT', 'DSR-2026-000907', 'RELEASED'),
('000908', 'G-0002', 'OTHER', 'CHECK', 12500.00, 250.00, 'Repair of the Cebu branch service vehicle', 'APPROVED',
 'FAILED', 'The expense account 6105 is closed for posting in the period', 6, 'PENDING', null, null, 'DISBURSEMENT',
 'DSR-2026-000908', 'IN_VOUCHER');

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

-- The end-of-day runs of the business dates on which the vouchers were approved.
insert into dsb_eod_run (company_id, run_no, business_date, status, vouchers, checks, credits, forms, reports, emails,
    message, created_at, created_by)
select v.company_id, 'EOD-2026-' || lpad((900 + row_number() over (order by d.business_date))::text, 6, '0'),
       d.business_date, 'CONFIRMED', d.vouchers, d.checks, d.vouchers - d.checks, d.vouchers, 6, d.vouchers,
       d.vouchers || ' vouchers released', d.business_date + time '19:00' at time zone 'Asia/Manila', 'disbtl'
from (select (approved_at at time zone 'Asia/Manila')::date as business_date, count(*) as vouchers,
             count(*) filter (where mode = 'CHECK') as checks
      from dsb_voucher where dv_no like 'DV-2026-0009%' and eod_run_id is null group by 1) d
cross join lateral (select company_id from dsb_voucher where dv_no like 'DV-2026-0009%' limit 1) v
where not exists (select 1 from dsb_eod_run x where x.company_id = v.company_id and x.business_date = d.business_date);

update dsb_voucher v set eod_run_id = r.id
from dsb_eod_run r
where v.dv_no like 'DV-2026-0009%' and v.eod_run_id is null and r.company_id = v.company_id
  and r.business_date = (v.approved_at at time zone 'Asia/Manila')::date;

update dsb_instrument i set eod_run_id = v.eod_run_id
from dsb_voucher v where v.id = i.voucher_id and v.dv_no like 'DV-2026-0009%' and i.eod_run_id is null;

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
 date '2027-09-12', date '2026-10-09', 12, 15450.00, 19351.13, 2703.75, 324.45, 270.38, 19351.13, 0, 0, null, null);

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

-- =====================================================================================================
-- 4. Commission: certificates 2307 of the commission submitted to Comptrollership and the Motor Mania
--    incentive of the last quarter.
-- =====================================================================================================
insert into cmr_certificate (company_id, submission_no, insurer_code, certificate_form, certificate_no, period_from,
    period_to, tax_withheld, stage, reject_reason, submitted_count, decided_at, decided_by, created_at, created_by)
select c.id, s.submission_no, s.insurer, '2307', s.certificate_no, date_trunc('quarter', current_date - 92)::date,
       (date_trunc('quarter', current_date) - interval '1 day')::date, s.tax, s.stage, s.reject_reason, s.submitted,
       case when s.stage <> 'SUBMITTED' then (current_date - s.decided_ago) + time '14:00' at time zone 'Asia/Manila' end,
       case when s.stage <> 'SUBMITTED' then 'comptrol' end,
       (current_date - s.days_ago) + time '10:30' at time zone 'Asia/Manila', 'commrec'
from (values
    ('BCS-2026-000901', 'INS-MGIC', '2307-MGIC-2026-0731', 237.91, 'ACKNOWLEDGED', null, 1, 6, 5),
    ('BCS-2026-000902', 'INS-LAC', '2307-LAC-2026-0418', 965.43, 'REJECTED',
     'The certificate shows the TIN of the head office; the insurer to issue it to the branch TIN', 1, 4, 3),
    ('BCS-2026-000903', 'INS-VMI', '2307-VMI-2026-0112', 1406.25, 'SUBMITTED', null, 1, 2, null)
) as s(submission_no, insurer, certificate_no, tax, stage, reject_reason, submitted, days_ago, decided_ago)
join org_company c on c.code = 'FVI'
where not exists (select 1 from cmr_certificate x where x.submission_no = s.submission_no);

insert into cmr_certificate_or (certificate_id, or_index, or_no, amount)
select c.id, 1, o.or_no, o.amount
from (values
    ('BCS-2026-000901', 'OR-HO-100002', 2426.72),
    ('BCS-2026-000902', 'OR-CEB-200002', 9847.43)
) as o(submission_no, or_no, amount)
join cmr_certificate c on c.submission_no = o.submission_no
where not exists (select 1 from cmr_certificate_or x where x.certificate_id = c.id);

insert into cmr_incentive_run (company_id, run_no, scheme_id, period_from, period_to, status, eligible_count,
    eligible_production, excluded_count, excluded_amount, tier_applied, incentive_amount, pass_on_amount, posted_at,
    posted_by, journal_refs, created_at, created_by)
select c.id, 'INR-2026-000901', s.id, date_trunc('quarter', current_date - 92)::date,
       (date_trunc('quarter', current_date) - interval '1 day')::date, 'POSTED', 2, 45534.49, 1, 22268.75,
       'Fixed per policy', 1000.00, 1000.00, now() - interval '5 days', 'commtl', 'INC-HO-2026-000901',
       now() - interval '6 days', 'commrec'
from org_company c join cmr_incentive_scheme s on s.company_id = c.id and s.code = 'MOTOR_MANIA'
where c.code = 'FVI' and not exists (select 1 from cmr_incentive_run x where x.run_no = 'INR-2026-000901');

insert into cmr_incentive_run_line (run_id, invoice_no, insurer_code, sales_unit, segment, product_line,
    basic_premium, gross_premium, excluded, exclusion_reason, incentive)
select r.id, l.invoice_no, 'INS-MGIC', 'T-CBG1', 'CBG', l.line, l.basic, l.gross, l.excluded, l.reason, l.incentive
from (values
    ('BI-HO-2026-000001', 'MOTOR', 13595.00, 17027.86, false, null, 500.00),
    ('BI-HO-2026-000003', 'MOTOR', 22805.00, 28506.63, false, null, 500.00),
    ('BI-HO-2026-000002', 'FIRE', 17815.00, 22268.75, true, 'Not a motor policy; cancelled flat', 0.00)
) as l(invoice_no, line, basic, gross, excluded, reason, incentive)
join cmr_incentive_run r on r.run_no = 'INR-2026-000901'
where not exists (select 1 from cmr_incentive_run_line x where x.run_id = r.id and x.invoice_no = l.invoice_no);

-- =====================================================================================================
-- 5. Employee Benefits: two new programme lines placed and booked this year (Group Personal Accident
--    of Luzon Agri-Industrial with Mindanao Pacific, Group Life of Pacific Harbor with Luzon Assurance)
--    and the franchise requests of the renewals sent to the incumbent insurers.
-- =====================================================================================================
create temporary table seed_eb_account (
    arn varchar(30), programme_no varchar(30), line_no integer, benefit_line varchar(20), product varchar(20),
    client_code varchar(30), insurer varchar(30), policy_no varchar(60), members integer, created_ago integer,
    inception_ago integer, booked_ago integer, basic numeric(19, 2), dst numeric(19, 2), ptx numeric(19, 2),
    lgt numeric(19, 2), comm_rate numeric(19, 8), officer varchar(50), team varchar(40), invoice_no varchar(40)
) on commit drop;

insert into seed_eb_account values
('ARN-2026-950001', 'EBP-2026-000002', 2, 'GPA', 'EBGPA01', 'CL-2026-000004', 'INS-MPI', 'MPI-GPA-2026-07731', 410,
 48, 32, 25, 246000.00, 30750.00, 4920.00, 1230.00, 25, 'ebao', 'SM', 'BI-HO-2026-000901'),
('ARN-2026-950002', 'EBP-2026-000005', 2, 'GLI', 'EBGLI01', 'CL-2026-000003', 'INS-LAC', 'LAC-GLI-2026-04418', 120,
 30, 16, 12, 180000.00, 0.00, 3600.00, 0.00, 15, 'ebao', 'VOLUNTARY', 'BI-HO-2026-000902');

insert into acc_account (company_id, arn, client_id, client_code, client_name, product_code, line_code, cover_type_code,
    market_segment, source_channel, insurer_code, period_from, period_to, multi_year, term_years, currency,
    total_sum_insured, rating_basis, net_premium, dst, premium_tax, vat, fst, lgt, total_charges, gross_premium,
    commission_rate, commission, vat_on_commission, minimum_applied, payment_arrangement, contact_name,
    contact_email, contact_mobile, account_officer, status, payment_status, payment_source, payment_confirmed_at,
    placement_slip_ref, placed_at, insurer_ref, policy_issue_date, epolicy_received, business_type, origin,
    created_at, created_by)
select c.id, e.arn, cl.id, cl.client_code, cl.display_name, e.product, e.benefit_line, e.benefit_line, 'CORPORATE',
       'EMAIL', e.insurer, current_date - e.inception_ago, current_date - e.inception_ago + 365, false, 1, 'PHP',
       e.members * 500000.00, 'ANNUAL', e.basic, e.dst, e.ptx, 0, 0, e.lgt, e.dst + e.ptx + e.lgt,
       e.basic + e.dst + e.ptx + e.lgt, e.comm_rate, round(e.basic * e.comm_rate / 100, 2),
       round(e.basic * e.comm_rate / 100 * 0.12, 2), false, 'VIA_BROKER', cl.display_name, cl.email, cl.mobile,
       e.officer, 'BOOKED', 'PAID', 'Payment report', (current_date - e.booked_ago) + time '10:00' at time zone 'Asia/Manila',
       'PL-2026-' || right(e.arn, 6), (current_date - e.inception_ago - 5) + time '15:00' at time zone 'Asia/Manila',
       e.policy_no, current_date - e.inception_ago, true, 'NEW_BUSINESS', 'EMPLOYEE_BENEFITS',
       (current_date - e.created_ago) + time '09:00' at time zone 'Asia/Manila', e.officer
from seed_eb_account e
join org_company c on c.code = 'FVI'
join crm_client cl on cl.client_code = e.client_code
where not exists (select 1 from acc_account x where x.arn = e.arn);

insert into eb_programme_line (programme_id, line_no, benefit_line, product_code, incumbent_insurer, current_policy_no,
    current_arn, period_from, period_to, headcount, active, created_at, created_by)
select p.id, e.line_no, e.benefit_line, e.product, e.insurer, e.policy_no, e.arn, current_date - e.inception_ago,
       current_date - e.inception_ago + 365, e.members, true,
       (current_date - e.created_ago) + time '09:00' at time zone 'Asia/Manila', e.officer
from seed_eb_account e
join eb_programme p on p.programme_no = e.programme_no
where not exists (select 1 from eb_programme_line x where x.programme_id = p.id and x.line_no = e.line_no);

insert into bkg_invoice (company_id, branch_id, invoice_no, arn, account_id, transaction_no, kind, status, policy_year,
    policy_no, client_id, client_code, client_name, insurer_code, risk_code, line_code, market_segment, source_channel,
    account_officer, sales_unit, department, cost_center, currency, booking_date, inception_date, expiry_date,
    basic_premium, dst, premium_tax_vat, lgt, fst, other_charges, commission_rate, commission, vat_on_commission,
    wtax_rate, wtax_amount, direct_payment, cwt2_percent, incentive_eligible, business_type, booking_source,
    booked_by, booked_at, created_at, created_by, root_invoice_no, ledger_context, incentive_status)
select a.company_id, b.id, e.invoice_no, a.arn, a.id, 'NB', 'BOOKING', 'BOOKED', 1, e.policy_no, a.client_id,
       a.client_code, a.client_name, a.insurer_code, a.product_code, a.line_code, 'CORPORATE', 'EMAIL', e.officer,
       e.team, 'EB', 'NB-CORP', 'PHP', current_date - e.booked_ago, a.period_from, a.period_to, e.basic, e.dst, e.ptx,
       e.lgt, 0, 0, e.comm_rate, a.commission, a.vat_on_commission, 10, round(a.commission * 0.10, 2), false, false,
       false, 'NEW_BUSINESS', 'INDIVIDUAL', 'ebproc', (current_date - e.booked_ago) + time '14:00' at time zone 'Asia/Manila',
       (current_date - e.booked_ago) + time '14:00' at time zone 'Asia/Manila', 'ebproc', e.invoice_no, 'NEW',
       'NOT_ELIGIBLE'
from seed_eb_account e
join acc_account a on a.arn = e.arn
join org_branch b on b.company_id = a.company_id and b.code = 'HO'
where not exists (select 1 from bkg_invoice x where x.invoice_no = e.invoice_no);

-- Franchise requests of the renewals: one waiting for the insurer, one approved by the insurer
-- without evidence yet (advised to the client).
insert into eb_franchise_request (company_id, franchise_no, programme_id, cycle_id, insurer_code, status,
    submitted_at, due_date, advice_due_date, remarks, created_at, created_by)
select cy.company_id, f.franchise_no, cy.programme_id, cy.id, f.insurer, 'SUBMITTED',
       (current_date - f.sent_ago) + time '10:00' at time zone 'Asia/Manila', current_date - f.sent_ago + 10,
       current_date - f.sent_ago + 15, f.remarks, (current_date - f.sent_ago) + time '09:30' at time zone 'Asia/Manila',
       'ebao'
from (values
    ('EBF-2026-000901', 'EBC-2026-000004', 'INS-MPI', 6,
     'Renewal of the Group Personal Accident of 120 members on the expiring terms'),
    ('EBF-2026-000902', 'EBC-2026-000001', 'INS-MGIC', 3,
     'Renewal of the Group HMO of 240 members; the client asks to keep the dental rider')
) as f(franchise_no, cycle_no, insurer, sent_ago, remarks)
join eb_cycle cy on cy.cycle_no = f.cycle_no
where not exists (select 1 from eb_franchise_request x where x.franchise_no = f.franchise_no);

-- =====================================================================================================
-- 6. Submitted Policies: corporate policies in review with their IAAF, the renewal proposal letters to
--    the bank counterpart, an expired policy whose renewal waits on a hold cover that starts after the
--    expiry, an intake file with refused rows and a migration upload with refused rows.
-- =====================================================================================================
insert into sbm_intake_run (company_id, run_no, source_code, bulk_job_no, file_name, file_sha256, received, created,
    updated, duplicate, failed, status, started_at, finished_at, created_at, created_by)
select c.id, 'SIR-2026-000901', 'IBG_LEASING_DOC', 'BLK-2026-000913', 'ibg-leasing-policies.xlsx',
       md5('ibg-leasing-policies-901') || md5('ibg-leasing-policies-901'), 14, 12, 0, 0, 2, 'COMPLETED',
       (current_date - 36) + time '08:10' at time zone 'Asia/Manila',
       (current_date - 36) + time '08:12' at time zone 'Asia/Manila',
       (current_date - 36) + time '08:10' at time zone 'Asia/Manila', 'sbmtl'
from org_company c
where c.code = 'FVI' and not exists (select 1 from sbm_intake_run x where x.run_no = 'SIR-2026-000901');

create temporary table seed_sbm (
    sbm_no varchar(30), natural_key varchar(80), assured varchar(250), status varchar(30), received_ago integer,
    inception date, expiry date, insurer varchar(30), policy_no varchar(60), sum_insured numeric(19, 2),
    premium numeric(19, 2), location varchar(250), adequacy varchar(30), email varchar(120)
) on commit drop;

insert into seed_sbm values
('SBM-2026-000901', 'POL:POL-C60114', 'Luzon Agri-Industrial Corp.', 'IN_REVIEW', 35, date '2026-01-15',
 date '2027-01-15', 'INS-LAC', 'POL-C60114', 32000000.00, 480000.00, 'Warehouse 3, Calamba Premiere Park, Laguna',
 null, 'rm.ibgleasing@bdo.com.ph'),
('SBM-2026-000902', 'POL:POL-C60231', 'Mindanao Agri Ventures Inc.', 'IN_REVIEW', 12, date '2026-02-01',
 date '2027-02-01', 'INS-MPI', 'POL-C60231', 21500000.00, 322500.00, 'Packing plant, Tagum City, Davao del Norte',
 'WITH_FINDINGS', 'rm.ibgleasing@bdo.com.ph'),
('SBM-2026-000903', 'POL:POL-C57702', 'Pacific Harbor Logistics Inc.', 'RENEWAL_IN_PROGRESS', 60, null,
 null, 'INS-MPI', 'POL-C57702', 27000000.00, 405000.00, 'Container yard, Port Area, Manila', 'ADEQUATE',
 'rm.ibgleasing@bdo.com.ph');

insert into sbm_policy (company_id, sbm_no, segment, business_type, natural_key, source_code, intake_run_id,
    date_received, assured_name, email, bank_counterpart_email, insurer_code, policy_no, inception_date, expiry_date,
    sum_insured, total_premium, currency, property_location, occupancy, mortgagee, ffy, employee_account, no_touch,
    migrated, has_documents, classification, bucket, insurer_approval_required, fallout, handler_username,
    ao_username, adequacy_status, status, renewal_ref, renewal_arn, remarks, created_at, created_by)
select c.id, s.sbm_no, 'NONCBG_CORPORATE', 'NB', s.natural_key, 'IBG_LEASING_DOC', r.id, current_date - s.received_ago,
       s.assured, null, s.email, s.insurer, s.policy_no,
       coalesce(s.inception, current_date - 385), coalesce(s.expiry, current_date - 20), s.sum_insured, s.premium,
       'PHP', s.location, 'Industrial', 'BDO Leasing and Finance, Inc.', false, false, false, false, true,
       'SUBMITTED', case when s.status = 'IN_REVIEW' then 'FOR_REVIEW' else 'FOR_RENEWAL' end, false, false, 'sbmtl',
       'ao2', s.adequacy, s.status,
       case when s.status = 'RENEWAL_IN_PROGRESS' then 'RNW-SBM-2026-000903' end,
       case when s.status = 'RENEWAL_IN_PROGRESS' then 'ARN-2026-950011' end,
       case when s.adequacy = 'WITH_FINDINGS' then 'Sum insured below the appraised value of the plant' end,
       (current_date - s.received_ago) + time '08:11' at time zone 'Asia/Manila', 'sbmtl'
from seed_sbm s
join org_company c on c.code = 'FVI'
join sbm_intake_run r on r.run_no = 'SIR-2026-000901'
where not exists (select 1 from sbm_policy x where x.sbm_no = s.sbm_no);

insert into sbm_iaaf (company_id, iaaf_no, policy_id, status, current_level, total_levels, submitted_at,
    template_version, created_at, created_by)
select p.company_id, i.iaaf_no, p.id, i.status, i.level, 2,
       case when i.status = 'FOR_APPROVAL' then (current_date - 20) + time '15:00' at time zone 'Asia/Manila' end,
       1, (current_date - i.created_ago) + time '10:00' at time zone 'Asia/Manila', 'sbmtl'
from (values
    ('IAAF-2026-000901', 'SBM-2026-000901', 'FOR_APPROVAL', 1, 22),
    ('IAAF-2026-000902', 'SBM-2026-000902', 'DRAFT', 0, 9)
) as i(iaaf_no, sbm_no, status, level, created_ago)
join sbm_policy p on p.sbm_no = i.sbm_no
where not exists (select 1 from sbm_iaaf x where x.iaaf_no = i.iaaf_no);

insert into sbm_letter (company_id, letter_no, policy_id, rule_id, letter_type, channel, status, recipient,
    template_code, template_version, error, sent_at, created_at, created_by)
select p.company_id, l.letter_no, p.id, r.id, 'RENEWAL_PROPOSAL', 'BANK_COUNTERPART', l.status,
       'rm.ibgleasing@bdo.com.ph', 'SBM_RENEWAL_PROPOSAL', 1, l.error,
       case when l.status = 'SENT' then (current_date - l.days_ago) + time '07:05' at time zone 'Asia/Manila' end,
       (current_date - l.days_ago) + time '07:00' at time zone 'Asia/Manila', 'sbmtl'
from (values
    ('SBL-2026-000901', 'SBM-2026-000901', 'SENT', null, 30),
    ('SBL-2026-000902', 'SBM-2026-000903', 'SENT', null, 50),
    ('SBL-2026-000903', 'SBM-2026-000902', 'FAILED', 'The mail server refused the recipient address', 8)
) as l(letter_no, sbm_no, status, error, days_ago)
join sbm_policy p on p.sbm_no = l.sbm_no
join sbm_letter_rule r on r.company_id = p.company_id and r.letter_type = 'RENEWAL_PROPOSAL'
                      and r.segment = 'NONCBG_CORPORATE'
where not exists (select 1 from sbm_letter x where x.letter_no = l.letter_no);

-- The renewal of the expired policy: account placed with the insurer, hold cover requested from the
-- day the insurer accepted, four days after the expiry.
insert into acc_account (company_id, arn, client_id, client_code, client_name, product_code, line_code, cover_type_code,
    market_segment, source_channel, insurer_code, period_from, period_to, multi_year, term_years, currency,
    total_sum_insured, rating_basis, net_premium, dst, premium_tax, vat, fst, lgt, total_charges, gross_premium,
    commission_rate, commission, vat_on_commission, minimum_applied, payment_arrangement, contact_name, contact_email,
    account_officer, status, payment_status, placement_slip_ref, placed_at, business_type, origin, renewal_of_ref,
    created_at, created_by)
select c.id, 'ARN-2026-950011', cl.id, cl.client_code, cl.display_name, 'PAR01', 'PROPERTY', 'FIRE', 'CORPORATE',
       'EMAIL', 'INS-MPI', current_date - 20, current_date + 345, false, 1, 'PHP', 27000000.00, 'ANNUAL', 324000.00,
       40500.00, 6480.00, 0, 6480.00, 1620.00, 55080.00, 379080.00, 20, 64800.00, 7776.00, false, 'VIA_BROKER',
       cl.display_name, cl.email, 'ao2', 'PLACED', 'UNPAID', 'PL-2026-950011',
       (current_date - 17) + time '16:00' at time zone 'Asia/Manila', 'RENEWAL', 'SUBMITTED_POLICY', 'POL-C57702',
       (current_date - 28) + time '09:00' at time zone 'Asia/Manila', 'sbmtl'
from org_company c join crm_client cl on cl.client_code = 'CL-2026-000003'
where c.code = 'FVI' and not exists (select 1 from acc_account x where x.arn = 'ARN-2026-950011');

insert into sbm_renewal (company_id, policy_id, handoff_status, manual, renewal_ref, insurer_assigned, ra_template,
    arn, hold_cover_on, outcome, handed_off_at, message, created_at, created_by)
select p.company_id, p.id, 'HANDED_OFF', false, 'RNW-SBM-2026-000903', 'INS-MPI', 'GENERIC', 'ARN-2026-950011',
       current_date - 16, 'IN_PROGRESS', (current_date - 30) + time '08:00' at time zone 'Asia/Manila',
       'Handed off to the renewal of the corporate team', (current_date - 30) + time '08:00' at time zone 'Asia/Manila',
       'sbmtl'
from sbm_policy p
where p.sbm_no = 'SBM-2026-000903' and not exists (select 1 from sbm_renewal x where x.policy_id = p.id);

insert into plc_hold_cover (company_id, account_id, arn, insurer_code, status, start_date, expiry_date, created_at,
    created_by, duration_days, expiring_policy_no, remarks, requested_by)
select a.company_id, a.id, a.arn, 'INS-MPI', 'REQUESTED', current_date - 16, current_date + 14,
       (current_date - 16) + time '11:00' at time zone 'Asia/Manila', 'sbmtl', 30, 'POL-C57702',
       'The insurer accepted the renewal four days after the expiry; cover from acceptance', 'sbmtl'
from acc_account a
where a.arn = 'ARN-2026-950011' and not exists (select 1 from plc_hold_cover x where x.arn = a.arn);

-- Migration upload of submitted policies with rows refused.
insert into bulk_job (company_id, job_no, handler_code, file_name, status, total_rows, valid_rows, invalid_rows,
    committed_rows, failed_rows, completed_at, created_at, created_by)
select c.id, 'BLK-2026-000914', 'SBM_MIGRATION', 'legacy-submitted-policies-batch2.xlsx', 'COMPLETED', 5, 3, 2, 3, 0,
       now() - interval '15 days', now() - interval '15 days', 'sbmtl'
from org_company c
where c.code = 'FVI' and not exists (select 1 from bulk_job x where x.job_no = 'BLK-2026-000914');

insert into bulk_row (job_id, row_no, data, status, messages, result_ref)
select j.id, r.row_no, r.data, r.status, r.messages, r.result_ref
from (values
    (1, '{"Policy no":"POL-C51120","Assured":"Visayas Shipping Lines Inc.","Expiry date":"2027-03-14"}',
     'COMMITTED', null, 'POL:POL-C51120'),
    (2, '{"Policy no":"POL-C51188","Assured":"Metro Retail Holdings Corp.","Expiry date":"2027-13-01"}',
     'INVALID', 'Expiry date: 2027-13-01 is not a date', null),
    (3, '{"Policy no":"","Assured":"Cebu Motor Works","Expiry date":"2027-02-20"}',
     'INVALID', 'Policy no is mandatory', null),
    (4, '{"Policy no":"POL-C51207","Assured":"Luzon Steel Manufacturing Corp.","Expiry date":"2027-04-02"}',
     'COMMITTED', null, 'POL:POL-C51207'),
    (5, '{"Policy no":"POL-C51233","Assured":"Mindanao Agri Ventures Inc.","Expiry date":"2027-05-11"}',
     'COMMITTED', null, 'POL:POL-C51233')
) as r(row_no, data, status, messages, result_ref)
join bulk_job j on j.job_no = 'BLK-2026-000914'
where not exists (select 1 from bulk_row x where x.job_id = j.id);

-- =====================================================================================================
-- 7. Sanction Screening: the client of the PEP case is tagged PEP; the SLA monitor reminded the
--    assignees and recorded the breach of the high-risk case waiting for Compliance review.
-- =====================================================================================================
insert into crm_client_tag (client_id, tag_code, active, created_at, created_by)
select c.id, 'PEP', true, k.created_at + interval '2 hours', 'compoff'
from crm_client c
join scr_case k on k.client_id = c.id and k.case_type = 'PEP'
where c.client_code = 'CL-2026-000006'
  and not exists (select 1 from crm_client_tag t where t.client_id = c.id and t.tag_code = 'PEP');

insert into scr_case_event (case_id, event, from_stage, to_value, remarks, actor, occurred_at, created_at, created_by)
select k.id, e.event, k.stage, e.notified, e.remarks, 'system', now() - e.ago, now() - e.ago, 'system'
from (values
    ('SCR-2026-000005', 'REMINDER', 'compoff', 'Compliance review due within a day', interval '30 hours'),
    ('SCR-2026-000005', 'BREACH', 'compoff, compchk', 'Compliance review past its due time', interval '3 hours'),
    ('SCR-2026-000002', 'REMINDER', 'investigator', 'Investigation due within two days', interval '5 hours'),
    ('SCR-2026-000003', 'DOCUMENT_REMINDER', 'investigator, ao',
     'Proof of the source of funds still missing after the return', interval '20 hours')
) as e(case_no, event, notified, remarks, ago)
join scr_case k on k.case_no = e.case_no
where not exists (select 1 from scr_case_event x where x.case_id = k.id and x.event = e.event);

update scr_case set breached = true
where case_no = 'SCR-2026-000005' and not breached
  and exists (select 1 from scr_case_event e where e.case_id = scr_case.id and e.event = 'BREACH');

-- =====================================================================================================
-- 8. Renewal: the paid-off loans report of Loan Monitoring of last month; none of its PNs belongs to an
--    open renewal of the seed (their policies were not mortgage-tagged renewals).
-- =====================================================================================================
insert into bulk_job (company_id, job_no, handler_code, file_name, status, total_rows, valid_rows, invalid_rows,
    committed_rows, failed_rows, completed_at, created_at, created_by)
select c.id, 'BLK-2026-000915', 'RNW_LAMD_REPORT', 'lamd-paid-off-loans.xlsx', 'COMPLETED', 3, 3, 0, 3, 0,
       now() - interval '7 days', now() - interval '7 days', 'lamd'
from org_company c
where c.code = 'FVI' and not exists (select 1 from bulk_job x where x.job_no = 'BLK-2026-000915');

insert into rnw_lamd_report (company_id, report_no, report_type, period, job_no, line_count, matched_count,
    created_at, created_by)
select c.id, 'LMD-2026-000901', 'PAID_OFF', to_char(date_trunc('month', current_date) - interval '1 month', 'YYYY-MM'),
       'BLK-2026-000915', 3, 0, now() - interval '7 days', 'lamd'
from org_company c
where c.code = 'FVI' and not exists (select 1 from rnw_lamd_report x where x.job_no = 'BLK-2026-000915');

insert into rnw_lamd_line (report_id, row_no, pn_no, loan_status, status_date, borrower, match_outcome, message,
    created_at, created_by)
select r.id, l.row_no, l.pn_no, 'PAID_OFF', current_date - l.days_ago, l.borrower, 'UNMATCHED',
       'No open renewal has PN ' || l.pn_no, r.created_at, 'lamd'
from (values
    (1, 'PN-0144-2023-118820', 'Salazar, Quintin Ramos', 38),
    (2, 'PN-0151-2022-097431', 'Tan, Rowena Uy', 33),
    (3, 'PN-0102-2024-003915', 'Mendoza Agri Trading', 31)
) as l(row_no, pn_no, borrower, days_ago)
join rnw_lamd_report r on r.report_no = 'LMD-2026-000901'
where not exists (select 1 from rnw_lamd_line x where x.report_id = r.id and x.row_no = l.row_no);

-- =====================================================================================================
-- 9. Data Migration: the trial run of a delta client extract with refused rows and its client matching,
--    a delta policy load whose policies expire in the go-live renewal window, and the FY2027 true-up
--    of the legacy invoices with its reconciliation.
-- =====================================================================================================
insert into mig_batch (company_id, batch_no, object_code, environment_class, mode, status, staged_count, valid_count,
    warning_count, invalid_count, loaded_count, skipped_count, rejected_count, excluded_count, waived_count,
    error_rate, unmapped_count, review_count, validated_by, validated_at, load_approved_by, load_approved_at,
    loaded_by, started_at, ended_at, signed_off_at, created_at, created_by)
select c.id, b.batch_no, b.object_code, 'NON_PRODUCTION', 'DELTA', b.status, b.staged, b.valid, 0, b.invalid,
       b.loaded, 0, b.rejected, 0, 0, b.error_rate, 0, b.review, 'migops', now() - b.ago - interval '2 hours',
       'miglead', now() - b.ago - interval '1 hour', 'migops', now() - b.ago, now() - b.ago + interval '4 minutes',
       case when b.status = 'SIGNED_OFF' then now() - b.ago + interval '1 day' end, now() - b.ago - interval '3 hours',
       'migops'
from (values
    ('MGB-2026-000901', 'C01', 'LOADED_WITH_REJECTS', 5, 2, 2, 2, 1, 40.00, 1, interval '6 days'),
    ('MGB-2026-000902', 'P01', 'SIGNED_OFF', 3, 3, 0, 3, 0, 0.00, 0, interval '5 days'),
    ('MGB-2026-000903', 'F01', 'RECONCILED', 2, 2, 0, 2, 0, 0.00, 0, interval '3 days')
) as b(batch_no, object_code, status, staged, valid, invalid, loaded, rejected, error_rate, review, ago)
join org_company c on c.code = 'FVI'
where not exists (select 1 from mig_batch x where x.batch_no = b.batch_no);

insert into mig_extract (company_id, extract_no, object_code, layout_code, layout_version, source_system, as_of,
    sequence_no, mode, file_name, sha256, extracted_at, extracted_by, declared_rows, parsed_rows, staged_rows, masked,
    status, received_by, received_at, checked_at, created_at, created_by)
select c.id, e.extract_no, e.object_code, e.object_code, 1, 'EBIX', timestamp '2027-12-31 18:00:00', 2, 'DELTA',
       e.object_code || '_EBIX_20271231_02.csv', md5(e.extract_no) || md5(e.object_code || e.extract_no),
       now() - e.ago - interval '5 hours', 'ebix.extract', e.rows, e.rows, e.rows, true, 'STAGED', 'migops',
       now() - e.ago - interval '4 hours', now() - e.ago - interval '4 hours', now() - e.ago - interval '4 hours',
       'migops'
from (values
    ('MGX-2026-000901', 'C01', 5, interval '6 days'),
    ('MGX-2026-000902', 'P01', 3, interval '5 days'),
    ('MGX-2026-000903', 'F01', 2, interval '3 days')
) as e(extract_no, object_code, rows, ago)
join org_company c on c.code = 'FVI'
where not exists (select 1 from mig_extract x where x.extract_no = e.extract_no);

insert into mig_stage_row (extract_id, object_code, layout_code, row_no, legacy_key, raw_payload, row_hash, status,
    batch_id, target_entity, target_code, message, loaded_at)
select x.id, x.object_code, x.layout_code, r.row_no, r.legacy_key, r.payload::jsonb, md5(x.extract_no || r.legacy_key),
       r.status, b.id, case when r.status = 'LOADED' then r.entity end, case when r.status = 'LOADED' then r.target end,
       r.message, case when r.status = 'LOADED' then b.ended_at end
from (values
    ('MGX-2026-000901', 'MGB-2026-000901', 1, 'E970211', '{"client_type": "C", "name": "Pacific Harbour Logistics Inc", "tin": "008-421-337-000"}',
     'LOADED', 'Client', 'CL-2026-000003', null),
    ('MGX-2026-000901', 'MGB-2026-000901', 2, 'E970212', '{"client_type": "I", "last_name": "Villanueva", "first_name": "Carmela", "birth_date": "1984-02-11"}',
     'LOADED', 'Client', 'CL-2026-000006', null),
    ('MGX-2026-000901', 'MGB-2026-000901', 3, 'E970213', '{"client_type": "C", "name": "Sierra Agro Holdings", "tin": ""}',
     'INVALID', null, null, null),
    ('MGX-2026-000901', 'MGB-2026-000901', 4, 'E970214', '{"client_type": "X", "name": "Dela Rosa Trading"}',
     'INVALID', null, null, null),
    ('MGX-2026-000901', 'MGB-2026-000901', 5, 'E970215', '{"client_type": "I", "last_name": "Reyes", "first_name": "Jose Miguel"}',
     'REJECTED', null, null, 'Duplicate of client E970045 already loaded; kept apart for the data owner'),
    ('MGX-2026-000902', 'MGB-2026-000902', 1, 'EP970311', '{"policy_no": "FI-970311", "expiry": "2028-01-20"}',
     'LOADED', 'Account', 'ARN-2026-950021', null),
    ('MGX-2026-000902', 'MGB-2026-000902', 2, 'EP970312', '{"policy_no": "FI-970312", "expiry": "2028-02-15"}',
     'LOADED', 'Account', 'ARN-2026-950022', null),
    ('MGX-2026-000902', 'MGB-2026-000902', 3, 'EP970313', '{"policy_no": "MC-970313", "expiry": "2028-04-10"}',
     'LOADED', 'Account', 'ARN-2026-950023', null),
    ('MGX-2026-000903', 'MGB-2026-000903', 1, 'I97000311', '{"invoice_no": "I97000311", "true_up": "FY2027"}',
     'LOADED', 'Invoice', 'I97000311', null),
    ('MGX-2026-000903', 'MGB-2026-000903', 2, 'I97000312', '{"invoice_no": "I97000312", "true_up": "FY2027"}',
     'LOADED', 'Invoice', 'I97000312', null)
) as r(extract_no, batch_no, row_no, legacy_key, payload, status, entity, target, message)
join mig_extract x on x.extract_no = r.extract_no
join mig_batch b on b.batch_no = r.batch_no
where not exists (select 1 from mig_stage_row s where s.extract_id = x.id and s.row_no = r.row_no);

insert into mig_issue (stage_row_id, batch_id, rule_code, severity, field, value, message, resolution, created_at,
    created_by)
select s.id, s.batch_id, i.rule_code, i.severity, i.field, i.value, i.message, 'OPEN', now() - interval '6 days',
       'migops'
from (values
    ('E970213', 'DQ-001', 'ERROR', 'tin', '', 'TIN is mandatory for a corporate client'),
    ('E970214', 'DQ-003', 'ERROR', 'client_type', 'X', 'Code X of LOV:CLIENT_TYPE is not mapped'),
    ('E970211', 'DQ-010', 'WARNING', 'name', 'Pacific Harbour Logistics Inc',
     'Name differs from the matched BIBS client Pacific Harbor Logistics Inc.')
) as i(legacy_key, rule_code, severity, field, value, message)
join mig_stage_row s on s.legacy_key = i.legacy_key and s.layout_code = 'C01'
join mig_batch b on b.id = s.batch_id and b.batch_no = 'MGB-2026-000901'
where not exists (select 1 from mig_issue x where x.stage_row_id = s.id and x.rule_code = i.rule_code);

insert into mig_client_match (batch_id, cluster_no, left_row_id, left_key, right_row_id, right_key, right_client_code,
    score, matched_keys, decision, survivor_key, decided_by, decided_at, created_at, created_by)
select b.id, m.cluster_no, s.id, s.legacy_key, null, null, m.client_code, m.score, m.keys, m.decision, m.survivor,
       m.decided_by, case when m.decided_by is not null then now() - interval '5 days' end, now() - interval '6 days',
       'migops'
from (values
    (1, 'E970211', 'CL-2026-000003', 94, 'TIN, name (similar), address', 'AUTO_MERGE', 'CL-2026-000003', null),
    (2, 'E970212', 'CL-2026-000006', 78, 'Name, birth date', 'MERGE', 'CL-2026-000006', 'migsteward'),
    (3, 'E970215', 'CL-2026-000002', 66, 'Name', 'KEEP_SEPARATE', null, 'migsteward')
) as m(cluster_no, legacy_key, client_code, score, keys, decision, survivor, decided_by)
join mig_batch b on b.batch_no = 'MGB-2026-000901'
join mig_stage_row s on s.batch_id = b.id and s.legacy_key = m.legacy_key
where not exists (select 1 from mig_client_match x where x.batch_id = b.id and x.cluster_no = m.cluster_no);

-- Policies loaded by the delta: in force at go-live and expiring in the go-live renewal window.
create temporary table seed_mig_policy (
    arn varchar(30), legacy_ref varchar(30), policy_no varchar(60), cover_no varchar(30), client_code varchar(30),
    product varchar(20), line varchar(30), insurer varchar(30), expiry date, net numeric(19, 2), gross numeric(19, 2),
    legacy_client varchar(30)
) on commit drop;

insert into seed_mig_policy values
('ARN-2026-950021', 'EP970311', 'FI-970311', 'EC970311', 'CL-2026-000004', 'PAR01', 'PROPERTY', 'INS-LAC',
 date '2028-01-20', 96000.00, 120960.00, 'E970044'),
('ARN-2026-950022', 'EP970312', 'FI-970312', 'EC970312', 'CL-2026-000006', 'PAR08', 'PROPERTY', 'INS-MGIC',
 date '2028-02-15', 10600.00, 13361.25, 'E970212'),
('ARN-2026-950023', 'EP970313', 'MC-970313', 'EC970313', 'CL-2026-000005', 'MTR10', 'MOTOR', 'INS-MGIC',
 date '2028-04-10', 15450.00, 19351.13, 'E970061');

insert into acc_account (company_id, arn, client_id, client_code, client_name, product_code, line_code, market_segment,
    source_channel, insurer_code, period_from, period_to, multi_year, term_years, currency, rating_basis, net_premium,
    gross_premium, minimum_applied, payment_arrangement, account_officer, status, payment_status, insurer_ref,
    business_type, origin, created_at, created_by)
select c.id, p.arn, cl.id, cl.client_code, cl.display_name, p.product, p.line, 'CBG', 'MIGRATION', p.insurer,
       (p.expiry - interval '1 year')::date, p.expiry, false, 1, 'PHP', 'ANNUAL', p.net, p.gross, false, 'VIA_BROKER',
       'ao', 'BOOKED', 'PAID', p.policy_no, 'RENEWAL', 'MIGRATED', now() - interval '5 days', 'mig-loader'
from seed_mig_policy p
join org_company c on c.code = 'FVI'
join crm_client cl on cl.client_code = p.client_code
where not exists (select 1 from acc_account x where x.arn = p.arn);

insert into acc_account_legacy (account_id, company_id, source_system, legacy_ref, policy_no, cover_no, cover_version,
    legacy_client_no, legacy_package_code, legacy_package_version, legacy_status, assured_name, migration_batch,
    created_at, created_by)
select a.id, a.company_id, 'EBIX', p.legacy_ref, p.policy_no, p.cover_no, 1, p.legacy_client, null, null, 'IN_FORCE',
       a.client_name, 'MGB-2026-000902', now() - interval '5 days', 'mig-loader'
from seed_mig_policy p
join acc_account a on a.arn = p.arn
where not exists (select 1 from acc_account_legacy x where x.account_id = a.id);

insert into mig_ra_sent (company_id, legacy_policy_ref, account_arn, cover_no, expiry_date, ra_sent_date, ra_ref,
    ra_channel, sent_to, proposed_insurer, currency, proposed_premium, sent_by, tracker_name, remarks, batch_id,
    rolled_back, created_at, created_by)
select b.company_id, 'EP970311', 'ARN-2026-950021', 'EC970311', date '2028-01-20', date '2027-11-05', 'RA-EBIX-2027-11873',
       'EMAIL', 'finance@luzonagri.ph', 'INS-LAC', 'PHP', 121500.00, 'ao', 'RA tracker - corporate November 2027',
       'Renewal advice sent from the legacy system before the freeze', b.id, false, now() - interval '5 days', 'migops'
from mig_batch b
where b.batch_no = 'MGB-2026-000902'
  and not exists (select 1 from mig_ra_sent x where x.legacy_policy_ref = 'EP970311');

-- FY2027 true-up of the legacy invoices: prepared, approved, posted and reconciled.
insert into mig_recon_run (company_id, run_no, batch_id, object_code, as_of, status, break_count, run_by, run_at,
    created_at, created_by)
select b.company_id, 'MGR-2026-000901', b.id, 'F01', date '2027-12-31', 'EXPLAINED', 1, 'migrecon',
       now() - interval '2 days', now() - interval '2 days', 'migrecon'
from mig_batch b
where b.batch_no = 'MGB-2026-000903' and not exists (select 1 from mig_recon_run x where x.run_no = 'MGR-2026-000901');

insert into mig_trueup (company_id, trueup_no, reference, as_of, status, batch_id, journals_posted, items_adjusted,
    prepared_by, prepared_at, approved_by, approved_at, posted_at, recon_run_id, remarks, created_at, created_by)
select b.company_id, '1', 'TU1-FY2027', date '2027-12-31', 'RECONCILED', b.id, 2, 2, 'migops',
       now() - interval '4 days', 'miglead', now() - interval '3 days', now() - interval '3 days', r.id,
       'Premium changes booked in the legacy system after the opening extract', now() - interval '4 days', 'migops'
from mig_batch b join mig_recon_run r on r.run_no = 'MGR-2026-000901'
where b.batch_no = 'MGB-2026-000903' and not exists (select 1 from mig_trueup x where x.reference = 'TU1-FY2027');

update mig_recon_run r set trueup_id = t.id
from mig_trueup t where t.recon_run_id = r.id and r.run_no = 'MGR-2026-000901' and r.trueup_id is null;

insert into mig_recon_line (run_id, level, measure, currency, source_value, staged_value, target_value, difference,
    tolerance, status, break_reason, explanation, explained_by, explained_at, approved_by, approved_at, created_at,
    created_by)
select r.id, l.level, l.measure, l.currency, l.source, l.staged, l.target, l.target - l.source, 0, l.status, l.reason,
       l.explanation, case when l.status = 'EXPLAINED' then 'migrecon' end,
       case when l.status = 'EXPLAINED' then now() - interval '2 days' end,
       case when l.status = 'EXPLAINED' then 'miglead' end,
       case when l.status = 'EXPLAINED' then now() - interval '1 day' end, now() - interval '2 days', 'migrecon'
from (values
    ('L1', 'Rows received F01_EBIX_20271231_02.csv', null, 2.00, 2.00, 2.00, 'MATCHED', null, null),
    ('TU', 'True-up of the basic premium of legacy invoices', 'PHP', 4250.00, 4250.00, 4250.00, 'MATCHED', null, null),
    ('TU', 'True-up of the commission of legacy invoices', 'PHP', 595.00, 595.00, 595.00, 'MATCHED', null, null),
    ('TU', 'Legacy control account LGC-DTIP after the true-up', 'PHP', 15450.00, 15450.00, 15449.40, 'EXPLAINED',
     'Rounding', 'Rounding of the documentary stamp tax on two endorsements')
) as l(level, measure, currency, source, staged, target, status, reason, explanation)
join mig_recon_run r on r.run_no = 'MGR-2026-000901'
where not exists (select 1 from mig_recon_line x where x.run_id = r.id);

-- =====================================================================================================
-- 10. Organisation: the employees of the cost centres (head office and branches), one separated.
-- =====================================================================================================
insert into org_employee (company_id, employee_no, full_name, branch_id, cost_center, position, email, party_code,
    hired_on, separated_on, active, created_at, created_by)
select c.id, e.no, e.name, b.id, e.cc, e.position, e.email, e.party, e.hired, e.separated, e.separated is null,
       now() - interval '30 days', 'hrappr'
from (values
    ('EMP-0101', 'Concepcion, Teresa Marie', 'HO', 'EXEC', 'President and Chief Executive Officer',
     'tconcepcion@bdoi.com.ph', null, date '2015-03-02', null::date),
    ('EMP-0214', 'Navarro, Antonio Jose', 'HO', 'FIN', 'Comptroller', 'anavarro@bdoi.com.ph', null,
     date '2016-07-18', null),
    ('EMP-0233', 'Lim, Grace Anne', 'HO', 'FIN', 'General Ledger Officer', 'glim@bdoi.com.ph', null,
     date '2019-01-07', null),
    ('EMP-0245', 'Mercado, Paolo Luis', 'HO', 'FIN', 'Cashier', 'pmercado@bdoi.com.ph', null, date '2020-06-15', null),
    ('EMP-0301', 'Fernandez, Clarissa Joy', 'HO', 'CLM', 'Claims Officer', 'cfernandez@bdoi.com.ph', null,
     date '2018-09-03', null),
    ('EMP-0302', 'Ocampo, Cedric James', 'HO', 'CLM', 'Claims Officer', 'cocampo@bdoi.com.ph', null,
     date '2021-02-01', null),
    ('EMP-0388', 'Bautista, Ramon Luis', 'HO', 'MKT', 'Account Officer', 'rbautista@bdoi.com.ph', 'EMP-0388',
     date '2017-11-13', null),
    ('EMP-0390', 'Aquino, Aileen Grace', 'HO', 'NB-CBG-M', 'Account Officer', 'aaquino@bdoi.com.ph', null,
     date '2019-04-22', null),
    ('EMP-0392', 'Robles, Marites Ann', 'HO', 'NB-CBG-M', 'Marketing Team Leader', 'mrobles@bdoi.com.ph', null,
     date '2014-08-11', null),
    ('EMP-0395', 'Castro, Arnel Vincent', 'HO', 'NB-CORP', 'Account Officer', 'acastro@bdoi.com.ph', null,
     date '2020-10-05', null),
    ('EMP-0412', 'Dela Paz, Kristine Mae', 'CEB', 'NB-CBG-V', 'Account Officer', 'kdelapaz@bdoi.com.ph', 'EMP-0412',
     date '2022-03-14', null),
    ('EMP-0415', 'Sison, Benjamin Rey', 'CEB', 'NB-CBG-V', 'Branch Cashier', 'bsison@bdoi.com.ph', null,
     date '2021-07-01', null),
    ('EMP-0421', 'Torres, Consuelo Faith', 'CEB', 'CLM', 'Claims Officer', 'ctorres@bdoi.com.ph', null,
     date '2023-01-16', null),
    ('EMP-0502', 'Villaroman, Dennis Paul', 'DVO', 'NB-CBG-V', 'Account Officer', 'dvillaroman@bdoi.com.ph', null,
     date '2022-09-05', null),
    ('EMP-0610', 'Ilagan, Ingrid Sol', 'HO', 'IT', 'Information Security Officer', 'iilagan@bdoi.com.ph', null,
     date '2018-05-21', null),
    ('EMP-0702', 'Domingo, Helena Rose', 'HO', 'HR', 'Human Resources Officer', 'hdomingo@bdoi.com.ph', null,
     date '2016-02-08', null),
    ('EMP-0398', 'Pascual, Rodel Ian', 'HO', 'NB-CBG-M', 'Account Officer', 'rpascual@bdoi.com.ph', null,
     date '2019-08-19', date '2026-06-30')
) as e(no, name, branch, cc, position, email, party, hired, separated)
join org_company c on c.code = 'FVI'
join org_branch b on b.company_id = c.id and b.code = e.branch
where not exists (select 1 from org_employee x where x.company_id = c.id and x.employee_no = e.no);

-- =====================================================================================================
-- 11. Payment requests: the cash advance of the Cebu account officer (disbursed, check printed) and the
--     refund of the part of a cancelled receipt that was not reinstated (waiting for approval).
-- =====================================================================================================
insert into prq_request (company_id, branch_id, request_no, kind, stage, segment, reference_text, request_date,
    requesting_unit, payee_type, payee_code, payee_name, payment_mode, rfp_type, purpose, currency, amount,
    validation_required, validation_round, submitted_by, submitted_at, reviewed_by, reviewed_at, approved_by,
    approved_at, hr_approved_by, hr_approved_at, send_count, disbursement_request_no, disbursement_status, dv_no,
    dv_status, instrument_status, disbursed_at, created_at, created_by)
select c.id, b.id, r.request_no, r.kind, r.stage, 'CBG', r.reference, current_date - r.days_ago, r.unit, r.payee_type,
       r.payee_code, r.payee_name, 'CHECK', r.rfp_type, r.purpose, 'PHP', r.amount, r.kind = 'REFUND',
       case when r.kind = 'REFUND' then 1 else 0 end, r.maker,
       (current_date - r.days_ago) + time '10:00' at time zone 'Asia/Manila', r.reviewer,
       case when r.reviewer is not null then (current_date - r.days_ago) + time '14:00' at time zone 'Asia/Manila' end,
       r.approver,
       case when r.approver is not null then (current_date - r.days_ago + 1) + time '09:30' at time zone 'Asia/Manila' end,
       r.hr, case when r.hr is not null then (current_date - r.days_ago + 1) + time '11:00' at time zone 'Asia/Manila' end,
       case when r.dsr is not null then 1 else 0 end, r.dsr, case when r.dsr is not null then 'RELEASED' end, r.dv,
       case when r.dv is not null then 'APPROVED' end, case when r.dv is not null then 'PRINTED' end,
       case when r.dv is not null then (current_date - r.days_ago + 2) + time '16:00' at time zone 'Asia/Manila' end,
       (current_date - r.days_ago) + time '09:00' at time zone 'Asia/Manila', r.maker
from (values
    ('RFP-2026-000901', 'CASH_ADVANCE', 'DISBURSED', 'Client service visits, Cebu and Bohol', 'Marketing - Cebu',
     'EMPLOYEE', 'EMP-0412', 'Dela Paz, Kristine Mae', 'CASH_ADVANCE',
     'Cash advance for the client service visits of the quarter', 15000.00, 27, 'mktao', 'mkttl', 'mktappr',
     'hrappr', 'DSR-2026-000905', 'DV-2026-000905'),
    ('RRF-2026-000901', 'REFUND', 'FOR_APPROVAL', 'AR-CEB-000006', 'Cashiering - Cebu', 'CLIENT', 'CL-2026-000001',
     'Santos, Maria Clara Reyes', null, 'Refund of the part of the cancelled receipt not reinstated', 16506.63, 2,
     'cashbr', 'cashtl', null, null, null, null)
) as r(request_no, kind, stage, reference, unit, payee_type, payee_code, payee_name, rfp_type, purpose, amount,
       days_ago, maker, reviewer, approver, hr, dsr, dv)
join org_company c on c.code = 'FVI'
join org_branch b on b.company_id = c.id and b.code = 'CEB'
where not exists (select 1 from prq_request x where x.request_no = r.request_no);

insert into prq_request_line (request_id, line_no, ar_no, client_code, assured_name, invoice_no, root_invoice_no,
    amount, reason_code, branch_unit, cancelled_policy, live)
select q.id, 1, 'AR-CEB-000006', 'CL-2026-000001', 'Santos, Maria Clara Reyes', 'BI-HO-2026-000003',
       'BI-HO-2026-000003', 16506.63, 'DOUBLE_PAYMENT', 'Cebu Branch', false, true
from prq_request q
where q.request_no = 'RRF-2026-000901' and not exists (select 1 from prq_request_line x where x.request_id = q.id);

-- =====================================================================================================
-- 12. Receivables: post-dated cheques of corporate customers received at Cebu and due for deposit.
-- =====================================================================================================
insert into rcv_pdc (company_id, branch_id, pdc_no, received_date, party_id, party_code, payer_name, department,
    cheque_no, cheque_date, drawee_bank, currency, exchange_rate, amount, base_amount, bank_account_code, narration,
    status, status_date, created_at, created_by)
select c.id, b.id, d.pdc_no, current_date - d.received_ago, p.id, p.code, p.name, 'FND', d.cheque_no,
       current_date - d.due_ago, d.bank, 'PHP', 1, d.amount, d.amount, '1111', d.narration, 'ON_HAND',
       current_date - d.received_ago, (current_date - d.received_ago) + time '15:00' at time zone 'Asia/Manila',
       'accountant'
from (values
    ('PDC-CEB-2026-000901', 'C-000202', '220417', 'Metrobank', 41850.00, 31, 1,
     'Post-dated cheque for the premium of the hull renewal'),
    ('PDC-CEB-2026-000902', 'C-000203', '508812', 'Land Bank', 18225.00, 29, 0,
     'Post-dated cheque, second instalment of the crop insurance premium')
) as d(pdc_no, party_code, cheque_no, bank, amount, received_ago, due_ago, narration)
join org_company c on c.code = 'FVI'
join org_branch b on b.company_id = c.id and b.code = 'CEB'
join pty_party p on p.company_id = c.id and p.code = d.party_code
where not exists (select 1 from rcv_pdc x where x.company_id = c.id and x.pdc_no = d.pdc_no);

-- =====================================================================================================
-- 13. Collections: the motor policy of Maria Clara Santos and its endorsement were on the worklist until
--     the client paid at the head office cashier.
-- =====================================================================================================
insert into clx_item (company_id, invoice_no, arn, root_invoice_no, invoice_kind, policy_no, policy_year, client_code,
    assured_name, insurer_code, branch_id, segment, sales_unit, unit_head_username, ao_username, product_line,
    currency, booking_date, inception_date, expiry_date, dp_flag, cwt_flag, invoice_category, gross_premium,
    net_outstanding, outstanding_pr2307, aging_days, aging_bracket, payment_status, status, listed_on, completed_on,
    last_refreshed_at, current_handler, last_effort_at, last_effort_code, remarks, created_at, created_by, origin)
select c.id, i.invoice_no, 'ARN-2026-940001', 'BI-HO-2026-000001', i.kind, 'MGIC-MC-2026-98801', 1, 'CL-2026-000001',
       'Santos, Maria Clara Reyes', 'INS-MGIC', b.id, 'CBG', 'T-CBG1', 'mkttl', 'ao', 'MOTOR', 'PHP', i.booked,
       date '2026-09-01', date '2027-09-01', false, false, 'REGULAR', i.gross, 0, 0, current_date - 7 - i.booked,
       '0-30', 'PAID', 'COMPLETED', current_date - 14, current_date - 7, now() - interval '7 days', 'clxhandler',
       now() - interval '9 days', 'CALL', 'Client promised to pay at the head office cashier',
       now() - interval '14 days', 'system', 'BIBS'
from (values
    ('BI-HO-2026-000001', 'BOOKING', date '2026-09-15', 17027.86),
    ('BI-HO-2026-000005', 'ENDORSEMENT_PLUS', date '2026-09-20', 2690.00)
) as i(invoice_no, kind, booked, gross)
join org_company c on c.code = 'FVI'
join org_branch b on b.company_id = c.id and b.code = 'HO'
where not exists (select 1 from clx_item x where x.company_id = c.id and x.invoice_no = i.invoice_no);
