-- =====================================================================================
-- iNXT BrokerVerse - SEED DATA for platform features (loaded only with the 'seed' profile):
-- suspense account monitoring, recurring journal templates and a supplier awaiting
-- authorization (visible in the checker's "My Approvals" inbox).
-- =====================================================================================

update sys_parameter set param_value = '1606', updated_at = now(), updated_by = 'SYSTEM'
where param_key = 'SUSPENSE_ACCOUNT_CODES';

insert into jnl_recurring_template (company_id, branch_id, name, journal_type, currency, narration,
    reference, frequency, day_of_month, start_date, end_date, auto_reverse, auto_submit, active,
    created_at, created_by)
select c.id, b.id, t.name, t.jtype, 'PHP', t.narration, t.ref, 'MONTHLY', t.dom, date '2026-10-01',
       null, t.rev, false, true, now(), 'accountant'
from org_company c
join org_branch b on b.company_id = c.id and b.code = 'HO',
(values ('Monthly office rent - head office', 'MANUAL', 'Monthly office rent - head office',
         'LEASE-HO-2026', 5, false),
        ('Month-end utilities accrual', 'ACCRUAL', 'Accrual of electricity and water for the month',
         null, 31, true))
     as t(name, jtype, narration, ref, dom, rev)
where c.code = 'FVI';

insert into jnl_recurring_line (template_id, line_no, account_code, side, amount, cost_center,
    narration)
select t.id, l.no, l.acc, l.side, l.amt, l.cc, l.nar
from jnl_recurring_template t,
(values ('Monthly office rent - head office', 0, '5603', 'DEBIT', 150000.00, 'FIN', 'Office rent'),
        ('Monthly office rent - head office', 1, '1111', 'CREDIT', 150000.00, null, 'Rent paid'),
        ('Month-end utilities accrual', 0, '5603', 'DEBIT', 45000.00, 'FIN', 'Utilities accrued'),
        ('Month-end utilities accrual', 1, '2502', 'CREDIT', 45000.00, null, 'Accrued expenses'))
     as l(tname, no, acc, side, amt, cc, nar)
where t.name = l.tname;

insert into pty_party (company_id, code, name, party_type, tax_id, address, email, phone,
    default_currency, credit_days, record_status, created_at, created_by)
select c.id, 'S-000901', 'Makati Office Supplies Trading', 'SUPPLIER', '009-876-543-000',
       'Legaspi Village, Makati City', 'sales@makati-supplies.example.ph', '+63 2 8700 0901',
       'PHP', 30, 'PENDING_AUTHORIZATION', now(), 'accountant'
from org_company c where c.code = 'FVI';
