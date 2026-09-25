-- =====================================================================================
-- iNXT BrokerVerse - V831 Quotations: package version that priced the current quotation
-- version and the approved rate-scheme exception it uses (Product Maintenance BRD-3,
-- BRPM.007). The version is also kept in the JSON content of every quotation version.
--   Design: docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md sections 3 and 9.2.
-- =====================================================================================
alter table quo_quotation add column product_version_no integer;
alter table quo_quotation add column rate_override_ref varchar(30);
alter table quo_quotation add constraint ck_quo_quotation_product_version
    check (product_version_no is null or product_version_no >= 1);
