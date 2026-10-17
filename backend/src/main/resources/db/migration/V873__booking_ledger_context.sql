-- =====================================================================================
-- iNXT BrokerVerse - V873 Ledger context of the booked invoices (Data Migration BRD-13, wave DM2-A;
-- docs/architecture/DATA_MIGRATION_DESIGN.md sections 14.4 H and 14.5).
--   bkg_invoice.ledger_context  NEW, or LEGACY for an endorsement or cancellation of a migrated
--                               invoice: its BROKER_BOOKING amounts carry the LG_ components, which
--                               the rules post on the legacy control accounts (seed rules V1982).
-- =====================================================================================

alter table bkg_invoice add column ledger_context varchar(10) not null default 'NEW';
alter table bkg_invoice add constraint ck_bkg_invoice_ledger_context
    check (ledger_context in ('NEW', 'LEGACY'));

update acc_event_type
set amount_components = amount_components || ',LG_PR_BASIC,LG_PR_DST,LG_PR_PTX_VAT,LG_PR_LGT,LG_PR_FST,'
    || 'LG_PR_OTHER,LG_DTIP,LG_COMMISSION_RECEIVABLE,LG_UNREALIZED_COMMISSION,LG_DEFERRED_OUTPUT_VAT,'
    || 'LG_COMMISSION_INCOME,LG_OUTPUT_VAT'
where code = 'BROKER_BOOKING' and amount_components not like '%LG_PR_BASIC%';
