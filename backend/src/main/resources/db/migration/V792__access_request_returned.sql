-- =====================================================================================
-- iNXT BrokerVerse - V792 Access requests can be returned (BRD-5 BASAU 2.4.1, 2.6.x, wave A1-GL).
-- The approver returns a request with remarks; the requester edits and resubmits it (PENDING
-- again). The requester is notified of approved, rejected and returned requests.
-- (V791 is the role-permission change request of Product Maintenance.)
-- =====================================================================================
alter table nba_access_request drop constraint ck_nba_access_status;
alter table nba_access_request add constraint ck_nba_access_status
    check (status in ('PENDING', 'APPROVED', 'REJECTED', 'RETURNED'));
alter table nba_access_request add column returned_count integer not null default 0;
