-- =====================================================================================
-- iNXT BrokerVerse - V1950 Demo Sanction Screening (BRD-10) users and configuration (demo profile
-- only; password for all users: Brokerverse@2026). DEMO ONLY - never load in production.
--   Users: design section 6.2 and test data TD-SS-01 (compoff, compchk, ucc, investigator,
--   investigator2, scrapprover, amlcom1, amlcom2) plus compdual, a combined maker and checker that
--   demonstrates the four-eyes refusals (FR-SS-019, FR-SS-023). The roles and grants are in V1050.
--   Configuration: one ACTIVE version of every type for the demo company FVI (test data TD-SS-02):
--   matching thresholds, three risk categories, approval routes with a Corbank route, assignment
--   with a Retail PEP scenario, SLA matrix, validation rules requiring the KYC form, the KYC /
--   transaction / EDD / STR templates and a placeholder STR layout (AMLC format parked, SQ09), and
--   one PENDING SLA version made by compoff for compchk's My Approvals. All values are demo values
--   to be replaced by Compliance before go-live (SQ02-SQ06, SQ08).
--   Watchlist entries and screening runs come with V1951 (S1-B), cases with V1952 (S1-C).
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('compoff',       'Carmela Compliance Officer',   'compoff@brokerverse-demo.ph'),
             ('compchk',       'Cesar Compliance Checker',     'compchk@brokerverse-demo.ph'),
             ('compdual',      'Dolores Compliance Maker-Checker', 'compdual@brokerverse-demo.ph'),
             ('ucc',           'Ursula Unit Compliance Coordinator', 'ucc@brokerverse-demo.ph'),
             ('investigator',  'Ivan Investigator',            'investigator@brokerverse-demo.ph'),
             ('investigator2', 'Isabel Investigator',          'investigator2@brokerverse-demo.ph'),
             ('scrapprover',   'Samuel Screening Approver',    'scrapprover@brokerverse-demo.ph'),
             ('amlcom1',       'Amparo AML Committee',         'amlcom1@brokerverse-demo.ph'),
             ('amlcom2',       'Alfredo AML Committee',        'amlcom2@brokerverse-demo.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('compoff', 'COMPLIANCE_OFFICER'), ('compchk', 'COMPLIANCE_CHECKER'),
             ('compdual', 'COMPLIANCE_OFFICER'), ('compdual', 'COMPLIANCE_CHECKER'),
             ('ucc', 'UNIT_COMPLIANCE_COORD'), ('investigator', 'SCR_INVESTIGATOR'),
             ('investigator2', 'SCR_INVESTIGATOR'), ('scrapprover', 'SCR_APPROVER'),
             ('amlcom1', 'AML_COMMITTEE'), ('amlcom2', 'AML_COMMITTEE'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

-- ---------- Version 1 of every configuration type, ACTIVE since 2026-01-01 ----------------------
insert into scr_config_version (company_id, config_type, scope, version_no, status, effective_from,
    change_note, submitted_by, submitted_at, decided_by, decided_at, created_at, created_by)
select c.id, t.config_type, t.scope, 1, 'ACTIVE', date '2026-01-01',
       'Initial demo configuration (values to be confirmed by Compliance)', 'compoff',
       timestamptz '2025-12-15 09:00:00+08', 'compchk', timestamptz '2025-12-16 10:00:00+08', now(),
       'compoff'
from org_company c
cross join (values ('MATCH_CRITERIA', ''), ('RISK_RULES', ''), ('APPROVAL_MATRIX', ''),
                   ('ASSIGNMENT_MATRIX', ''), ('SLA_MATRIX', ''), ('VALIDATION_RULES', ''),
                   ('TEMPLATE', 'KYC_REVIEW'), ('TEMPLATE', 'TRANSACTION_REVIEW'), ('TEMPLATE', 'EDD'),
                   ('TEMPLATE', 'STR'), ('STR_LAYOUT', '')) as t(config_type, scope)
where c.code = 'FVI';

-- Matching criteria (SNSRP-101; SQ02)
insert into scr_match_rule (version_id, sort_order, list_type, subject_type, algorithm, threshold,
    match_fields, min_score_for_case, created_at, created_by)
select v.id, m.ord, m.list_type, m.subject, m.algorithm, m.threshold, m.fields, m.case_score, now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    (10, 'SANCTION', 'INDIVIDUAL', 'EXACT', 1.0000, 'ALIAS,BIRTH_DATE,NAME', 1.0000),
    (20, 'SANCTION', 'INDIVIDUAL', 'PHONETIC', 0.8500, 'ALIAS,BIRTH_DATE,NAME', 0.9000),
    (30, 'SANCTION', 'INDIVIDUAL', 'FUZZY', 0.8500, 'ALIAS,BIRTH_DATE,NAME', 0.9000),
    (40, 'SANCTION', 'ENTITY', 'EXACT', 1.0000, 'ALIAS,NAME', 1.0000),
    (50, 'SANCTION', 'ENTITY', 'FUZZY', 0.8800, 'ALIAS,NAME', 0.9200),
    (60, 'PEP', 'INDIVIDUAL', 'EXACT', 1.0000, 'ALIAS,BIRTH_DATE,NAME', 1.0000),
    (70, 'PEP', 'INDIVIDUAL', 'PHONETIC', 0.8500, 'ALIAS,BIRTH_DATE,NAME', 0.9000),
    (80, 'PEP', 'INDIVIDUAL', 'FUZZY', 0.8800, 'ALIAS,BIRTH_DATE,NAME', 0.9200),
    (90, 'PEP', 'ENTITY', 'FUZZY', 0.9000, 'ALIAS,NAME', 0.9400),
    (100, 'INTERNAL', 'INDIVIDUAL', 'EXACT', 1.0000, 'ALIAS,NAME', 1.0000),
    (110, 'INTERNAL', 'INDIVIDUAL', 'FUZZY', 0.8800, 'ALIAS,NAME', 0.9200),
    (120, 'INTERNAL', 'ENTITY', 'FUZZY', 0.9000, 'ALIAS,NAME', 0.9400),
    (130, 'ADVERSE_MEDIA', 'INDIVIDUAL', 'FUZZY', 0.9000, 'NAME', 0.9500)
) as m(ord, list_type, subject, algorithm, threshold, fields, case_score)
where v.config_type = 'MATCH_CRITERIA' and v.version_no = 1;

-- Risk categories and rules (SNSRP-102; SQ03)
insert into scr_risk_category (version_id, code, name, tier, kyc_risk_rating, tags, case_type,
    requires_edd, created_at, created_by)
select v.id, k.code, k.name, k.tier, k.rating, k.tags, k.case_type, k.edd, now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    ('HIGH_SANCTION', 'High risk - sanctions list', 1, 'HIGH', 'WATCHLIST_REVIEW', 'NAME_MATCH', false),
    ('PEP', 'Politically exposed person', 2, 'HIGH', 'PEP', 'PEP', true),
    ('STANDARD', 'Standard risk', 3, 'STANDARD', '', null, false)
) as k(code, name, tier, rating, tags, case_type, edd)
where v.config_type = 'RISK_RULES' and v.version_no = 1;

