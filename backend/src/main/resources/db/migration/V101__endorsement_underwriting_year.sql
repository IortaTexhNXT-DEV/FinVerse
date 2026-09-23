-- =====================================================================================
-- iNXT FinVerse - V101 Underwriting year of each endorsement.
-- A renewal extends the policy to a new period, which belongs to the underwriting year in
-- which it starts; endorsements made afterwards belong to that renewed period. The policy row
-- keeps the underwriting year of the original issue. Every premium transaction (policy issue
-- or endorsement) now carries its own year, read by the premium registers (PGIBR015/016 group
-- by UW year) and by reinsurance to choose the treaty programme.
-- Existing rows: a renewal takes the year of its new period; any other endorsement the year of
-- the latest approved renewal made before it, else the policy's underwriting year.
-- =====================================================================================

alter table uw_endorsement add column uw_year integer;

update uw_endorsement e
set uw_year = coalesce(
    case when e.endorsement_type = 'RENEWAL'
         then extract(year from e.new_period_from)::integer end,
    (select extract(year from r.new_period_from)::integer
     from uw_endorsement r
     where r.policy_id = e.policy_id
       and r.endorsement_type = 'RENEWAL'
       and r.status = 'APPROVED'
       and r.endorsement_no < e.endorsement_no
     order by r.endorsement_no desc
     limit 1),
    (select p.uw_year from uw_policy p where p.id = e.policy_id));

alter table uw_endorsement alter column uw_year set not null;
