-- =====================================================================================
-- iNXT BrokerVerse - V1931 Seed Employee Benefits (BRD-8) programmes and cycles (seed profile only).
-- SEED DATA ONLY - never load in production.
--   Six programmes of the V981 corporate clients and prospects, dated from the day the seed is
--   loaded so the storyline stays current (EMPLOYEE_BENEFITS_DESIGN 3, E2 storyline, first part):
--     EBP-2026-000001 Pacific Harbor Logistics, HMO + GLI: renewal advice sent 20 days ago, waiting
--                     for feedback (cycle EBC-2026-000001 in RA_SENT)
--     EBP-2026-000002 Luzon Agri-Industrial, HMO: expires in 120 days, renewal advice due
--     EBP-2026-000003 Bayside Builders (prospect), HMO + GPA: new business in REQUIREMENTS
--     EBP-2026-000004 Metro Dental Clinic Partners (prospect), GPA: new business lost on price
--     EBP-2026-000005 Pacific Harbor Logistics, voluntary GPA: feedback received by e-mail, cycle in
--                     REQUIREMENTS
--     EBP-2026-000006 Luzon Agri-Industrial, GLI: not flagged for renewal (the renewal advice job
--                     raises EB_RA_NOT_SENT for it)
--   Every open cycle has its EB_CYCLE work case. Number series EBP-2026 and EBC-2026 continue at 101.
-- =====================================================================================

insert into eb_programme (company_id, programme_no, client_id, client_code, client_name, name, team_code,
    funding, account_officer, sales_unit, renewal_eligible, status, created_at, created_by)
select k.id, p.programme_no, c.id, coalesce(c.client_code, c.prospect_code), c.display_name, p.name,
       p.team_code, p.funding, p.ao, null, p.eligible, p.status, now() - interval '30 days', p.ao
from (values
    ('EBP-2026-000001', 'CL-2026-000003', 'Pacific Harbor Group Health and Life', 'BDO', 'EMPLOYER', 'ebao', true, 'ACTIVE'),
    ('EBP-2026-000002', 'CL-2026-000004', 'Luzon Agri-Industrial Health Plan', 'SM', 'EMPLOYER', 'ebao', true, 'ACTIVE'),
    ('EBP-2026-000003', 'PR-2026-000008', 'Bayside Builders Employee Benefits', 'NEW_BUSINESS', 'EMPLOYER', 'ebao2', false, 'PROSPECT'),
    ('EBP-2026-000004', 'PR-2026-000010', 'Metro Dental Voluntary Accident Cover', 'SOLICITED', 'VOLUNTARY', 'ebao2', false, 'LOST'),
    ('EBP-2026-000005', 'CL-2026-000003', 'Pacific Harbor Voluntary Personal Accident', 'VOLUNTARY', 'VOLUNTARY', 'ebao', true, 'ACTIVE'),
    ('EBP-2026-000006', 'CL-2026-000004', 'Luzon Agri-Industrial Group Life', 'SM', 'EMPLOYER', 'ebao2', false, 'ACTIVE'))
     as p(programme_no, code, name, team_code, funding, ao, eligible, status)
join crm_client c on c.client_code = p.code or (c.client_code is null and c.prospect_code = p.code)
join org_company k on k.id = c.company_id
where not exists (select 1 from eb_programme x where x.programme_no = p.programme_no);

insert into eb_programme_line (programme_id, line_no, benefit_line, product_code, incumbent_insurer,
    current_policy_no, current_arn, period_from, period_to, headcount, active, created_at, created_by)
select p.id, l.line_no, l.benefit_line, null, l.insurer, l.policy_no, null,
       case when l.ends is null then null else current_date + l.ends - 365 end,
       case when l.ends is null then null else current_date + l.ends end,
       l.headcount, true, now() - interval '30 days', 'SYSTEM'
