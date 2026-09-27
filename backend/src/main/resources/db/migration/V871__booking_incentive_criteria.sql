-- =====================================================================================
-- iNXT BrokerVerse - V871 Booking: package version and incentive criteria of a booked invoice
-- (Product Maintenance BRD-3).
--   BRPM.007  the invoice carries the package version of its account
--   PMADD07   booking stamps the codes of the catalog incentive criteria that match the invoice
--             (comma-separated); the incentive flag is "at least one code"
--   Transition: the booking incentive rules (bkg_incentive_rule, BRNB.107) are copied once into
--   the catalog master as criteria of type MIGRATED, pending authorisation (an authoriser reviews
--   them in My Approvals), and the booking table is frozen: its API is read-only from now on.
--   Design: docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md sections 3 and 9.4.
-- =====================================================================================
alter table bkg_invoice add column product_version_no integer;
alter table bkg_invoice add column incentive_criteria varchar(500);

insert into cat_incentive_criteria (company_id, code, name, incentive_type, value_basis, value,
    description, effective_from, effective_to, record_status, created_at, created_by)
select r.company_id, 'MIG-' || r.id, left(r.description, 150), 'MIGRATED', 'RULE', null,
       'Migrated from booking incentive rule ' || r.id
           || case when r.active then '' else ' (inactive)' end,
       r.period_from, r.period_to, 'PENDING_AUTHORIZATION', now(), 'SYSTEM'
from bkg_incentive_rule r
where r.product_code is not null
  and exists (select 1 from cat_product p where p.code = r.product_code);

insert into cat_incentive_criteria_product (criteria_id, product_code, market_segment, source_channel)
select c.id, r.product_code, r.market_segment, r.source_channel
from bkg_incentive_rule r
join cat_incentive_criteria c on c.company_id = r.company_id and c.code = 'MIG-' || r.id;