insert into scr_risk_rule (version_id, priority, category_code, condition_attr, operator, rule_values,
    created_at, created_by)
select v.id, r.priority, r.category, r.attr, r.op, r.vals, now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    (10, 'HIGH_SANCTION', 'MATCH_LIST_TYPE', 'EQ', 'SANCTION'),
    (20, 'PEP', 'PEP', 'EQ', 'TRUE'),
    (30, 'PEP', 'MATCH_LIST_TYPE', 'EQ', 'PEP'),
    (40, 'HIGH_SANCTION', 'MATCH_LIST_TYPE', 'IN', 'ADVERSE_MEDIA,INTERNAL')
) as r(priority, category, attr, op, vals)
where v.config_type = 'RISK_RULES' and v.version_no = 1;

-- Approval and escalation matrix (SNSRP-103, 703; SQ04). Units: CORP-NCR = Corbank, CBG-NCR = Retail.
insert into scr_approval_route (version_id, sort_order, from_stage, case_type, risk_category,
    marketing_unit, disposition, to_stage, approver_kind, approver_value, created_at, created_by)
select v.id, a.ord, a.from_stage, a.case_type, a.risk, a.unit, a.disposition, a.to_stage, a.kind,
       a.approver, now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    (10, 'INVESTIGATION', null, null, null, 'FALSE_POSITIVE', 'CLOSED', null, null),
    (20, 'INVESTIGATION', 'HIGH_RISK', null, 'CORP-NCR', null, 'UNIT_HEAD_APPROVAL', 'USER', 'scrapprover'),
    (30, 'INVESTIGATION', null, null, null, null, 'UNIT_HEAD_APPROVAL', 'UNIT_HEAD', null),
    (40, 'UNIT_HEAD_APPROVAL', null, null, null, 'CONCUR', 'COMPLIANCE_REVIEW', 'ROLE', 'COMPLIANCE_OFFICER'),
    (50, 'UNIT_HEAD_APPROVAL', null, null, null, 'NOT_CONCUR', 'RETURNED', null, null),
    (60, 'COMPLIANCE_REVIEW', null, null, null, 'ESCALATE_COMMITTEE', 'AML_COMMITTEE', 'ROLE', 'AML_COMMITTEE'),
    (70, 'COMPLIANCE_REVIEW', null, null, null, 'FOR_STR', 'STR_PREPARATION', 'ROLE', 'COMPLIANCE_OFFICER'),
    (80, 'COMPLIANCE_REVIEW', null, null, null, 'CLOSE_NO_ACTION', 'CLOSED', null, null),
    (90, 'COMPLIANCE_REVIEW', null, null, null, 'RETURN', 'RETURNED', null, null)
) as a(ord, from_stage, case_type, risk, unit, disposition, to_stage, kind, approver)
where v.config_type = 'APPROVAL_MATRIX' and v.version_no = 1;

