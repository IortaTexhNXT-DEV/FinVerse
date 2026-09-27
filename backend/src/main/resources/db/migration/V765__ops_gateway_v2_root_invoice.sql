-- =====================================================================================
-- iNXT BrokerVerse - V765 Operations ledger contract for BRD-5 (Accounting, Disbursement, ACSL).
--   Requirements: docs/requirements/BDOI_ACCT_BRD_SPEC.md
--     DIS 3.25.0    system-triggered requests carry the RFP number, payee class, disbursement
--                   type, supporting documents and accounting references; refunds to clients and
--                   remittances to insurers go straight to the approver
--     DIS 2.20.0    an approved DV may be cancelled: gateway status CANCELLED, sources regularise
--     DIS 2.8, 3.26 DV stage and instrument status reported back to the source
--     DIS 3.27.2, ACSL 2.16.0  related transactions linked to a single (root) invoice number
--     ACSL 2.9.1    corrections recorded on the invoice as CORRECTION movements
--   Design: docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md sections 2.2, 4 and 12.2 (wave A0).
--   Only V761 tables are changed; no foreign key to V8xx tables.
-- =====================================================================================

-- ---------- Payment requests: gateway v2 (DIS 3.25.0, 2.20.0, 2.8, 3.26, 3.27.2) ----------
alter table ops_disbursement_request drop constraint ops_disbursement_request_request_type_check;
alter table ops_disbursement_request add constraint ck_ops_disbursement_type check (request_type in (
    'REMITTANCE', 'REFUND', 'CWT2307', 'PASS_ON', 'SUPPLIER', 'GOVERNMENT', 'OTHER_BANK_UNIT',
    'EMPLOYEE', 'CASH_ADVANCE', 'SERVICE_FEE', 'OTHER'));
alter table ops_disbursement_request drop constraint ops_disbursement_request_status_check;
alter table ops_disbursement_request add constraint ck_ops_disbursement_status check (status in (
    'SENT', 'ACKNOWLEDGED', 'DV_ASSIGNED', 'PAID', 'RETURNED', 'CANCELLED'));

alter table ops_disbursement_request
    add column rfp_no               varchar(40),
    add column payee_class          varchar(30),
    add column disbursement_type    varchar(40),
    add column root_invoice_no      varchar(40),
    add column attachment_refs      varchar(1000),
    add column accounting_refs      varchar(1000),
    add column straight_to_approval boolean      not null default false,
    add column dv_status            varchar(30),
    add column instrument_status    varchar(30),
    add column cancelled_at         timestamptz,
    add column cancel_reason        varchar(250);
alter table ops_disbursement_request add constraint ck_ops_disbursement_cancel
    check ((status = 'CANCELLED') = (cancelled_at is not null));
create index ix_ops_disbursement_root on ops_disbursement_request (root_invoice_no)
    where root_invoice_no is not null;

-- ---------- Invoice family (DIS 3.27.2, ACSL 2.16.0, AQ29) -----------------------------------
-- The root of an original booking is its own number; endorsements and cancellations take the
-- root of their parent chain. Existing rows are back-filled by walking the parent chain.
alter table ops_invoice add column root_invoice_no varchar(40);

with recursive family (id, invoice_no, root_invoice_no, depth) as (
    select i.id, i.invoice_no, i.invoice_no, 0
    from ops_invoice i
    where i.parent_invoice_no is null
       or not exists (select 1 from ops_invoice p where p.invoice_no = i.parent_invoice_no)
    union all
    select c.id, c.invoice_no, f.root_invoice_no, f.depth + 1
    from ops_invoice c
    join family f on c.parent_invoice_no = f.invoice_no
    where f.depth < 50
)
update ops_invoice i
set root_invoice_no = coalesce(f.root_invoice_no, i.parent_invoice_no, i.invoice_no)
from family f
where f.id = i.id;

update ops_invoice set root_invoice_no = coalesce(parent_invoice_no, invoice_no)
where root_invoice_no is null;

alter table ops_invoice alter column root_invoice_no set not null;
create index ix_ops_invoice_root on ops_invoice (root_invoice_no);

-- ---------- CORRECTION movements (ACSL 2.9.1) ------------------------------------------------
alter table ops_invoice_movement drop constraint ck_ops_invoice_movement_type;
alter table ops_invoice_movement add constraint ck_ops_invoice_movement_type check (movement_type in (
    'BOOKED', 'APPLIED', 'UNAPPLIED', 'REMITTED', 'ADJUSTED', 'WRITE_OFF', 'DP_REVERSAL',
    'CWT_RECLASS', 'MIN_BAL', 'CORRECTION'));
