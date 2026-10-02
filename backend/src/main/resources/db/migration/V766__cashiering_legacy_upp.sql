-- =====================================================================================
-- iNXT BrokerVerse - V766 Legacy unapplied payments and legacy postings in Cashiering
-- (Data Migration BRD-13, wave DM1-B; docs/architecture/DATA_MIGRATION_DESIGN.md sections 14.3,
-- 14.4 and 14.5).
--   csh_unapplied.origin MIGRATED     an unapplied payment carried from legacy at cut-over (F02)
--   csh_unapplied.source_system,
--   legacy_ref, migration_batch        its source system, legacy reference and loading batch
--   csh_unapplied.ledger_context       NEW, or LEGACY: its money sits on the legacy unapplied
--                                      collections account (2206) and its applications, refunds
--                                      and reclassifications post with the LG_ components
--   legacy_ar_no, legacy_ar_date       the acknowledgement receipt issued in legacy (no BIBS AR)
--   match_refs                         invoice, cover, PN and bank references for the automatch
--   Legacy amount components on the Cashiering and Remittance events: an application to a legacy
--   invoice posts LG_PR_*, a legacy unapplied item LG_APPLIED / LG_AMOUNT, the commission realised
--   on a legacy invoice LG_REALIZED_* (the remittance components are added in V786, after the
--   remittance events of V771). Comptrollership adds the legacy lines to the rules (seed V1982).
-- =====================================================================================

alter table csh_unapplied drop constraint if exists csh_unapplied_origin_check;
alter table csh_unapplied add constraint ck_csh_unapplied_origin check (origin in ('NO_MATCH', 'EXCESS',
    'CANCELLED_REFERENCE', 'ADJUSTMENT', 'CANCELLATION', 'DP_REINSTATE', 'REMITTANCE_RETURN', 'REAPPLY',
    'PREBOOKED', 'OTHER', 'MIGRATED'));
alter table csh_unapplied add column source_system varchar(10);
alter table csh_unapplied add column legacy_ref varchar(80);
alter table csh_unapplied add column migration_batch varchar(20);
alter table csh_unapplied add column ledger_context varchar(10) not null default 'NEW';
alter table csh_unapplied add column legacy_ar_no varchar(40);
alter table csh_unapplied add column legacy_ar_date date;
alter table csh_unapplied add column match_refs varchar(500);
alter table csh_unapplied add constraint ck_csh_unapplied_ledger_context check (ledger_context in ('NEW', 'LEGACY'));
alter table csh_unapplied add constraint ck_csh_unapplied_migrated
    check (origin <> 'MIGRATED' or (source_system is not null and legacy_ref is not null
                                    and ledger_context = 'LEGACY'));
create index ix_csh_unapplied_origin on csh_unapplied (company_id, origin);
create index ix_csh_unapplied_legacy_ref on csh_unapplied (legacy_ref) where legacy_ref is not null;

update acc_event_type
set amount_components = amount_components
        || ',LG_APPLIED,LG_PR_BASIC,LG_PR_DST,LG_PR_PTX_VAT,LG_PR_LGT,LG_PR_FST,LG_PR_OTHER'
        || ',LG_REALIZED_COMMISSION,LG_REALIZED_VAT'
where code = 'OPS_PAYMENT_APPLY' and amount_components not like '%LG_APPLIED%';

update acc_event_type
set amount_components = amount_components || ',LG_AMOUNT'
where code = 'OPS_UNAPPLIED_REFUND' and amount_components not like '%LG_AMOUNT%';

update acc_event_type
set amount_components = amount_components || ',LG_RELEASED,LG_ASSIGNED'
where code = 'OPS_UNAPPLIED_RECLASS' and amount_components not like '%LG_RELEASED%';

-- A migrated item of a rolled-back migration batch is closed by the system.
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('OPS_DISPOSITION', 'UNAPPLIED', 'migration_rollback', 'CLOSED', 'Closed by Migration Rollback', 'CASH_APPROVE',
        false, null, 50);
