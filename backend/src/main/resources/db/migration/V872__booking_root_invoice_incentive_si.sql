-- =====================================================================================
-- iNXT BrokerVerse - V872 Booking changes of BRD-5 (A1-OPSX; ACCOUNTING_DISBURSEMENT_DESIGN 12.2):
--   DIS 3.27.2 / ACSL 2.16.0  root invoice number of the invoice family: the invoice itself for an
--               original booking, the original invoice for its endorsements and cancellation (the
--               same value as ops_invoice.root_invoice_no, V765)
--   DIS 3.29.1  service invoice trigger ON_INCENTIVE and type EARLY_INCENTIVE, issued by remittance
--               after the early-incentive computation with the 2% withholding tax; never issued by hand
-- =====================================================================================

-- ---------- Root invoice number (DIS 3.27.2) ------------------------------------------------
alter table bkg_invoice add column root_invoice_no varchar(40);

-- Back-fill along the parent chain (originals are their own root)
with recursive family (id, invoice_no, root_invoice_no) as (
    select i.id, i.invoice_no, i.invoice_no
    from bkg_invoice i
    where i.parent_invoice_no is null and i.invoice_no is not null
    union all
    select c.id, c.invoice_no, f.root_invoice_no
    from bkg_invoice c
    join family f on c.parent_invoice_no = f.invoice_no
    where c.invoice_no is not null
)
update bkg_invoice b
set root_invoice_no = f.root_invoice_no
from family f
where b.id = f.id;

-- Endorsements whose parent is not a booked invoice keep the parent as root (as the ledger does)
update bkg_invoice
set root_invoice_no = parent_invoice_no
where root_invoice_no is null and invoice_no is not null and parent_invoice_no is not null;

create index ix_bkg_invoice_root on bkg_invoice (root_invoice_no);

-- ---------- Early-incentive service invoice (DIS 3.29.1) -------------------------------------
alter table bkg_si_type drop constraint if exists bkg_si_type_trigger_type_check;
alter table bkg_si_type add constraint ck_bkg_si_type_trigger
    check (trigger_type in ('ON_BOOKING', 'ON_ENDORSEMENT', 'MANUAL', 'ON_INCENTIVE'));

insert into bkg_si_type (code, name, recipient, trigger_type, owner_permission, template_code,
    created_at, created_by)
select 'EARLY_INCENTIVE', 'Early remittance incentive invoice to insurer', 'INSURER', 'ON_INCENTIVE',
       'REMIT_APPROVE', 'SERVICE_INVOICE_NOTE', now(), 'SYSTEM'
where not exists (select 1 from bkg_si_type where code = 'EARLY_INCENTIVE');
