-- =====================================================================================
-- iNXT BrokerVerse - V1076 Submitted Policies retention: the retention rule of the closed
--   masterlist records (SUBMITTED_POLICIES_DESIGN section 12). Record type SUBMITTED_POLICY,
--   records booked, not renewed, excluded or closed, 5 years online and 10 in the archive,
--   reviewed before any action (archive and purge are parked).
-- =====================================================================================

insert into nba_retention_rule (record_type, statuses, years_online, years_archive, action, active,
                                description, created_at, created_by)
select 'SUBMITTED_POLICY', 'BOOKED,NOT_RENEWED,EXCLUDED,CLOSED', 5, 10, 'REVIEW', true,
       'Closed submitted policies with their reviews, IAAF, TOR, letters and fees: 5 years online, 15 years in total',
       now(), 'SYSTEM'
where not exists (select 1 from nba_retention_rule where record_type = 'SUBMITTED_POLICY');
