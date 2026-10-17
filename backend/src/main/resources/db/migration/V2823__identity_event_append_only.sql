-- The identity events received from UIDM-ISC and the Enterprise SSO platform are the evidence of
-- every joiner, mover, leaver and group-profile membership change. Like the audit trail (V26), the
-- access change log (V1061) and the access request history (V1062), they are kept at database
-- level for every client: a row is never deleted or truncated, and what was received (source, type,
-- identifiers, payload, time) never changes. Only the processing outcome of a refused or failed
-- event is updated when it is reprocessed (status, user, message, processed at/by, attempts).
-- There is no retention purge; archiving is a DBA task done by the table owner with the triggers
-- disabled, on record.

create function idn_identity_event_guard() returns trigger language plpgsql as $$
begin
    if tg_op = 'UPDATE'
        and new.id = old.id
        and new.received_at = old.received_at
        and new.source = old.source
        and new.event_type = old.event_type
        and new.windows_id is not distinct from old.windows_id
        and new.user_id is not distinct from old.user_id
        and new.uidm_request_no is not distinct from old.uidm_request_no
        and new.ad_status is not distinct from old.ad_status
        and new.payload = old.payload then
        return new;
    end if;
    raise exception 'idn_identity_event rows are append-only; only the processing outcome changes';
end;
$$;

create trigger trg_idn_identity_event_guard
    before update or delete on idn_identity_event
    for each row execute function idn_identity_event_guard();

create trigger trg_idn_identity_event_no_truncate
    before truncate on idn_identity_event
    for each statement execute function idn_identity_event_guard();
