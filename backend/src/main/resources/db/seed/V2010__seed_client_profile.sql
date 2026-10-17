-- Client profile of the seed companies (V1150, range V2010-V2019 of the client values moved out of
-- the platform). Seed profile only: the client's identity lives in the company master, not in the
-- platform code. The input VAT tax code of FVI is kept in the tax code master, where the supplier
-- invoices take the VAT rate from (the tax seed runner skips a code that already exists).
update org_company
set short_name = 'BDOI', group_name = 'BDO', logo_ref = 'theme:logo', head_office_code = 'HO',
    default_bank_code = 'BDO-CA'
where code = 'FVI';

update org_company
set short_name = 'BDOI Singapore', group_name = 'BDO', logo_ref = 'theme:logo',
    head_office_code = 'HO'
where code = 'FVS';

insert into tax_code (company_id, code, name, tax_type, atc, payee_class, rate, gl_account_code,
    income_nature, effective_from, effective_to, record_status, authorized_by, authorized_at,
    created_at, created_by)
select c.id, 'VAT-IN', 'Input VAT 12%', 'VAT_INPUT', null, null, 12, '1603', null,
       date '2020-01-01', null, 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
where c.code = 'FVI'
  and not exists (select 1 from tax_code t where t.company_id = c.id and t.code = 'VAT-IN');
