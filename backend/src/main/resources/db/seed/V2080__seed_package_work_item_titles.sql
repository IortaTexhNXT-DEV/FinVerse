-- Seed data correction found by the Product Maintenance screenshots: the work items of the seeded package requests
-- were described as "<request type code> <product code or 'new package'> - <title>" (for example "NEW new package -
-- SME Motor Fleet Programme"). The running system now describes them by the request type in words and the package
-- title ("New package: SME Motor Fleet Programme"); the seeded descriptions follow.
-- Seed profile only; only a description still in the former form is changed.
update wf_case w
set title = case r.request_type
              when 'NEW' then 'New package'
              when 'AMEND' then 'Amendment'
              when 'UPDATE' then 'Update'
              when 'RENEW' then 'Renewal'
              when 'RETIRE' then 'Retirement'
              when 'REACTIVATE' then 'Reactivation'
            end || ': ' || r.title
from pm_request r
where w.workflow_code = 'PM_PACKAGE_REQUEST'
  and w.reference = r.request_no
  and w.company_id = r.company_id
  and r.request_type in ('NEW', 'AMEND', 'UPDATE', 'RENEW', 'RETIRE', 'REACTIVATE')
  and w.title = r.request_type || ' ' || coalesce(r.target_product_code, 'new package') || ' - ' || r.title;

-- The seeded package advisory gave its effective date as 2026-09-01; advisories write dates as dd-MMM-yyyy.
update pm_advisory
set body = replace(body, 'for new business from 2026-09-01', 'for new business from 01-Sep-2026')
where product_code = 'PAR08' and version_no = 2 and body like '%for new business from 2026-09-01%';
