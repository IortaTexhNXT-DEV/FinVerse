-- =====================================================================================
-- iNXT BrokerVerse - V1932 Seed Employee Benefits (BRD-8) pending items (seed profile only).
-- SEED DATA ONLY - never load in production.
--   Tracked items of the V1931 programmes (BRID-030), dated from the day the seed is loaded:
--     an HMO card of a Pacific Harbor member 12 days past due with one follow-up sent (the job
--     EB_ITEM_FOLLOWUP follows it up again and escalates after EB_FOLLOWUP_MAX), a billing of the
--     group life insurer due in 5 days, and a card replacement received two days ago.
--   The member rosters, member changes and SOAs of the design follow with those screens.
-- =====================================================================================

insert into eb_tracked_item (company_id, programme_id, cycle_id, item_type, subject, member_ref,
    member_change_ref, account_arn, responsible, party_code, recipient_email, status, due_date,
    follow_ups_sent, last_follow_up_at, escalated_at, received_on, released_on, closed_on, remarks,
    created_at, created_by)
select p.company_id, p.id, null, i.item_type, i.subject, i.member_ref, null, null, i.responsible,
       i.party_code, null, i.status, current_date + i.due, i.follow_ups,
       case when i.follow_ups > 0 then now() - interval '5 days' end, null,
       case when i.status = 'RECEIVED' then current_date - 2 end, null, null, null,
       now() - interval '20 days', 'ebao'
from (values
    ('EBP-2026-000001', 'HMO_CARD', 'HMO card of Juan Dela Cruz', 'EMP-00123', 'INSURER', 'INS-MGIC', 'PENDING', -12, 1),
    ('EBP-2026-000001', 'BILLING_INVOICE', 'Billing of the group life additions', null, 'INSURER', 'INS-LAC', 'PENDING', 5, 0),
    ('EBP-2026-000005', 'CARD_REPLACEMENT', 'Replacement card of Maria Lim', 'EMP-00456', 'INSURER', 'INS-MPI', 'RECEIVED', -4, 0))
     as i(programme_no, item_type, subject, member_ref, responsible, party_code, status, due, follow_ups)
join eb_programme p on p.programme_no = i.programme_no
where not exists (select 1 from eb_tracked_item x where x.programme_id = p.id and x.subject = i.subject);
