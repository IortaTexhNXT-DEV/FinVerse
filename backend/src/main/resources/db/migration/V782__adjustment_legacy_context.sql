-- =====================================================================================
-- iNXT BrokerVerse - V782 Legacy components of the Adjustment events (Data Migration BRD-13;
-- docs/architecture/DATA_MIGRATION_DESIGN.md sections 14.4 H, L and 24). A write-off or a
-- commission change on a legacy invoice posts its premium receivable, commission receivable,
-- unrealised commission and deferred output VAT on the legacy control accounts (LG_ components;
-- the rules carry the legacy lines).
-- =====================================================================================

update acc_event_type
set amount_components = amount_components || ',LG_PR_BASIC,LG_PR_DST,LG_PR_PTX_VAT,LG_PR_LGT,LG_PR_FST,LG_PR_OTHER'
where code = 'OPS_WRITE_OFF' and amount_components not like '%LG_PR_BASIC%';

update acc_event_type
set amount_components = amount_components
    || ',LG_COMMISSION_RECEIVABLE,LG_UNREALIZED_COMMISSION,LG_DEFERRED_OUTPUT_VAT'
where code = 'OPS_ADJ_COMMISSION' and amount_components not like '%LG_COMMISSION_RECEIVABLE%';
