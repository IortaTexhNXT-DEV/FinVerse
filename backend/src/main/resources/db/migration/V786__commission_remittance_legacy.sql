-- =====================================================================================
-- iNXT BrokerVerse - V786 Legacy amount components of the Remittance events (Data Migration
-- BRD-13, wave DM1-B; docs/architecture/DATA_MIGRATION_DESIGN.md sections 14.4 G and 14.5).
--   OPS_REMITTANCE: a remittance line of a legacy invoice settles the legacy due to insurer and
--   the legacy commission receivable (LG_DTIP, LG_COMMISSION_RECEIVABLE); Comptrollership adds the
--   legacy lines to the rule (seed rules V1982). Runs after the remittance events of V771.
-- =====================================================================================

update acc_event_type
set amount_components = amount_components || ',LG_DTIP,LG_COMMISSION_RECEIVABLE'
where code = 'OPS_REMITTANCE' and amount_components not like '%LG_DTIP%';