-- Case assignment matrix (SNSRP-106; SQ04)
insert into scr_assignment_rule (version_id, sort_order, case_type, trigger_code, risk_category,
    marketing_unit, client_type, team_role, user_name, balancing, created_at, created_by)
select v.id, s.ord, s.case_type, null, null, s.unit, null, 'SCR_INVESTIGATOR', null, s.balancing, now(),
       'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    (10, 'PEP', 'CBG-NCR', 'ROUND_ROBIN'),
    (20, null, null, 'LEAST_OPEN')
) as s(ord, case_type, unit, balancing)
where v.config_type = 'ASSIGNMENT_MATRIX' and v.version_no = 1;

-- SLA matrix (SNSRP-108; SQ08)
insert into scr_sla_rule (version_id, stage, case_type, risk_category, sla_hours, reminder_lead_hours,
    escalate_to_role, calendar, created_at, created_by)
select v.id, s.stage, s.case_type, null, s.hours, s.lead, s.escalate, 'CALENDAR', now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    ('INVESTIGATION', null, 72, 24, 'COMPLIANCE_OFFICER'),
    ('INVESTIGATION', 'PEP', 24, 8, 'COMPLIANCE_OFFICER'),
    ('RETURNED', null, 24, 8, 'COMPLIANCE_OFFICER'),
    ('UNIT_HEAD_APPROVAL', null, 24, 8, 'COMPLIANCE_OFFICER'),
    ('COMPLIANCE_REVIEW', null, 24, 8, 'COMPLIANCE_CHECKER'),
    ('AML_COMMITTEE', null, 72, 24, 'COMPLIANCE_OFFICER'),
    ('STR_PREPARATION', null, 48, 12, 'COMPLIANCE_CHECKER'),
    ('STR_EXTRACTION', null, 120, 24, 'COMPLIANCE_CHECKER')
) as s(stage, case_type, hours, lead, escalate)
where v.config_type = 'SLA_MATRIX' and v.version_no = 1;

-- Validation rules (SNSRP-701, 802; SQ06)
insert into scr_validation_rule (version_id, stage, case_type, rule_kind, parameters, blocking,
    created_at, created_by)
