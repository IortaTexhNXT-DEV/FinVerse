-- =====================================================================================
-- iNXT BrokerVerse - V301 A cession carries the underwriting year of the premium transaction it
-- cedes (V101: a renewal and the later endorsements of the renewed period belong to the year
-- the renewed period starts). Cessions made before V101 carry the policy's original year for
-- renewals and their endorsements; align them, so the RI registers group them by the right
-- year. treaty_year is left untouched: it records the programme that was actually applied.
-- =====================================================================================

update ri_cession c
set uw_year = e.uw_year
from uw_endorsement e
where e.policy_id = c.policy_id
  and e.endorsement_no = c.endorsement_no
  and c.uw_year <> e.uw_year;
