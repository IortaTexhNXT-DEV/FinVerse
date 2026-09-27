-- =====================================================================================
-- iNXT BrokerVerse - V818 Origin of insurers and commission rates (Data Migration BRD-13, wave
-- DM1-C; docs/architecture/DATA_MIGRATION_DESIGN.md sections 10 and 24).
--   origin            BIBS (maintained in BIBS) or MIGRATED (loaded from a legacy system)
--   source_system     legacy source system
--   legacy_ref        legacy insurer code / legacy rate key
--   migration_batch   the loading batch (rollback per batch)
-- Insurers the INSURER map creates (object R04) and the commission rates of object R07 show a
-- LEGACY badge and an Origin filter in the insurer set-up.
-- =====================================================================================

alter table cat_insurer add column origin varchar(10) not null default 'BIBS';
alter table cat_insurer add column source_system varchar(10);
alter table cat_insurer add column legacy_ref varchar(80);
alter table cat_insurer add column migration_batch varchar(20);
alter table cat_insurer add constraint ck_cat_insurer_origin check (origin in ('BIBS', 'MIGRATED'));

alter table cat_commission_rate add column origin varchar(10) not null default 'BIBS';
alter table cat_commission_rate add column source_system varchar(10);
alter table cat_commission_rate add column legacy_ref varchar(80);
alter table cat_commission_rate add column migration_batch varchar(20);
alter table cat_commission_rate add constraint ck_cat_commission_rate_origin
    check (origin in ('BIBS', 'MIGRATED'));
