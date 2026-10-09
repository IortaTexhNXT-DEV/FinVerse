-- =====================================================================================
-- SIT/UAT seed data (seed profile only; never loaded in production): the Product Maintenance
-- workspace of BDOI's FRS - package descriptions on the Product Matrix, three package deactivation
-- requests (pending, rejected with remarks, approved with the new expiry date) and an anniversary
-- date within the 90-day notice, so the dashboard, the Product Matrix tabs and the Deactivation
-- Requests list show realistic rows. Dates are relative to the load date.
-- =====================================================================================

update cat_product set description = v.description, updated_at = now(), updated_by = 'mbs'
from (values
    ('MTR10', 'Comprehensive motor cover for private cars with own damage, theft, third party and Acts of Nature'),
    ('MTR12', 'Comprehensive motor cover for bank auto-loan vehicles with roadside assistance'),
    ('MTR27', 'Motor cover for light commercial vehicles of SME fleets'),
    ('MTR28', 'Motor cover for motorcycles financed by the bank'),
    ('MTR29', 'Motor cover for employee vehicles of corporate clients'),
    ('PAR19', 'Fire and allied perils for residential properties under housing loans'),
    ('PAR25', 'Fire and allied perils for small business premises')
) as v(code, description)
where cat_product.code = v.code;

insert into pm_deactivation_request (company_id, request_no, product_code, version_no, package_name,
    effective_date, reason, remarks, approver, status, package_end_date, decision_remarks,
    decided_by, decided_at, expiry_date, created_at, created_by)
select (select id from org_company where code = 'FVI'), v.request_no, p.code, 1, p.name,
       current_date + v.effective_in, v.reason, v.remarks, 'tsuhead', v.status, null,
       v.decision_remarks, v.decided_by,
       case when v.decided_by is null then null else now() - interval '1 day' end,
       case when v.status = 'APPROVED' then current_date + v.effective_in end,
       now() - v.age, 'mbs'
from (values
    ('PKD-2026-900001', 'MTR27', 30, 'LOW_UPTAKE', 'Fewer than ten accounts in twelve months',
     'PENDING', null, null, interval '2 day'),
    ('PKD-2026-900002', 'MTR28', 15, 'INSURER_WITHDRAWAL', 'Lead insurer gave notice of withdrawal',
     'REJECTED', 'The insurer renewed the terms for another year', 'tsuhead', interval '6 day'),
    ('PKD-2026-900003', 'MTR29', 45, 'REPLACED', 'Replaced by the new corporate fleet package',
     'APPROVED', 'Approved; accounts renew to the new package', 'tsuhead', interval '9 day')
) as v(request_no, code, effective_in, reason, remarks, status, decision_remarks, decided_by, age)
join cat_product p on p.code = v.code;

-- The approved request set the package expiry date of MTR29.
update cat_product_version set package_end_date = current_date + 45, updated_at = now(),
    updated_by = 'tsuhead'
where product_code = 'MTR29' and version_no = 1;

-- A package reaching its anniversary within the 90-day notice.
update cat_product_version set anniversary_date = current_date + 60
where product_code = 'MTR10' and version_no = 1;
