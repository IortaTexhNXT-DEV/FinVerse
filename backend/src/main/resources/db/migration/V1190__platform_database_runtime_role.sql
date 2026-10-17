-- =====================================================================================
-- iNXT BrokerVerse - V1190 Platform hardening (range V1190-V1199): least-privilege runtime role.
-- Flyway runs as the owner of the schema objects (SPRING_FLYWAY_USER); the application connects
-- as a login that is a member of the runtime role ${runtime_role} (placeholder
-- spring.flyway.placeholders.runtime_role, env BROKERVERSE_DB_RUNTIME_ROLE). The runtime role
-- may read and write rows (SELECT, INSERT, UPDATE, DELETE) and use the sequences, nothing more:
-- no DDL, no TRUNCATE, no TRIGGER or REFERENCES privilege, never the owner of an object, so it
-- can neither alter a table nor disable the immutability triggers. Tables and sequences created
-- later by the owner get the same grants through the default privileges. The Flyway history is
-- read-only for the runtime role. See docs/operations/DEPLOYMENT.md "Database roles".
-- =====================================================================================

do $$
declare
    runtime text := '${runtime_role}';
    schema_name text := current_schema();
begin
    if not exists (select 1 from pg_roles where rolname = runtime) then
        if exists (select 1 from pg_roles where rolname = current_user and (rolcreaterole or rolsuper)) then
            execute format('create role %I nologin', runtime);
        else
            raise exception 'Database role % does not exist and % may not create it: create it first (DEPLOYMENT.md "Database roles")',
                runtime, current_user;
        end if;
    end if;
    if exists (select 1 from pg_roles where rolname = runtime and rolsuper) then
        raise exception 'Database role % is a superuser; the runtime role must not be', runtime;
    end if;

    -- Nobody but the owner creates objects in the application schema (the default from
    -- PostgreSQL 15; a warning only when the migration user does not own the schema).
    execute format('revoke create on schema %I from public', schema_name);
    execute format('grant usage on schema %I to %I', schema_name, runtime);

    -- Existing objects.
    execute format('grant select, insert, update, delete on all tables in schema %I to %I', schema_name, runtime);
    execute format('revoke truncate, references, trigger on all tables in schema %I from %I', schema_name, runtime);
    execute format('grant usage, select on all sequences in schema %I to %I', schema_name, runtime);
    execute format('revoke insert, update, delete on table %I.%I from %I', schema_name, '${flyway:table}', runtime);

    -- Objects the owner creates in later migrations.
    execute format('alter default privileges in schema %I grant select, insert, update, delete on tables to %I',
        schema_name, runtime);
    execute format('alter default privileges in schema %I grant usage, select on sequences to %I',
        schema_name, runtime);
end
$$;
