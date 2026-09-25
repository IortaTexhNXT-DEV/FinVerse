-- =====================================================================================
-- iNXT BrokerVerse - V819 Catalog: Unit Head of a sales unit (BRCLXN.011/012, CQ05).
--   Design: docs/architecture/COLLECTIONS_DESIGN.md sections 9 and 14 (deferred from C0 to C1-A;
--   V818 / V819 were held by Product Maintenance, which merged without using them).
--   The collection worklist resolves the Unit Head of an invoice from its sales unit, walking up
--   to the department and region when the team has none (SalesOrganisationService.unitHead).
--   The head is an operational attribute: it is audited, not re-authorized. BDOI to confirm the
--   source of the Unit Head (CQ05).
-- =====================================================================================

alter table cat_sales_unit add column head_username varchar(50);
