-- =====================================================================================
-- iNXT BrokerVerse - V767 Origin of the receipt series (Data Migration BRD-13, wave DM1-C;
-- docs/architecture/DATA_MIGRATION_DESIGN.md sections 10 and 24). The AR and OR series in use in
-- the legacy system at the freeze (object R11) continue in BIBS from their next number; they show
-- a LEGACY badge and an Origin filter in the receipt series set-up.
-- =====================================================================================

alter table csh_receipt_series add column origin varchar(10) not null default 'BIBS';
alter table csh_receipt_series add column source_system varchar(10);
alter table csh_receipt_series add column legacy_ref varchar(80);
alter table csh_receipt_series add column migration_batch varchar(20);
alter table csh_receipt_series add constraint ck_csh_receipt_series_origin
    check (origin in ('BIBS', 'MIGRATED'));
