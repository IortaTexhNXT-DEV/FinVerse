-- =====================================================================================
-- iNXT BrokerVerse - V1901 Demo Collections plans and escalation (wave C1-B, demo data).
--   Requirements: docs/requirements/BDOI_CLXN_BRD_SPEC.md BRCLXN.049/053/055/058
--   Design: docs/architecture/COLLECTIONS_DESIGN.md sections 3 (demo V1901) and 4.3.
--   * The escalation rules the process describes (p.16, p.40; defaults to confirm, CQ14):
--     a broken promise goes to the team lead, 45 days from booking to the team lead, the 60th
--     day from inception to the unit head, an installment overdue 15 days to the team lead. They
--     are authorized, so the job CLX_ESCALATION and the promise check use them.
--   * One rule waiting for authorization (maker badmin), for the approval inbox.
--   The storyline that needs booked invoices (the three-year account with its SOA per billing
--   cycle, a quarterly plan, a kept and a broken promise, automatic and manual escalations) runs
--   after the Operations demo as the Java runner collections.demo.CollectionsPlansDemoData.
-- =====================================================================================

insert into clx_escalation_rule (company_id, code, name, basis, threshold, segment, sales_unit,
    product_line, amount_from, amount_to, target_level, target_username, reason_code, sla_hours,
    notify, effective_from, effective_to, record_status, authorized_by, authorized_at, created_at,
    created_by)
select c.id, r.code, r.name, r.basis, r.threshold, null, null, null, r.amount_from, null,
       r.target_level, r.target_username, r.reason_code, r.sla_hours, true, date '2026-01-01', null,
       r.record_status, case when r.record_status = 'ACTIVE' then 'SYSTEM' end,
       case when r.record_status = 'ACTIVE' then now() end, now(), r.maker
from org_company c
cross join (values
    ('CLX-BROKEN-PROMISE', 'Broken promise to pay - team lead', 'BROKEN_PROMISES_COUNT', 1.00,
     null::numeric, 'TL', 'mkttl', 'BROKEN_PROMISE', 24, 'ACTIVE', 'SYSTEM'),
    ('CLX-AGING-45', 'Unpaid 45 days after booking - team lead', 'AGING_FROM_BOOKING', 45.00,
     null::numeric, 'TL', null, 'AGING', 48, 'ACTIVE', 'SYSTEM'),
    ('CLX-INCEPTION-60', 'Unpaid on the 60th day from inception - unit head', 'AGING_FROM_INCEPTION', 60.00,
     null::numeric, 'UH', null, 'AGING', 72, 'ACTIVE', 'SYSTEM'),
    ('CLX-INSTALLMENT-15', 'Installment overdue 15 days - team lead', 'INSTALLMENT_OVERDUE_DAYS', 15.00,
     null::numeric, 'TL', null, 'INSTALLMENT_OVERDUE', 48, 'ACTIVE', 'SYSTEM'),
    ('CLX-AMOUNT-1M', 'Outstanding of one million or more - section head', 'AMOUNT_OVER', 1000000.00,
     null::numeric, 'SECTION_HEAD', null, 'OTHERS', 72, 'PENDING_AUTHORIZATION', 'badmin')
) as r(code, name, basis, threshold, amount_from, target_level, target_username, reason_code,
       sla_hours, record_status, maker)
where c.code = 'FVI';
