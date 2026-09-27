-- =====================================================================================
-- iNXT BrokerVerse - V803 Origin of a client (Data Migration BRD-13, wave DM1-C;
-- docs/architecture/DATA_MIGRATION_DESIGN.md sections 9 and 14.5).
--   origin           BIBS (created in BIBS) or MIGRATED (loaded from a legacy system)
--   source_system    legacy source system of a migrated client (EBIX, QPS, CMS...)
--   legacy_ref       legacy client code
--   migration_batch  migration batch that loaded the client (rollback per batch)
-- A migrated client is registered as a confirmed client without the onboarding workflow and
-- is not screened on registration; the cut-over plan runs one full screening after the load.
-- =====================================================================================

alter table crm_client add column origin varchar(10) not null default 'BIBS';
alter table crm_client add column source_system varchar(10);
alter table crm_client add column legacy_ref varchar(80);
alter table crm_client add column migration_batch varchar(20);
alter table crm_client add constraint ck_crm_client_origin check (origin in ('BIBS', 'MIGRATED'));
alter table crm_client add constraint ck_crm_client_origin_ref
    check (origin = 'BIBS' or (source_system is not null and legacy_ref is not null));
create index ix_crm_client_origin on crm_client (company_id, origin);
create index ix_crm_client_legacy_ref on crm_client (legacy_ref) where legacy_ref is not null;

insert into lov_value (type_code, code, label, sort_order, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select 'CLIENT_DEACTIVATION_REASON', 'MIGRATION_ROLLBACK', 'Migration batch rolled back', 90,
       date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
where not exists (select 1 from lov_value
                  where type_code = 'CLIENT_DEACTIVATION_REASON' and code = 'MIGRATION_ROLLBACK');
