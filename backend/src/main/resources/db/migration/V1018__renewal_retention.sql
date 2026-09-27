-- =====================================================================================
-- iNXT BrokerVerse - V1018 Renewal (BRD-6) retention: the retention rule of the closed renewals.
--   RENEWAL_DESIGN section 13 (nbadmin): record type RENEWAL_CANDIDATE, renewals renewed or closed,
--   5 years online and 10 in the archive, reviewed before any action (archive and purge are parked).
-- =====================================================================================

insert into nba_retention_rule (record_type, statuses, years_online, years_archive, action, active,
                                description, created_at, created_by)
select 'RENEWAL_CANDIDATE', 'RENEWED,CLOSED', 5, 10, 'REVIEW', true,
       'Closed renewals with their checks, dispositions, letters and acceptances: 5 years online, 15 years in total',
       now(), 'SYSTEM'
where not exists (select 1 from nba_retention_rule where record_type = 'RENEWAL_CANDIDATE');
