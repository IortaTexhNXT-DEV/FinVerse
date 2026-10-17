-- Product Maintenance corrections found by the business sign-off packs (BRD-1, BRD-3).
--
-- 1. Package products of the Fire (PAR) and Motor (MTR) matrices.
--
-- The 23 codes MTR08..MTR38 and PAR01..PAR25 are the BDOI package products of the BRD product
-- matrices; production needs them (accounts, renewals and migrated policies refer to the codes).
-- Their matrices (rates, limits, wordings) are not loaded yet, so V810 gave them working names.
-- This migration gives each package a business name and makes it inactive: an inactive product is
-- kept, can be read and reported on, but cannot be quoted or sold until Product Maintenance loads
-- its matrix and activates it. The seed profile activates them for testing (V2001 in db/seed).

update cat_product p
set name = case
        when p.line_code = 'MOTOR' then 'Motor Comprehensive Package ' || p.code
        when p.cover_type_code = 'ALL_RISK' then 'Property All Risks Package ' || p.code
        when p.cover_type_code = 'BROAD_NAMED_PERILS' then 'Broad Named Perils Package ' || p.code
        else 'Fire and Lightning Package ' || p.code
    end,
    record_status = 'INACTIVE',
    updated_at = now(),
    updated_by = 'SYSTEM'
where p.code in ('MTR08', 'MTR10', 'MTR12', 'MTR15', 'MTR22', 'MTR23', 'MTR24', 'MTR25', 'MTR26',
                 'MTR27', 'MTR28', 'MTR29', 'MTR31', 'MTR32', 'MTR34', 'MTR35', 'MTR38',
                 'PAR01', 'PAR08', 'PAR09', 'PAR13', 'PAR19', 'PAR25')
  and p.name like '% (product matrix to be loaded)';

-- 2. ManCom and the TSU Team Head / Head attach documents to package requests: the signed ManCom
--    sheet (MANCOM_SIGNOFF) and the TSU approval papers. Their roles had the document view only; they
--    get the document upload permission like the other package roles (TSU Team Lead, MBS).
insert into sec_role_permission (role_id, permission)
select r.id, 'ATTACHMENT_MANAGE'
from sec_role r
where r.code in ('MANCOM', 'TSU_HEAD')
  and not exists (select 1 from sec_role_permission x
                  where x.role_id = r.id and x.permission = 'ATTACHMENT_MANAGE');
