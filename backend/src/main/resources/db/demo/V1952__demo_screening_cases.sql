-- =====================================================================================
-- iNXT BrokerVerse - V1952 Demo Sanction Screening (BRD-10) cases and STRs (demo profile only;
-- password for all users: Brokerverse@2026). DEMO ONLY - never load in production.
--   Test data TD-SS-01 / TD-SS-06 / TD-SS-08 / TD-SS-09 of the test plan:
--     users     amlcom3, amlcom4, amlcom5 (the five AML Committee members of SCR_COMMITTEE_SIZE 5)
--               and scrdual (investigator and approver: the four-eyes refusal of FR-SS-061).
--     reasons   demo STR reason codes DEMO01-DEMO03 in SCR_STR_REASON, so that an STR can be
--               completed in the demo; the AMLC reason codes replace them when BDOI supplies the
--               format (SQ09). Production keeps the list empty until then.
--     cases     one case in every stage of SCR_CASE for the demo company FVI: NEW, INVESTIGATION
--               (with the V1951 potential match of CL-2026-000002), RETURNED (by Compliance),
--               UNIT_HEAD_APPROVAL, COMPLIANCE_REVIEW (past its SLA), AML_COMMITTEE (one vote
--               cast, four pending), STR_PREPARATION (committee APPROVE_STR), two STR_EXTRACTION
--               cases (one STR APPROVED, one EXTRACTED) and CLOSED (false positive).
--   Clients are the demo clients of V981; the configuration versions those of V1950.
-- =====================================================================================

