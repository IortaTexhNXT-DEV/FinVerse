-- =====================================================================================
-- iNXT BrokerVerse - V851 Placement: hold cover re-assignment (contract change of Submitted
-- Policies wave S0, in the placement range V850-V859).
--   Requirements: docs/requirements/BDOI_SP_BRD_SPEC.md BRIDSP-32 (update the assigned insurer
--                 while a hold cover request is open).
--   Design: docs/architecture/SUBMITTED_POLICIES_DESIGN.md sections 3.5 and 9 (placement row).
--   A hold cover request closed because the insurer was re-assigned has the status REASSIGNED, on
--   the hold cover and on the account's hold cover summary.
-- =====================================================================================

alter table plc_hold_cover drop constraint if exists ck_plc_hold_cover_status;
alter table plc_hold_cover add constraint ck_plc_hold_cover_status
    check (status in ('REQUESTED', 'CONFIRMED', 'DECLINED', 'EXPIRED', 'REASSIGNED'));

alter table acc_account drop constraint if exists ck_acc_hold_cover;
alter table acc_account add constraint ck_acc_hold_cover
    check (hold_cover_status is null
        or hold_cover_status in ('REQUESTED', 'CONFIRMED', 'DECLINED', 'EXPIRED', 'REASSIGNED'));
