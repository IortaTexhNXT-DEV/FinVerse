-- =====================================================================================
-- iNXT BrokerVerse - V29 Journals: assignment, automatic reversal date, correction links
-- (BRD-5 FRBS / ACSL, wave A1-GL).
--   FRBS 2.5.1  manual entries assigned to a poster by the TL (assigned_to, "assigned to me");
--   FRBS 2.8.1  reversal date of accruals: job JOURNAL_AUTO_REVERSAL posts the reversal on
--               reverse_on (system journal REVERSAL linked by reversal_of_id);
--   ACSL 2.9.1 / 2.16.0  a correcting journal points at the journal it corrects and carries the
--               related and root invoice numbers (one invoice family).
-- =====================================================================================
alter table jnl_batch add column assigned_to varchar(50);
alter table jnl_batch add column assigned_by varchar(50);
alter table jnl_batch add column assigned_at timestamptz;
alter table jnl_batch add column reverse_on date;
alter table jnl_batch add column corrects_batch_id bigint references jnl_batch (id);
alter table jnl_batch add column related_invoice_no varchar(40);
alter table jnl_batch add column root_invoice_no varchar(40);

create index ix_batch_assigned on jnl_batch (company_id, assigned_to, status)
    where assigned_to is not null;
create index ix_batch_reverse_on on jnl_batch (reverse_on)
    where reverse_on is not null and reversed_by_id is null;
create index ix_batch_root_invoice on jnl_batch (company_id, root_invoice_no)
    where root_invoice_no is not null;
create index ix_batch_corrects on jnl_batch (corrects_batch_id) where corrects_batch_id is not null;
