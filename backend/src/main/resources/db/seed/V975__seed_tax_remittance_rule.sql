-- =====================================================================================
-- SEED DATA (seed profile only): accounting rule of the TAX_REMITTANCE event for the seed
-- company FVI. The remittance clears the tax payable account of the return's form (role
-- TAX_PAYABLE), applies credits such as input VAT (role TAX_CREDIT) and pays the balance from
-- the bank account chosen on payment (role BANK). Tax masters, IC mappings, returns and 2307
-- certificates are created by the TaxSeedData runner through the services (maker-checker).
-- Every lookup is scoped to company FVI (a second seed company FVS exists).
-- =====================================================================================
insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, 'TAX_REMITTANCE', 'Standard tax remittance', 100, date '2010-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company c where c.code = 'FVI';

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line)
select r.id, l.n, l.side, l.acc, l.comp, false
from acc_rule r
join org_company c on c.id = r.company_id and c.code = 'FVI',
(values
  (1, 'DEBIT',  '@TAX_PAYABLE', 'TAX_PAYABLE'),
  (2, 'CREDIT', '@TAX_CREDIT',  'TAX_CREDIT'),
  (3, 'CREDIT', '@BANK',        'AMOUNT')
) as l(n, side, acc, comp)
where r.event_type = 'TAX_REMITTANCE';
