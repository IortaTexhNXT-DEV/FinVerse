-- Field-level change log of master data (Sprint 2, G05): every change of a maker-checker master
-- record (insurers, products, parties, lists of values and the other authorizable masters) and of a
-- client, one row per changed field with the value before and after, the maker and the time of the
-- change; the checker and the time of the authorization are added when the change is authorized.
create table master_change_log (
    id               bigserial primary key,
    entity_type      varchar(80)  not null,
    entity_id        bigint       not null,
    change_kind      varchar(12)  not null,
    field_name       varchar(80)  not null,
    old_value        varchar(2000),
    new_value        varchar(2000),
    maker            varchar(50),
    made_at          timestamptz  not null,
    checker          varchar(50),
    checked_at       timestamptz,
    constraint ck_master_change_kind check (change_kind in ('CREATE', 'UPDATE'))
);

create index ix_master_change_log_record on master_change_log (entity_type, entity_id, made_at);
create index ix_master_change_log_pending on master_change_log (entity_type, entity_id)
    where checker is null;
