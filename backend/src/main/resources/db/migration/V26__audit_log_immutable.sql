-- =====================================================================================
-- iNXT BrokerVerse - V26 The audit trail is insert-only at database level.
-- The application only ever inserts into audit_log (AuditTrailService); this trigger makes
-- the rule hold for every database client as well, the same way V1 protects
-- gl_ledger_entry. UPDATE and DELETE are rejected row by row; TRUNCATE (which bypasses row
-- triggers) is rejected by a statement trigger. There is no retention purge: archiving the
-- audit trail is a DBA task that needs the triggers disabled by the table owner, on record.
-- =====================================================================================

create function audit_log_immutable() returns trigger language plpgsql as $$
begin
    raise exception 'audit_log rows are immutable; the audit trail is insert-only';
end;
$$;

create trigger trg_audit_log_immutable
    before update or delete on audit_log
    for each row execute function audit_log_immutable();

create trigger trg_audit_log_no_truncate
    before truncate on audit_log
    for each statement execute function audit_log_immutable();
