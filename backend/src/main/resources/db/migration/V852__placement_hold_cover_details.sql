-- =====================================================================================
-- iNXT BrokerVerse - V852 Placement: hold cover requested from a renewal (Renewal BRD,
-- Walkthrough addendum BRRN.042 and R37-HC-01 to 04; FR-RN-086), in the placement range.
--   plc_hold_cover  duration (30 or 60 days), the expiring policy number, remarks and the
--                   requester of the request; the conditions of the insurer's confirmation; a
--                   requested or confirmed hold cover can be cancelled with a reason (CANCELLED),
--                   after which a new request is allowed
-- =====================================================================================
alter table plc_hold_cover add column duration_days integer;
alter table plc_hold_cover add column expiring_policy_no varchar(60);
alter table plc_hold_cover add column remarks varchar(200);
alter table plc_hold_cover add column requested_by varchar(50);
alter table plc_hold_cover add column conditions varchar(500);
alter table plc_hold_cover add column cancel_reason varchar(200);

alter table plc_hold_cover drop constraint if exists ck_plc_hold_cover_status;
alter table plc_hold_cover add constraint ck_plc_hold_cover_status
    check (status in ('REQUESTED', 'CONFIRMED', 'DECLINED', 'EXPIRED', 'REASSIGNED', 'CANCELLED'));

alter table acc_account drop constraint if exists ck_acc_hold_cover;
alter table acc_account add constraint ck_acc_hold_cover
    check (hold_cover_status is null
        or hold_cover_status in ('REQUESTED', 'CONFIRMED', 'DECLINED', 'EXPIRED', 'REASSIGNED',
            'CANCELLED'));
