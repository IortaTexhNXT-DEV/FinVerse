-- =====================================================================================
-- iNXT BrokerVerse - V1191 Platform hardening (range V1190-V1199): every insert-only table refuses
-- UPDATE, DELETE and TRUNCATE at database level.
-- TRUNCATE bypasses row triggers, so each immutable table also needs a statement trigger. Already
-- complete: audit_log (V26), sec_access_change_log (V1061), nba_access_request_event (V1062).
-- Added here:
--   TRUNCATE guard for the tables whose row guards exist: gl_ledger_entry (V1), scr_watchlist_change
--   (V1052, decided changes read only), scr_client_risk_profile (V1053), scr_case_event and
--   scr_committee_vote (V1054), mig_access_log (V1085);
--   row and TRUNCATE guards for the tables the application only ever inserts into: bcl_insurer_update
--   and bcl_reserve_change (V1022), bcl_status_history (V1021), csf_activity (V1041),
--   crm_client_note_history (V801), ops_invoice_origin_snapshot (V1086), clm_movement (V200);
--   TRUNCATE guard only for wf_case_history (V751; its remarks are corrected by a seed migration) and
--   clx_unapplied_disposition (V1004; a disposition is linked to its application request later).
-- A correction is a new row (reversal, superseding entry). Archiving or correcting such a table is a
-- DBA task on record: only the table owner can disable the triggers; the runtime role cannot (V1190).
-- =====================================================================================

create function platform_row_immutable() returns trigger language plpgsql as $$
begin
    raise exception '% rows are immutable; the table is insert-only', tg_table_name;
end;
$$;

create function platform_refuse_truncate() returns trigger language plpgsql as $$
begin
    raise exception '% rows are immutable; the table cannot be truncated', tg_table_name;
end;
$$;

do $$
declare
    t text;
begin
    foreach t in array array['bcl_insurer_update', 'bcl_reserve_change', 'bcl_status_history', 'csf_activity',
                             'crm_client_note_history', 'ops_invoice_origin_snapshot', 'clm_movement'] loop
        execute format('create trigger %I before update or delete on %I for each row execute function platform_row_immutable()',
            'trg_' || t || '_immutable', t);
    end loop;
    foreach t in array array['gl_ledger_entry', 'scr_watchlist_change', 'scr_client_risk_profile', 'scr_case_event',
                             'scr_committee_vote', 'mig_access_log', 'bcl_insurer_update', 'bcl_reserve_change',
                             'bcl_status_history', 'csf_activity', 'clx_unapplied_disposition',
                             'crm_client_note_history', 'ops_invoice_origin_snapshot', 'clm_movement',
                             'wf_case_history'] loop
        execute format('create trigger %I before truncate on %I for each statement execute function platform_refuse_truncate()',
            'trg_' || t || '_no_truncate', t);
    end loop;
end
$$;
