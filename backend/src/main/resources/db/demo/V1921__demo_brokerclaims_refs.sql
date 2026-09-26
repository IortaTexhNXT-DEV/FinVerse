-- =====================================================================================
-- iNXT BrokerVerse - V1921 Demo Claims Handling (BRD-7), wave CL1-A: insurer location references
-- of the demo property covers (demo profile only).
--   BRCLM.042 insurer location references per location of a cover and insurer, with effective
--   dates: the booking demo property covers (V988) get the reference their insurer uses; one of
--   them has a superseded reference so the history shows on Cover Lookup. The demo claims are
--   recorded at start-up by brokerclaims.demo.BrokerClaimsDemoData through the services.
--   Design: docs/architecture/CLAIMS_BROKING_DESIGN.md sections 4 and 5.2.
-- =====================================================================================

insert into bcl_location_ref (company_id, account_id, arn, account_item_no, location_key, insurer_code,
                              insurer_location_ref, effective_from, effective_to, created_at, created_by)
select a.company_id, a.id, a.arn, i.item_no, i.location_key, r.insurer, r.reference, r.effective_from,
       r.effective_to, now(), 'clmofficer'
from (values
    ('ARN-2026-940002', 'INS-MGIC', 'MGIC-LOC-0088', date '2026-09-01', date '2026-09-14'),
    ('ARN-2026-940002', 'INS-MGIC', 'MGIC-LOC-0091', date '2026-09-15', null::date),
    ('ARN-2026-940006', 'INS-LAC',  'LAC-FI-LOC-12', date '2026-09-20', null::date),
    ('ARN-2026-940007', 'INS-MGIC', 'MGIC-LOC-0107', date '2026-09-01', null::date)
) as r(arn, insurer, reference, effective_from, effective_to)
join acc_account a on a.arn = r.arn
join acc_risk_item i on i.account_id = a.id and i.item_no = 1 and i.kind = 'PROPERTY_LOCATION'
where not exists (select 1 from bcl_location_ref x
                  where x.arn = r.arn and x.insurer_code = r.insurer and x.insurer_location_ref = r.reference);
