-- =====================================================================================
-- iNXT BrokerVerse - V1088 Origin of products and payees (Data Migration BRD-13, waves DM1-B and
-- DM2-A; docs/architecture/DATA_MIGRATION_DESIGN.md sections 10 and 24). V1088 is the Data
-- Migration number reserved for owners whose own range is full (catalog, disbursement).
--   origin            BIBS (maintained in BIBS) or MIGRATED (loaded from a legacy system)
--   source_system     legacy source system
--   legacy_ref        legacy risk code / legacy payee code
--   migration_batch   the loading batch (rollback per batch)
-- Migrated products (object R05, risk codes the PRODUCT map creates) and payees (object R09) show a
-- LEGACY badge and an Origin filter in the product catalogue and the payee master.
-- =====================================================================================

alter table cat_product add column origin varchar(10) not null default 'BIBS';
alter table cat_product add column source_system varchar(10);
alter table cat_product add column legacy_ref varchar(80);
alter table cat_product add column migration_batch varchar(20);
alter table cat_product add constraint ck_cat_product_origin check (origin in ('BIBS', 'MIGRATED'));

alter table dsb_payee add column origin varchar(10) not null default 'BIBS';
alter table dsb_payee add column source_system varchar(10);
alter table dsb_payee add column legacy_ref varchar(80);
alter table dsb_payee add column migration_batch varchar(20);
alter table dsb_payee add constraint ck_dsb_payee_origin check (origin in ('BIBS', 'MIGRATED'));
create index ix_dsb_payee_origin on dsb_payee (company_id, origin);