-- ---------- Users (TD-SS-01, TD-SS-08) --------------------------------------------------------
insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('amlcom3', 'Arturo AML Committee', 'amlcom3@brokerverse-demo.ph'),
             ('amlcom4', 'Aurora AML Committee', 'amlcom4@brokerverse-demo.ph'),
             ('amlcom5', 'Anselmo AML Committee', 'amlcom5@brokerverse-demo.ph'),
             ('scrdual', 'Sofia Investigator-Approver', 'scrdual@brokerverse-demo.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('amlcom3', 'AML_COMMITTEE'), ('amlcom4', 'AML_COMMITTEE'), ('amlcom5', 'AML_COMMITTEE'),
             ('scrdual', 'SCR_INVESTIGATOR'), ('scrdual', 'SCR_APPROVER'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

-- ---------- Demo STR reasons (SQ09: the AMLC codes replace them) --------------------------------
insert into lov_value (type_code, code, label, sort_order, parent_code, effective_from, record_status,
                       authorized_by, authorized_at, created_at, created_by)
select v.type_code, v.code, v.label, v.sort_order, null, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(),
       'SYSTEM'
from (values
    ('SCR_STR_REASON', 'DEMO01', 'Demo: transaction with a listed person (to be replaced, SQ09)', 10),
    ('SCR_STR_REASON', 'DEMO02', 'Demo: no apparent economic purpose (to be replaced, SQ09)', 20),
    ('SCR_STR_REASON', 'DEMO03', 'Demo: amount not commensurate with the profile (to be replaced, SQ09)', 30)
) as v(type_code, code, label, sort_order)
where not exists (select 1 from lov_value x where x.type_code = v.type_code and x.code = v.code);

-- ---------- Cases in every stage (TD-SS-06) ---------------------------------------------------
-- d.hours_ago: stage entry; d.due_hours: due time from now (negative = past its SLA).
insert into scr_case (company_id, case_no, client_id, client_code, client_name, client_type,
    trigger_code, trigger_reference, case_type, risk_category, template_type, active_policy,
    marketing_unit, unit_head, account_officer, stage, status, stage_entered_at, assignee, investigator,
    returned_from, returned_by, round_no, committee_round, disposition, investigator_disposition,
    recommendation, str_required,
    committee_decision, committee_decided_at, due_at, reminder_lead_hours, remind_at, escalate_to_role,
    match_version_id, risk_version_id, approval_version_id, assignment_version_id, sla_version_id,
    validation_version_id, closed_at, created_at, created_by)
select co.id, d.case_no, c.id, coalesce(c.client_code, c.prospect_code), c.display_name, c.client_type,
       d.trigger_code, coalesce(c.client_code, c.prospect_code), d.case_type, d.category, d.template, d.active,
       d.unit, 'clxuh', 'ao', d.stage, case when d.stage = 'CLOSED' then 'CLOSED' else 'OPEN' end,
       now() - make_interval(hours => d.hours_ago), d.assignee, d.investigator, d.returned_from,
       d.returned_by, d.round_no, d.committee_round, d.disposition,
       case when d.stage in ('NEW', 'INVESTIGATION') then null when d.stage = 'CLOSED' then 'FALSE_POSITIVE'
            else 'TRUE_MATCH_REVIEW' end,
       d.recommendation, d.str_required,
       d.committee_decision,
       case when d.committee_decision is null then null else now() - make_interval(hours => d.hours_ago) end,
       case when d.due_hours is null then null else now() + make_interval(hours => d.due_hours) end,
       d.lead,
       case when d.due_hours is null then null else now() + make_interval(hours => d.due_hours - d.lead) end,
       d.escalate,
       (select v.id from scr_config_version v where v.company_id = co.id and v.config_type = 'MATCH_CRITERIA' and v.version_no = 1),
       (select v.id from scr_config_version v where v.company_id = co.id and v.config_type = 'RISK_RULES' and v.version_no = 1),
       (select v.id from scr_config_version v where v.company_id = co.id and v.config_type = 'APPROVAL_MATRIX' and v.version_no = 1),
       (select v.id from scr_config_version v where v.company_id = co.id and v.config_type = 'ASSIGNMENT_MATRIX' and v.version_no = 1),
       (select v.id from scr_config_version v where v.company_id = co.id and v.config_type = 'SLA_MATRIX' and v.version_no = 1),
       (select v.id from scr_config_version v where v.company_id = co.id and v.config_type = 'VALIDATION_RULES' and v.version_no = 1),
       case when d.stage = 'CLOSED' then now() - make_interval(hours => d.hours_ago) end,
       now() - make_interval(hours => d.hours_ago + 48), 'SYSTEM'
from org_company co
cross join (values
    ('SCR-2026-000001', 'PR-2026-000007', 'CLIENT_REGISTERED', 'NAME_MATCH', 'HIGH_SANCTION', 'KYC_REVIEW', false, 'CBG-NCR', 'NEW', 2, null, null, null, null, 1, 0, null, null, false, null, null, null, null),
    ('SCR-2026-000002', 'CL-2026-000002', 'PERIODIC', 'NAME_MATCH', 'HIGH_SANCTION', 'KYC_REVIEW', true, 'CBG-NCR', 'INVESTIGATION', 20, 'investigator', null, null, null, 1, 0, null, null, false, null, 52, 24, 'COMPLIANCE_OFFICER'),
    ('SCR-2026-000003', 'CL-2026-000005', 'CLIENT_CHANGED', 'HIGH_RISK', 'HIGH_SANCTION', 'KYC_REVIEW', true, 'CBG-NCR', 'RETURNED', 6, 'investigator', 'investigator', 'COMPLIANCE_REVIEW', 'compoff', 2, 0, 'TRUE_MATCH_REVIEW', 'Same person as the internal watchlist entry; escalate for enhanced review.', false, null, 18, 8, 'COMPLIANCE_OFFICER'),
    ('SCR-2026-000004', 'CL-2026-000003', 'ACCOUNT_SUBMITTED', 'NAME_MATCH', 'HIGH_SANCTION', 'KYC_REVIEW', true, 'CORP-NCR', 'UNIT_HEAD_APPROVAL', 4, 'scrapprover', 'investigator', null, null, 1, 0, 'TRUE_MATCH_REVIEW', 'Company name matches a sanctioned entity alias; directors to be verified.', false, null, 20, 8, 'COMPLIANCE_OFFICER'),
    ('SCR-2026-000005', 'CL-2026-000004', 'PERIODIC', 'HIGH_RISK', 'HIGH_SANCTION', 'KYC_REVIEW', true, 'CORP-NCR', 'COMPLIANCE_REVIEW', 30, null, 'investigator', null, null, 1, 0, 'CONCUR', 'Adverse media found on the majority shareholder; recommend escalation.', true, null, -6, 8, 'COMPLIANCE_CHECKER'),
    ('SCR-2026-000006', 'CL-2026-000006', 'CLIENT_REGISTERED', 'PEP', 'PEP', 'EDD', true, 'CBG-NCR', 'AML_COMMITTEE', 10, null, 'investigator', null, null, 1, 1, 'ESCALATE_COMMITTEE', 'Politically exposed; source of wealth not documented.', true, null, 62, 24, 'COMPLIANCE_OFFICER'),
    ('SCR-2026-000007', 'PR-2026-000008', 'LIST_CHANGE', 'NAME_MATCH', 'HIGH_SANCTION', 'KYC_REVIEW', false, 'CBG-NCR', 'STR_PREPARATION', 3, null, 'investigator', null, null, 1, 1, 'APPROVE_STR', 'Premium paid in cash by a third party linked to the listed entity.', true, 'APPROVE_STR', 45, 12, 'COMPLIANCE_CHECKER'),
    ('SCR-2026-000008', 'PR-2026-000010', 'LIST_CHANGE', 'HIGH_RISK', 'HIGH_SANCTION', 'KYC_REVIEW', false, 'CBG-NCR', 'STR_EXTRACTION', 26, null, 'investigator', null, null, 1, 1, 'APPROVE_STR', 'Partnership funds routed through an unrelated account.', true, 'APPROVE_STR', 94, 24, 'COMPLIANCE_CHECKER'),
    ('SCR-2026-000009', 'PR-2026-000011', 'PERIODIC', 'NAME_MATCH', 'HIGH_SANCTION', 'KYC_REVIEW', false, 'CBG-NCR', 'STR_EXTRACTION', 50, null, 'investigator', null, null, 1, 1, 'APPROVE_STR', 'Name and birth date match an internal watchlist entry.', true, 'APPROVE_STR', 70, 24, 'COMPLIANCE_CHECKER'),
    ('SCR-2026-000010', 'PR-2026-000009', 'CLIENT_REGISTERED', 'NAME_MATCH', 'HIGH_SANCTION', 'KYC_REVIEW', false, 'CBG-NCR', 'CLOSED', 100, null, 'investigator2', null, null, 1, 0, 'FALSE_POSITIVE', 'Different birth date and nationality on the valid ID.', false, null, null, null, null)
) as d(case_no, client_ref, trigger_code, case_type, category, template, active, unit, stage, hours_ago,
       assignee, investigator, returned_from, returned_by, round_no, committee_round, disposition,
       recommendation, str_required, committee_decision, due_hours, lead, escalate)
join crm_client c on c.company_id = co.id and (c.prospect_code = d.client_ref or c.client_code = d.client_ref)
where co.code = 'FVI';

-- Work items of workflow SCR_CASE, one per case, in the case's stage.
insert into wf_case (company_id, workflow_code, entity_type, entity_id, reference, title, link,
    originating_unit, stage_code, stage_entered_at, due_at, assignee, closed, created_at, created_by)
select k.company_id, 'SCR_CASE', 'ScreeningCase', k.id::text, k.case_no, k.client_name || ' - ' || k.case_type,
       '/screening/cases/' || k.id, k.marketing_unit, k.stage, k.stage_entered_at, k.due_at, k.assignee,
       k.stage = 'CLOSED', k.created_at, 'SYSTEM'
from scr_case k
where k.case_no like 'SCR-2026-0000%' and k.created_by = 'SYSTEM'
  and not exists (select 1 from wf_case w where w.entity_type = 'ScreeningCase' and w.entity_id = k.id::text);

update scr_case k set work_case_id = w.id
from wf_case w
where w.entity_type = 'ScreeningCase' and w.entity_id = k.id::text and k.work_case_id is null;

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor, automatic,
    occurred_at)
select w.id, null, 'NEW', 'start', null, null, 'SYSTEM', true, k.created_at
from wf_case w join scr_case k on k.work_case_id = w.id
where w.entity_type = 'ScreeningCase' and k.created_by = 'SYSTEM';

insert into wf_case_history (case_id, from_stage, to_stage, action, reason_code, comment, actor, automatic,
    occurred_at)
select w.id, 'NEW', k.stage, 'demo', null, 'Demo case in ' || k.stage, 'SYSTEM', true, k.stage_entered_at
from wf_case w join scr_case k on k.work_case_id = w.id
where w.entity_type = 'ScreeningCase' and k.created_by = 'SYSTEM' and k.stage <> 'NEW';

-- The potential match of CL-2026-000002 recorded by V1951 belongs to its investigation case.
update scr_match m set case_id = k.id
from scr_case k
where k.case_no = 'SCR-2026-000002' and m.client_id = k.client_id and m.case_id is null;

-- ---------- Timeline (insert-only) -----------------------------------------------------------
insert into scr_case_event (case_id, event, round_no, from_stage, to_stage, from_value, to_value,
    reason_code, remarks, actor, occurred_at, created_at, created_by)
select k.id, 'CREATED', 1, null, null, null, null, null, 'Opened by ' || k.trigger_code || ' (demo)', 'SYSTEM',
       k.created_at, now(), 'SYSTEM'
from scr_case k where k.created_by = 'SYSTEM' and k.case_no like 'SCR-2026-0000%';

insert into scr_case_event (case_id, event, round_no, from_stage, to_stage, from_value, to_value,
    reason_code, remarks, actor, occurred_at, created_at, created_by)
select k.id, 'ASSIGNED', 1, null, null, null, coalesce(k.investigator, 'queue'), null,
       'Scenario 20: team SCR_INVESTIGATOR (LEAST_OPEN)', 'SYSTEM', k.created_at + interval '1 minute', now(),
       'SYSTEM'
from scr_case k where k.created_by = 'SYSTEM' and k.case_no like 'SCR-2026-0000%' and k.stage <> 'NEW';

insert into scr_case_event (case_id, event, round_no, from_stage, to_stage, from_value, to_value,
    reason_code, remarks, actor, occurred_at, created_at, created_by)
select k.id, 'SUBMITTED', 1, 'INVESTIGATION', 'UNIT_HEAD_APPROVAL', null, null,
       case when k.stage = 'CLOSED' then 'FALSE_POSITIVE' else 'TRUE_MATCH_REVIEW' end, k.recommendation,
       k.investigator, k.created_at + interval '1 day', now(), 'SYSTEM'
from scr_case k
where k.created_by = 'SYSTEM' and k.case_no like 'SCR-2026-0000%'
  and k.stage not in ('NEW', 'INVESTIGATION');

insert into scr_case_event (case_id, event, round_no, from_stage, to_stage, from_value, to_value,
    reason_code, remarks, actor, occurred_at, created_at, created_by)
select k.id, 'RETURNED', 1, 'COMPLIANCE_REVIEW', 'RETURNED', null, null, 'INCOMPLETE_DETAILS',
       'Add the source of funds and the latest valid ID.', 'compoff', k.stage_entered_at, now(), 'SYSTEM'
from scr_case k where k.case_no = 'SCR-2026-000003' and k.created_by = 'SYSTEM';

insert into scr_case_event (case_id, event, round_no, from_stage, to_stage, from_value, to_value,
    reason_code, remarks, actor, occurred_at, created_at, created_by)
select k.id, 'COMMITTEE_VOTE', 1, null, null, null, 'APPROVE_STR', 'APPROVE_STR',
       'The cash payments by the third party support an STR.', 'amlcom1', k.stage_entered_at + interval '2 hours',
       now(), 'SYSTEM'
from scr_case k where k.case_no = 'SCR-2026-000006' and k.created_by = 'SYSTEM';

-- ---------- Committee votes: one cast on the committee case, three on each approved case ----------
insert into scr_committee_vote (case_id, round_no, member, decision, remarks, voted_at, created_at, created_by)
select k.id, 1, v.member, 'APPROVE_STR', v.remarks, k.stage_entered_at - make_interval(hours => v.ord),
       now(), 'SYSTEM'
from scr_case k
cross join (values (1, 'amlcom1', 'Supports the STR recommendation.'),
                   (2, 'amlcom2', 'Agree; file the STR.'),
                   (3, 'amlcom3', 'Agree with Compliance.')) as v(ord, member, remarks)
where k.created_by = 'SYSTEM' and k.case_no in ('SCR-2026-000007', 'SCR-2026-000008', 'SCR-2026-000009');

insert into scr_committee_vote (case_id, round_no, member, decision, remarks, voted_at, created_at, created_by)
select k.id, 1, 'amlcom1', 'APPROVE_STR', 'The cash payments by the third party support an STR.',
       k.stage_entered_at + interval '2 hours', now(), 'SYSTEM'
from scr_case k where k.case_no = 'SCR-2026-000006' and k.created_by = 'SYSTEM';

-- ---------- STRs: one APPROVED (not yet extracted), one EXTRACTED (TD-SS-09) ----------------------
insert into scr_str_extraction (company_id, batch_no, period_from, period_to, layout_version_id, str_count,
    file_name, sha256, report_run_id, re_extraction, reason, extracted_by, extracted_at, created_at, created_by)
select co.id, 'STRX-2026-000001', date_trunc('month', now())::date, now()::date,
       (select v.id from scr_config_version v where v.company_id = co.id and v.config_type = 'STR_LAYOUT' and v.version_no = 1),
       1, 'STRX-2026-000001.csv', repeat('0', 64), null, false, null, 'compoff', now() - interval '1 day', now(),
       'SYSTEM'
from org_company co where co.code = 'FVI';

insert into scr_str (company_id, case_id, str_no, template_version_id, subject_code, subject_name,
    subject_snapshot, reason_codes, status, committee_decided_at, ready_by, ready_at, extraction_id,
    extracted_at, created_at, created_by)
select k.company_id, k.id, s.str_no,
       (select v.id from scr_config_version v where v.company_id = k.company_id and v.config_type = 'TEMPLATE' and v.scope = 'STR' and v.version_no = 1),
       k.client_code, k.client_name, 'Name: ' || k.client_name || E'\nCode: ' || k.client_code, s.reasons, s.status,
       k.committee_decided_at, 'compoff', k.stage_entered_at,
       case when s.status = 'EXTRACTED' then (select x.id from scr_str_extraction x where x.batch_no = 'STRX-2026-000001') end,
       case when s.status = 'EXTRACTED' then now() - interval '1 day' end,
       k.stage_entered_at - interval '1 hour', 'compoff'
from scr_case k
join (values ('SCR-2026-000008', 'STR-2026-000001', 'DEMO02', 'APPROVED'),
             ('SCR-2026-000009', 'STR-2026-000002', 'DEMO01', 'EXTRACTED')) as s(case_no, str_no, reasons, status)
  on s.case_no = k.case_no
where k.created_by = 'SYSTEM';

insert into scr_str_extraction_item (extraction_id, str_id)
select x.id, s.id from scr_str_extraction x join scr_str s on s.extraction_id = x.id
where x.batch_no = 'STRX-2026-000001';

insert into scr_str_field (str_id, field_code, value_text, created_at, created_by)
select s.id, f.code, case f.code when 'SUBJECT_NAME' then s.subject_name
                                 when 'NARRATIVE' then k.recommendation
                                 when 'TOTAL_AMOUNT' then '125000.00' end, now(), 'SYSTEM'
from scr_str s join scr_case k on k.id = s.case_id
cross join (values ('SUBJECT_NAME'), ('NARRATIVE'), ('TOTAL_AMOUNT')) as f(code)
where s.str_no in ('STR-2026-000001', 'STR-2026-000002');

insert into scr_str_transaction (str_id, reference, txn_date, amount, currency, txn_type, description,
    created_at, created_by)
select s.id, 'OR-DEMO-' || right(s.str_no, 3), (now() - interval '20 days')::date, 125000.00, 'PHP', 'RECEIPT',
       'Premium paid in cash (demo)', now(), 'SYSTEM'
from scr_str s where s.str_no in ('STR-2026-000001', 'STR-2026-000002');

-- ---------- Number series continue after the demo numbers ------------------------------------
insert into document_sequence (sequence_key, next_value)
values ('SCR-2026', 101), ('STR-2026', 101), ('STRX-2026', 101)
on conflict (sequence_key) do update set next_value = greatest(document_sequence.next_value, 101);
