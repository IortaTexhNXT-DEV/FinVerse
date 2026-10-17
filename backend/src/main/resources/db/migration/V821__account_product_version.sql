-- =====================================================================================
-- iNXT BrokerVerse - V821 Accounts: package version and rate-scheme exception of the account
-- (Product Maintenance BRD-3, BRPM.007: "the scheme version used is logged for each
-- transaction"). Filled from the quotation / PRF or by rating a direct account on the current
-- version; null for non-packaged products and for accounts created before this change.
--   Design: docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md sections 3 and 9.3.
-- =====================================================================================
alter table acc_account add column product_version_no integer;
alter table acc_account add column rate_override_ref varchar(30);
alter table acc_account add constraint ck_acc_account_product_version
    check (product_version_no is null or product_version_no >= 1);