from (values
    ('EBP-2026-000001', 1, 'HMO', 'INS-MGIC', 'HMO-2025-0101', 100, 240),
    ('EBP-2026-000001', 2, 'GLI', 'INS-LAC', 'GLI-2025-0102', 100, 240),
    ('EBP-2026-000002', 1, 'HMO', 'INS-VMI', 'HMO-2025-0201', 120, 410),
    ('EBP-2026-000003', 1, 'HMO', null, null, null, 85),
    ('EBP-2026-000003', 2, 'GPA', null, null, null, 85),
    ('EBP-2026-000004', 1, 'GPA', null, null, null, 32),
    ('EBP-2026-000005', 1, 'GPA', 'INS-MPI', 'GPA-2025-0501', 60, 120),
    ('EBP-2026-000006', 1, 'GLI', 'INS-LAC', 'GLI-2025-0601', 90, 410))
     as l(programme_no, line_no, benefit_line, insurer, policy_no, ends, headcount)
join eb_programme p on p.programme_no = l.programme_no
where not exists (select 1 from eb_programme_line x where x.programme_id = p.id and x.line_no = l.line_no);

insert into eb_programme_contact (programme_id, name, email, mobile, role, receives_ra, receives_soa,
    active, created_at, created_by)
select p.id, t.name, t.email, t.mobile, t.role, t.ra, t.soa, true, now() - interval '30 days', 'SYSTEM'
from (values
    ('EBP-2026-000001', 'Liza Marquez', 'hr.head@pacificharbor.example', '+639171230001', 'HR_HEAD', true, false),
    ('EBP-2026-000001', 'Ramon Tolentino', 'finance@pacificharbor.example', '+639171230002', 'FINANCE', false, true),
    ('EBP-2026-000002', 'Grace Villamor', 'hr@luzonagri.example', '+639171230003', 'HR_HEAD', true, true),
    ('EBP-2026-000003', 'Arnel Bautista', 'hr@baysidebuilders.example', '+639171230004', 'HR_OFFICER', true, false),
    ('EBP-2026-000004', 'Dr. Celia Ramos', 'admin@metrodental.example', '+639171230005', 'HR_HEAD', true, false),
    ('EBP-2026-000005', 'Liza Marquez', 'hr.head@pacificharbor.example', '+639171230001', 'HR_HEAD', true, false),
    ('EBP-2026-000006', 'Grace Villamor', 'hr@luzonagri.example', '+639171230003', 'HR_HEAD', true, true))
     as t(programme_no, name, email, mobile, role, ra, soa)
join eb_programme p on p.programme_no = t.programme_no
where not exists (select 1 from eb_programme_contact x where x.programme_id = p.id and x.email = t.email);

-- ---------- Cycles -------------------------------------------------------------------------
insert into eb_cycle (company_id, cycle_no, programme_id, business_type, policy_year, target_inception,
    remarketing, stage, outcome, outcome_reason, outcome_remarks, closed_at, created_at, created_by)
select p.company_id, y.cycle_no, p.id, y.business_type,
       extract(year from current_date + y.inception)::int, current_date + y.inception, false, y.stage,
       y.outcome, y.reason, null,
       case when y.outcome is null then null else now() - interval '5 days' end,
       now() - make_interval(days => y.age), y.creator
from (values
    ('EBC-2026-000001', 'EBP-2026-000001', 'RENEWAL', 100, 'RA_SENT', null, null, 20, 'SYSTEM'),
    ('EBC-2026-000002', 'EBP-2026-000003', 'NEW_BUSINESS', 75, 'REQUIREMENTS', null, null, 14, 'ebao2'),
    ('EBC-2026-000003', 'EBP-2026-000004', 'NEW_BUSINESS', 45, 'CLOSED_LOST', 'LOST', 'PRICE', 25, 'ebao2'),
    ('EBC-2026-000004', 'EBP-2026-000005', 'RENEWAL', 60, 'REQUIREMENTS', null, null, 40, 'SYSTEM'))
     as y(cycle_no, programme_no, business_type, inception, stage, outcome, reason, age, creator)
join eb_programme p on p.programme_no = y.programme_no
where not exists (select 1 from eb_cycle x where x.cycle_no = y.cycle_no);

