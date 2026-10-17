-- =====================================================================================
-- iNXT BrokerVerse - V1011 Renewal (BRD-6) contract changes in the New Business modules
-- (design section 13): the system fast track of an accepted renewal account, the booking queue
-- source RENEWAL and the renewal link of quotations and proposal requests (New Business path).
--   BRRN.040  an accepted renewal proceeds to payment, placement and booking without resubmission
--   BRRN.033  a renewal with changes becomes a quotation or PRF linked by the renewal reference;
--             its accounts are created as RENEWAL of the expiring policy (shared work item BT0)
-- The account columns business_type / renewal_of_ref are the shared V822 (applied before V1011).
-- renewal_of_ref is kept next to renewal_ref on the quotation and the PRF so that the accounts they
-- create name the expiring policy without reading the renewal module.
-- =====================================================================================

-- ---------- NB_ACCOUNT: system fast track of a renewal account (design 7.2) --------------------
insert into wf_transition (workflow_code, from_stage, action, to_stage, label, permission, generic, reason_lov, sort_order)
values ('NB_ACCOUNT', 'DRAFT', 'renewal_fast_track', 'AWAITING_PAYMENT', 'Renewal accepted - fast track',
        'ACCOUNT_PROCESS', false, null, 15);

-- ---------- Booking queue source RENEWAL (design 13) -------------------------------------------
alter table bkg_queue drop constraint if exists bkg_queue_source_check;
alter table bkg_queue add constraint ck_bkg_queue_source
    check (source in ('MANUAL', 'AUTO', 'PLACEMENT', 'UPLOAD', 'RENEWAL'));

-- ---------- New Business path: renewal link of quotations and proposal requests ----------------
alter table quo_quotation add column renewal_ref varchar(30);
alter table quo_quotation add column renewal_of_ref varchar(40);
alter table quo_quotation add constraint ck_quo_quotation_renewal
    check ((renewal_ref is null) = (renewal_of_ref is null));
create unique index uq_quo_quotation_renewal on quo_quotation (renewal_ref) where renewal_ref is not null;

alter table npk_proposal add column renewal_ref varchar(30);
alter table npk_proposal add column renewal_of_ref varchar(40);
alter table npk_proposal add constraint ck_npk_proposal_renewal
    check ((renewal_ref is null) = (renewal_of_ref is null));
create unique index uq_npk_proposal_renewal on npk_proposal (renewal_ref) where renewal_ref is not null;
