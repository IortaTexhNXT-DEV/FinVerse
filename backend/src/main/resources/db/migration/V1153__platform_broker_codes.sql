-- =====================================================================================
-- iNXT BrokerVerse - V1153 Platform codes for the broker itself (range V1150-V1159). Stored codes
-- and columns that carried the client's name become platform codes; screens show the short name
-- of the company (client profile, V1150) where the broker is meant:
--   acc_account.payment_arrangement   VIA_BDOI    -> VIA_BROKER (premium paid through the broker)
--   eb_tracked_item.responsible       BDOI        -> BROKER
--   cmr_incentive_scheme.beneficiary  BDOI        -> BROKER
--   bcl_claim.source                  BDOI_NOTICE -> BROKER_NOTICE
--   prc_item.status                   BDOI_ONLY   -> BROKER_ONLY; columns bdoi_* -> broker_*
--   plc_billing_item.bdoi_location    -> broker_location
-- The data migration layout of accounts lists the new code. Histories keep the values recorded
-- at the time.
-- =====================================================================================

alter table acc_account drop constraint ck_acc_payment_arrangement;
update acc_account set payment_arrangement = 'VIA_BROKER' where payment_arrangement = 'VIA_BDOI';
alter table acc_account alter column payment_arrangement set default 'VIA_BROKER';
alter table acc_account add constraint ck_acc_payment_arrangement
    check (payment_arrangement in ('VIA_BROKER', 'DIRECT_TO_INSURER'));

alter table eb_tracked_item drop constraint ck_eb_tracked_item_responsible;
update eb_tracked_item set responsible = 'BROKER' where responsible = 'BDOI';
alter table eb_tracked_item add constraint ck_eb_tracked_item_responsible
    check (responsible in ('INSURER', 'CLIENT', 'BROKER'));

alter table cmr_incentive_scheme drop constraint if exists cmr_incentive_scheme_beneficiary_check;
update cmr_incentive_scheme set beneficiary = 'BROKER' where beneficiary = 'BDOI';
alter table cmr_incentive_scheme add constraint ck_cmr_incentive_scheme_beneficiary
    check (beneficiary in ('BROKER', 'BRANCH'));

alter table bcl_claim drop constraint ck_bcl_claim_source;
update bcl_claim set source = 'BROKER_NOTICE' where source = 'BDOI_NOTICE';
alter table bcl_claim add constraint ck_bcl_claim_source
    check (source in ('BROKER_NOTICE', 'INSURER_REPORTED', 'MIGRATED'));

alter table prc_item drop constraint if exists prc_item_status_check;
update prc_item set status = 'BROKER_ONLY' where status = 'BDOI_ONLY';
alter table prc_item add constraint ck_prc_item_status
    check (status in ('MATCHED', 'MATCHED_WITH_DISCREPANCY', 'BROKER_ONLY', 'UNMATCHED_PREBOOKED',
        'UNMATCHED_NO_BOOKING'));
alter table prc_item rename column bdoi_policy_no to broker_policy_no;
alter table prc_item rename column bdoi_reference_no to broker_reference_no;
alter table prc_item rename column bdoi_pn_no to broker_pn_no;
alter table prc_item rename column bdoi_period_from to broker_period_from;
alter table prc_item rename column bdoi_period_to to broker_period_to;
alter table prc_item rename column bdoi_assured_name to broker_assured_name;
alter table prc_item rename column bdoi_commission to broker_commission;
alter table prc_item rename column bdoi_basic_premium to broker_basic_premium;
alter table prc_item rename column bdoi_gross_premium to broker_gross_premium;

alter table plc_billing_item rename column bdoi_location to broker_location;

update mig_layout_column
set description = 'Premium paid through the broker or directly to the insurer',
    allowed_values = 'VIA_BROKER, DIRECT', example = 'VIA_BROKER'
where name = 'payment_arrangement' and allowed_values = 'VIA_BDOI, DIRECT';