insert into wf_case (company_id, workflow_code, entity_type, entity_id, reference, title, link,
    originating_unit, stage_code, stage_entered_at, due_at, assignee, closed, created_at, created_by)
select c.company_id, 'EB_CYCLE', 'EbCycle', c.id::text, c.cycle_no,
       case c.business_type when 'RENEWAL' then 'Renewal ' else 'New business ' end
           || c.policy_year || ' - ' || p.name,
       '/eb/programmes/' || p.id, p.team_code, c.stage, c.created_at + interval '1 day', null,
       p.account_officer, c.closed_at is not null, c.created_at, 'SYSTEM'
from eb_cycle c join eb_programme p on p.id = c.programme_id
where c.cycle_no like 'EBC-2026-00000%'
  and not exists (select 1 from wf_case w where w.entity_type = 'EbCycle' and w.entity_id = c.id::text);

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor, automatic,
    occurred_at)
select w.id, null, 'OPEN', 'start', null, null, 'SYSTEM', true, c.created_at
from wf_case w join eb_cycle c on w.entity_type = 'EbCycle' and w.entity_id = c.id::text
where c.cycle_no like 'EBC-2026-00000%'
  and not exists (select 1 from wf_case_history h where h.case_id = w.id);

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor, automatic,
    occurred_at)
select w.id, 'OPEN', c.stage, 'seed', c.outcome_reason, 'Seed cycle in ' || c.stage, 'SYSTEM', true,
       c.created_at + interval '1 day'
from wf_case w join eb_cycle c on w.entity_type = 'EbCycle' and w.entity_id = c.id::text
where c.cycle_no like 'EBC-2026-00000%'
  and (select count(*) from wf_case_history h where h.case_id = w.id) = 1;

-- ---------- Renewal advice, feedback and activity log -------------------------------------
insert into eb_renewal_advice (company_id, programme_id, cycle_id, expiry_date, sent_at, sent_by, trigger_kind,
    recipients, message_id, attachment_id, reminders_sent, last_reminder_at, feedback_at, created_at, created_by)
select c.company_id, c.programme_id, c.id, c.target_inception, c.created_at + interval '1 day', 'SYSTEM', 'JOB',
       r.recipients, null, null, 0, null, r.feedback, c.created_at + interval '1 day', 'SYSTEM'
from (values ('EBC-2026-000001', 'hr.head@pacificharbor.example', null::timestamptz),
             ('EBC-2026-000004', 'hr.head@pacificharbor.example', now() - interval '5 days'))
     as r(cycle_no, recipients, feedback)
join eb_cycle c on c.cycle_no = r.cycle_no
where not exists (select 1 from eb_renewal_advice x where x.cycle_id = c.id);

insert into eb_feedback (company_id, programme_id, cycle_id, channel, received_on, feedback_text, file_count,
    created_at, created_by)
select c.company_id, c.programme_id, c.id, 'EMAIL', current_date - 5,
       'The client renews the voluntary cover and asks the incumbent to keep the current benefits; updated master list to follow.',
       0, now() - interval '5 days', 'ebao'
from eb_cycle c
where c.cycle_no = 'EBC-2026-000004'
  and not exists (select 1 from eb_feedback x where x.cycle_id = c.id);

insert into eb_activity_log (company_id, programme_id, cycle_id, activity_code, reference, received_at,
    released_at, actor, remarks, created_at, created_by)
select c.company_id, c.programme_id, c.id, 'RENEWAL_ADVICE', c.cycle_no, a.sent_at, a.sent_at, 'SYSTEM',
       'Sent to ' || a.recipients, a.sent_at, 'SYSTEM'
from eb_renewal_advice a join eb_cycle c on c.id = a.cycle_id
where c.cycle_no like 'EBC-2026-00000%'
  and not exists (select 1 from eb_activity_log x where x.cycle_id = c.id and x.activity_code = 'RENEWAL_ADVICE');

insert into document_sequence (sequence_key, next_value) values ('EBP-2026', 101), ('EBC-2026', 101)
on conflict (sequence_key) do update set next_value = greatest(document_sequence.next_value, 101);