select v.id, r.stage, null, r.kind, r.params, r.blocking, now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    ('INVESTIGATION', 'TEMPLATE_COMPLETE', null, true),
    ('INVESTIGATION', 'DOCUMENT_TYPES_PRESENT', 'KYC_FORM', true),
    ('INVESTIGATION', 'DISPOSITION_ALLOWED', null, true),
    ('INVESTIGATION', 'RECOMMENDATION_PRESENT', null, true),
    ('COMPLIANCE_REVIEW', 'STR_FLAG_CONSISTENT', null, false)
) as r(stage, kind, params, blocking)
where v.config_type = 'VALIDATION_RULES' and v.version_no = 1;

-- Templates (SNSRP-104, 105, 501; field lists to be supplied by BDOI, SQ05)
insert into scr_template (version_id, template_type, name, created_at, created_by)
select v.id, v.scope, t.name, now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
join (values ('KYC_REVIEW', 'KYC review (demo)'), ('TRANSACTION_REVIEW', 'Transaction review (demo)'),
             ('EDD', 'Enhanced due diligence (demo)'), ('STR', 'Suspicious transaction report (demo)'))
     as t(scope, name) on t.scope = v.scope
where v.config_type = 'TEMPLATE' and v.version_no = 1;

insert into scr_template_field (template_id, section, code, label, data_type, lov_type, mandatory,
    help_text, sort_order, prefill_source, created_at, created_by)
select t.id, f.section, f.code, f.label, f.data_type, f.lov_type, f.mandatory, f.help, f.ord, f.prefill,
       now(), 'compoff'
from scr_template t
join scr_config_version v on v.id = t.version_id
join org_company c on c.id = v.company_id and c.code = 'FVI'
join (values
    ('KYC_REVIEW', 'Client profile', 'IDENTITY_VERIFIED', 'Identity verified against valid ID', 'CHECKBOX', null, true, null, 10, null),
    ('KYC_REVIEW', 'Client profile', 'OCCUPATION', 'Occupation / nature of business', 'TEXT', null, false, null, 20, null),
    ('KYC_REVIEW', 'Client profile', 'SOURCE_OF_FUNDS', 'Source of funds', 'LONG_TEXT', null, true, 'Where the premium money comes from', 30, null),
    ('KYC_REVIEW', 'Findings', 'MATCH_ASSESSMENT', 'Assessment of the name match', 'LONG_TEXT', null, true, 'Compare birth date, nationality and IDs with the list entry', 40, null),
    ('KYC_REVIEW', 'Findings', 'PROPOSED_RATING', 'Proposed risk rating', 'LOV', 'KYC_RISK_RATING', true, null, 50, null),
    ('KYC_REVIEW', 'Findings', 'RECOMMENDATION', 'Recommendation', 'LONG_TEXT', null, true, null, 60, null),
    ('TRANSACTION_REVIEW', 'Transaction', 'TRANSACTION_REFERENCE', 'Invoice / receipt / policy reference', 'TEXT', null, true, null, 10, null),
    ('TRANSACTION_REVIEW', 'Transaction', 'TRANSACTION_AMOUNT', 'Amount', 'AMOUNT', null, true, null, 20, null),
    ('TRANSACTION_REVIEW', 'Transaction', 'TRANSACTION_DATE', 'Transaction date', 'DATE', null, false, null, 30, null),
    ('TRANSACTION_REVIEW', 'Findings', 'UNUSUAL_PATTERN', 'Unusual pattern observed', 'LONG_TEXT', null, true, null, 40, null),
    ('TRANSACTION_REVIEW', 'Findings', 'RECOMMENDATION', 'Recommendation', 'LONG_TEXT', null, true, null, 50, null),
    ('EDD', 'Wealth', 'SOURCE_OF_WEALTH', 'Source of wealth', 'LONG_TEXT', null, true, null, 10, null),
    ('EDD', 'Wealth', 'SOURCE_OF_FUNDS', 'Source of funds', 'LONG_TEXT', null, true, null, 20, null),
    ('EDD', 'Approval', 'SENIOR_APPROVAL', 'Senior management approval obtained', 'CHECKBOX', null, true, null, 30, null),
    ('EDD', 'Approval', 'SUPPORTING_DOCUMENT', 'Supporting document', 'ATTACHMENT', null, false, null, 40, null),
    ('EDD', 'Approval', 'RECOMMENDATION', 'Recommendation', 'LONG_TEXT', null, true, null, 50, null),
    ('STR', 'Subject', 'SUBJECT_NAME', 'Subject name', 'TEXT', null, true, null, 10, 'CLIENT_NAME'),
    ('STR', 'Subject', 'SUBJECT_TIN', 'Subject TIN', 'TEXT', null, false, null, 20, 'CLIENT_TIN'),
    ('STR', 'Subject', 'SUBJECT_ADDRESS', 'Subject address', 'TEXT', null, false, null, 30, 'CLIENT_ADDRESS'),
    ('STR', 'Report', 'REASON_CODE', 'Reason for suspicion', 'LOV', 'SCR_STR_REASON', false, 'AMLC reason codes to be supplied (SQ09)', 40, null),
    ('STR', 'Report', 'TOTAL_AMOUNT', 'Total amount involved', 'AMOUNT', null, false, null, 50, 'TRANSACTION_TOTAL'),
    ('STR', 'Report', 'NARRATIVE', 'Narrative', 'LONG_TEXT', null, true, null, 60, 'CASE_RECOMMENDATION')
) as f(scope, section, code, label, data_type, lov_type, mandatory, help, ord, prefill)
  on f.scope = t.template_type
