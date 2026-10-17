-- =====================================================================================
-- iNXT BrokerVerse - V874 Booking: incentive eligibility of a booked and fully paid transaction
-- (New Business BRD after the IT walkthrough BRNB.107, FR-NB-118; Renewal BRRN.041, FR-RN-087).
--   bkg_invoice               incentive status PENDING (not fully paid), ELIGIBLE or NOT_ELIGIBLE
--                             with the time and reason of the latest evaluation. The criteria codes
--                             matched at booking stay the tag of the transaction (CPC2, FR-DS-090);
--                             the indicator is set only when the invoice is booked and fully paid
--                             (and, for a renewal, its acceptance confirmed), re-evaluated on a
--                             financial endorsement and invalidated on cancellation.
--   The history of the evaluations (bkg_incentive_evaluation) is created by V1340, after the
--   platform guards of the insert-only tables (V1191).
-- Invoices booked before this version keep their indicator as evaluated at booking.
-- =====================================================================================
alter table bkg_invoice add column incentive_status varchar(20) not null default 'PENDING'
    constraint ck_bkg_invoice_incentive check (incentive_status in ('PENDING', 'ELIGIBLE',
        'NOT_ELIGIBLE'));
alter table bkg_invoice add column incentive_evaluated_at timestamptz;
alter table bkg_invoice add column incentive_reason varchar(300);

update bkg_invoice
set incentive_status = case when incentive_eligible then 'ELIGIBLE' else 'NOT_ELIGIBLE' end,
    incentive_evaluated_at = booked_at,
    incentive_reason = 'Evaluated at booking'
where status = 'BOOKED';