where v.version_no = 1;

-- STR extraction layout: placeholder with the case fields until the AMLC format is supplied (SQ09)
insert into scr_str_layout (version_id, format, delimiter, encoding, created_at, created_by)
select v.id, 'CSV', ',', 'UTF-8', now(), 'compoff'
from scr_config_version v
join org_company c on c.id = v.company_id and c.code = 'FVI'
where v.config_type = 'STR_LAYOUT' and v.version_no = 1;

insert into scr_str_layout_column (layout_id, sort_order, field_code, fixed_value, header, length, pad,
    code_map, created_at, created_by)
select l.id, k.ord, k.field, k.fixed, k.header, null, null, '', now(), 'compoff'
from scr_str_layout l
join scr_config_version v on v.id = l.version_id
join org_company c on c.id = v.company_id and c.code = 'FVI'
cross join (values
    (1, 'STR_NO', null, 'STR No.'),
    (2, 'SUBJECT_NAME', null, 'Subject Name'),
    (3, 'TOTAL_AMOUNT', null, 'Amount'),
    (4, 'REASON_CODE', null, 'Reason Code'),
    (5, null, 'BDOI', 'Reporting Entity')
) as k(ord, field, fixed, header)
where v.version_no = 1;

-- ---------- One PENDING version for compchk's My Approvals (TD-SS-02) --------------------------
insert into scr_config_version (company_id, config_type, scope, version_no, status, effective_from,
    change_note, base_version_id, submitted_by, submitted_at, diff, created_at, created_by)
select c.id, 'SLA_MATRIX', '', 2, 'PENDING', date '2026-10-01',
       'Shorter investigation SLA (demo proposal)', v1.id, 'compoff', now(),
       '[{"item":"SLA INVESTIGATION / * / *","attribute":"SLA hours","before":"72","after":"48"},'
           || '{"item":"SLA INVESTIGATION / * / *","attribute":"Reminder lead hours","before":"24","after":"16"}]',
       now(), 'compoff'
from org_company c
join scr_config_version v1 on v1.company_id = c.id and v1.config_type = 'SLA_MATRIX' and v1.version_no = 1
where c.code = 'FVI';

insert into scr_sla_rule (version_id, stage, case_type, risk_category, sla_hours, reminder_lead_hours,
    escalate_to_role, calendar, created_at, created_by)
select v2.id, r.stage, r.case_type, r.risk_category,
       case when r.stage = 'INVESTIGATION' and r.case_type is null then 48 else r.sla_hours end,
       case when r.stage = 'INVESTIGATION' and r.case_type is null then 16 else r.reminder_lead_hours end,
       r.escalate_to_role, r.calendar, now(), 'compoff'
from scr_config_version v2
join scr_sla_rule r on r.version_id = v2.base_version_id
where v2.config_type = 'SLA_MATRIX' and v2.version_no = 2 and v2.status = 'PENDING';
