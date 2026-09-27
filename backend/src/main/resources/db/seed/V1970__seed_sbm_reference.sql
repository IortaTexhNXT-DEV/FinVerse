-- =====================================================================================
-- iNXT BrokerVerse - V1970 Seed Submitted Policies (BRD-12) reference data (seed profile only;
-- password for all users: Brokerverse@2026). SEED DATA ONLY - never load in production.
--   * Users of the personas (FRS BRD-12 section 3): sbmhandler (Submitted Handler, CBG Motor),
--     firehandler (Submitted Handler, CBG Fire), sbmchecker (Submitted Checker), sanitation
--     (Sanitation Handler), sbmtl (Team Lead), polreview (Policy Review Officer) and upphandler
--     (Handling Fee Handler). The Account Officers ao and ao2, the Marketing TL mkttl and the TSU
--     user tsu are the New Business seed users; badmin receives the rule administration role.
--   * User scopes: the CBG handlers see their segment; ao and ao2 see their own records.
--   * GL accounts 4115 Handling Fee Income and 1236 Service Fee Receivable - Insurers with the
--     rules of the events SBM_HANDLING_FEE and SBM_NO_TOUCH_FEE.
--   * Active rule sets of the four processing steps, insurer limits, insurer assignment rules,
--     letter rules and the IAAF / TOR approval matrix, all authorised.
--   * A LAMD loan snapshot of the seed PNs of V1971's runner.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select b.id from org_branch b join org_company c on c.id = b.company_id
        where c.code = 'FVI' and b.code = 'HO'),
       now(), 'SYSTEM'
from (values ('sbmhandler',  'Susana Motor Handler',        'sbmhandler@brokerverse-seed.ph'),
             ('firehandler', 'Fernando Fire Handler',       'firehandler@brokerverse-seed.ph'),
             ('sbmchecker',  'Carmela Submitted Checker',   'sbmchecker@brokerverse-seed.ph'),
             ('sanitation',  'Samuel Sanitation Handler',   'sanitation@brokerverse-seed.ph'),
             ('sbmtl',       'Teodora Submitted Team Lead', 'sbmtl@brokerverse-seed.ph'),
             ('polreview',   'Rodrigo Policy Reviewer',     'polreview@brokerverse-seed.ph'),
             ('upphandler',  'Ursula Unapplied Handler',    'upphandler@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('sbmhandler', 'SBM_HANDLER'), ('firehandler', 'SBM_HANDLER'), ('sbmchecker', 'SBM_CHECKER'),
             ('sanitation', 'SBM_SANITATION'), ('sbmtl', 'SBM_TL'), ('polreview', 'SBM_POLICY_REVIEWER'),
             ('upphandler', 'SBM_UPP_HANDLER'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

insert into sbm_user_scope (company_id, username, segments, own_records_only, created_at, created_by)
select c.id, s.username, s.segments, s.own, now(), 'SYSTEM'
from org_company c, (values ('sbmhandler', 'CBG_MOTOR', false), ('firehandler', 'CBG_FIRE', false),
                            ('ao', null, true), ('ao2', null, true))
     as s(username, segments, own)
where c.code = 'FVI'
  and not exists (select 1 from sbm_user_scope x where x.company_id = c.id and x.username = s.username);

-- ---------- GL accounts and accounting rules ------------------------------------------------------
insert into coa_account (company_id, code, name, account_class, level, parent_id, category_id, postable,
    control_account, sub_ledger_type, allow_manual_posting, cost_center_required, business_line_required,
    revaluation_required, reconcilable, inter_branch, report_group, opened_on, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, g.code, g.name, g.cls, 'SUB',
       (select p.id from coa_account p where p.company_id = c.id and p.code = g.parent),
       (select id from coa_category where code = g.category), true, g.control, g.sub_ledger,
       false, false, false, false, false, false, g.report_group, date '2026-01-01', 'ACTIVE',
       'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('4115', 'Handling Fee Income', 'INCOME', '4000', 'PREM', false, 'NONE', 'Service Fee Income'),
    ('1236', 'Service Fee Receivable - Insurers', 'ASSET', '1200', 'RECV', true, 'INSURER',
     'Insurance Receivables')
) as g(code, name, cls, parent, category, control, sub_ledger, report_group)
where c.code = 'FVI'
  and not exists (select 1 from coa_account a where a.company_id = c.id and a.code = g.code);

insert into acc_rule (company_id, event_type, name, priority, effective_from, record_status,
    authorized_by, authorized_at, created_at, created_by)
select c.id, e.code, e.name, 100, date '2020-01-01', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
cross join (values
    ('SBM_HANDLING_FEE', 'Handling fee recognised from unapplied collections (seed)'),
    ('SBM_NO_TOUCH_FEE', 'No Touch service fee billed to the insurer (seed)')
) as e(code, name)
where c.code = 'FVI'
  and not exists (select 1 from acc_rule r where r.company_id = c.id and r.event_type = e.code);

insert into acc_rule_line (rule_id, line_no, side, account_code, amount_component, party_line, narration)
select r.id, l.n, l.side, l.acc, l.comp, l.party, l.narr
from acc_rule r
join org_company c on c.id = r.company_id and c.code = 'FVI'
join (values
  ('SBM_HANDLING_FEE', 1, 'DEBIT', '2205', 'AMOUNT', true, 'Unapplied collections recognised'),
  ('SBM_HANDLING_FEE', 2, 'CREDIT', '4115', 'INCOME', false, 'Handling fee income'),
  ('SBM_HANDLING_FEE', 3, 'CREDIT', '2504', 'OUTPUT_VAT', false, 'Output VAT on the handling fee'),
  ('SBM_NO_TOUCH_FEE', 1, 'DEBIT', '1236', 'RECEIVABLE', true, 'Service fee receivable from the insurer'),
  ('SBM_NO_TOUCH_FEE', 2, 'CREDIT', '4115', 'INCOME', false, 'No Touch service fee income'),
  ('SBM_NO_TOUCH_FEE', 3, 'CREDIT', '2504', 'OUTPUT_VAT', false, 'Output VAT on the service fee')
) as l(event, n, side, acc, comp, party, narr) on l.event = r.event_type
where not exists (select 1 from acc_rule_line x where x.rule_id = r.id);

-- ---------- Rule sets of the processing steps ------------------------------------------------------
insert into sbm_rule_set (company_id, code, step, segment, business_type, version_no, status, effective_from,
    description, submitted_by, submitted_at, approved_by, approved_at, created_at, created_by)
select c.id, s.code, s.step, null, null, 1, 'ACTIVE', date '2026-01-01', s.descr, 'badmin', now(), 'mkttl',
       now(), now(), 'badmin'
from org_company c, (values
    ('SANITATION', 'SANITATION', 'Completeness and duplicates of the received policies'),
    ('MATCHING', 'MATCHING', 'Match with the LAMD loan snapshot'),
    ('CLASSIFICATION', 'CLASSIFICATION', 'In-force or submitted classification'),
    ('DISPOSITION', 'DISPOSITION', 'Renewal disposition buckets')
) as s(code, step, descr)
where c.code = 'FVI'
  and not exists (select 1 from sbm_rule_set x where x.company_id = c.id and x.code = s.code);

create temporary table sbm_seed_rule (
    set_code varchar(40), priority integer, name varchar(120), bucket varchar(40), tag varchar(20),
    classification varchar(20), template varchar(20), flag varchar(20), reason varchar(40),
    field varchar(40), operator varchar(10), value varchar(200), field2 varchar(40), operator2 varchar(10),
    value2 varchar(200)
) on commit drop;

insert into sbm_seed_rule values
    ('SANITATION', 100, 'PN number missing (CBG)', null, null, null, null, 'FALLOUT', 'MISSING_PN',
     'pnMissing', 'EQ', 'true', 'segment', 'IN', 'CBG_MOTOR,CBG_FIRE'),
    ('SANITATION', 90, 'Same PN as another open policy', null, null, null, null, 'FALLOUT', 'DUPLICATE_PN',
     'duplicatePn', 'EQ', 'true', null, null, null),
    ('SANITATION', 80, 'Same serial or motor number', null, null, null, null, 'FALLOUT', 'DUPLICATE_UNIT',
     'duplicateUnit', 'EQ', 'true', null, null, null),
    ('SANITATION', 1, 'Complete policy', null, null, null, null, null, null,
     'always', 'EQ', 'true', null, null, null),
    ('MATCHING', 100, 'No LAMD record of the CBG loan', null, null, null, null, 'FALLOUT', 'NO_LAMD_RECORD',
     'lamdFound', 'EQ', 'false', 'segment', 'IN', 'CBG_MOTOR,CBG_FIRE'),
    ('MATCHING', 90, 'Remedial loan', 'RMU', null, null, null, null, 'LOAN_NOT_ACTIVE',
     'loanStatus', 'EQ', 'REMEDIAL', null, null, null),
    ('MATCHING', 80, 'Fully paid loan', 'FULLY_PAID', null, null, null, null, 'LOAN_NOT_ACTIVE',
     'loanStatus', 'EQ', 'FULLY_PAID', null, null, null),
    ('MATCHING', 1, 'Loan matched or not required', null, null, null, null, null, null,
     'always', 'EQ', 'true', null, null, null),
    ('CLASSIFICATION', 100, 'Renewal business is in force', null, null, 'INFORCED', null, null, null,
     'businessType', 'EQ', 'RB', null, null, null),
    ('CLASSIFICATION', 1, 'New business is submitted', null, null, 'SUBMITTED', null, null, null,
     'always', 'EQ', 'true', null, null, null),
    ('DISPOSITION', 100, 'Free First Year', 'FFY', null, null, 'FFY', null, null,
     'ffy', 'EQ', 'true', null, null, null),
    ('DISPOSITION', 95, 'Group employee account', 'EMPLOYEE', null, null, null, null, 'EXCLUDED_LIST',
     'employeeAccount', 'EQ', 'true', null, null, null),
    ('DISPOSITION', 90, 'No Touch account', 'NO_TOUCH', null, null, null, null, 'EXCLUDED_LIST',
     'noTouch', 'EQ', 'true', null, null, null),
    ('DISPOSITION', 80, 'Corporate policy with documents to review', 'FOR_REVIEW', null, null, null, 'REVIEW',
     'FOR_REVIEW', 'segment', 'EQ', 'NONCBG_CORPORATE', 'hasDocuments', 'EQ', 'true'),
    ('DISPOSITION', 70, 'Large sum insured decided by the handler', 'FOR_MANUAL_DISPOSITION', null, null, null,
     null, 'MANUAL_DECISION', 'sumInsured', 'GT', '20000000', null, null, null),
    ('DISPOSITION', 1, 'Renew with the generic template', 'FOR_RENEWAL', null, null, 'GENERIC', null, null,
     'always', 'EQ', 'true', null, null, null);

insert into sbm_rule (rule_set_id, priority, name, outcome_bucket, outcome_tag, outcome_classification,
    outcome_ra_template, outcome_flag, reason_code, stop, active, created_at, created_by)
select s.id, r.priority, r.name, r.bucket, r.tag, r.classification, r.template, r.flag, r.reason, true, true,
       now(), 'badmin'
from sbm_seed_rule r
join sbm_rule_set s on s.code = r.set_code and s.version_no = 1
join org_company c on c.id = s.company_id and c.code = 'FVI'
where not exists (select 1 from sbm_rule x where x.rule_set_id = s.id and x.priority = r.priority);

insert into sbm_rule_condition (rule_id, seq, field, operator, value)
select x.id, 0, r.field, r.operator, r.value
from sbm_seed_rule r
join sbm_rule_set s on s.code = r.set_code and s.version_no = 1
join org_company c on c.id = s.company_id and c.code = 'FVI'
join sbm_rule x on x.rule_set_id = s.id and x.priority = r.priority
where not exists (select 1 from sbm_rule_condition y where y.rule_id = x.id and y.seq = 0);

insert into sbm_rule_condition (rule_id, seq, field, operator, value)
select x.id, 1, r.field2, r.operator2, r.value2
from sbm_seed_rule r
join sbm_rule_set s on s.code = r.set_code and s.version_no = 1
join org_company c on c.id = s.company_id and c.code = 'FVI'
join sbm_rule x on x.rule_set_id = s.id and x.priority = r.priority
where r.field2 is not null
  and not exists (select 1 from sbm_rule_condition y where y.rule_id = x.id and y.seq = 1);

-- ---------- Insurer limits, insurer assignment and letters ------------------------------------------
insert into sbm_limit_rule (company_id, insurer_code, segment, line, max_sum_insured, max_vehicle_age,
    attribute, attribute_limit, description, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, l.insurer, l.segment, null, l.max_si, l.max_age, null, null, l.descr, 'ACTIVE', 'mkttl', now(),
       now(), 'badmin'
from org_company c, (values
    ('INS-MGIC', 'CBG_MOTOR', 5000000.00, 10, 'Motor: up to PHP 5M and vehicles up to 10 years old'),
    ('INS-LAC', 'CBG_MOTOR', 3000000.00, 7, 'Motor: up to PHP 3M and vehicles up to 7 years old'),
    ('INS-VMI', 'CBG_FIRE', 30000000.00, null, 'Fire: up to PHP 30M per location'),
    ('INS-MPI', 'NONCBG_CORPORATE', 100000000.00, null, 'Corporate: up to PHP 100M')
) as l(insurer, segment, max_si, max_age, descr)
where c.code = 'FVI'
  and not exists (select 1 from sbm_limit_rule x where x.company_id = c.id and x.insurer_code = l.insurer
                  and x.segment = l.segment);

insert into sbm_insurer_rule (company_id, segment, vehicle_type, occupancy, insurer_code, priority,
    exclude_expiring, description, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, r.segment, r.vehicle, r.occupancy, r.insurer, r.priority, r.exclude, r.descr, 'ACTIVE', 'mkttl',
       now(), now(), 'badmin'
from org_company c, (values
    ('CBG_MOTOR', 'Truck', null, 'INS-LAC', 30, false, 'Trucks go to Luzon Assurance'),
    ('CBG_MOTOR', null, null, 'INS-MGIC', 20, true, 'Motor renewals to Mabuhay General unless it is the expiring insurer'),
    ('CBG_MOTOR', null, null, 'INS-LAC', 10, false, 'Motor fallback insurer'),
    ('CBG_FIRE', null, 'Residential', 'INS-VMI', 20, false, 'Residential fire to Visayas Mutual'),
    ('CBG_FIRE', null, null, 'INS-MPI', 10, false, 'Other fire risks to Mindanao Pacific'),
    ('NONCBG_CORPORATE', null, null, 'INS-MPI', 10, false, 'Corporate renewals to Mindanao Pacific'),
    ('NONCBG_RETAIL', null, null, 'INS-MGIC', 10, false, 'Retail renewals to Mabuhay General')
) as r(segment, vehicle, occupancy, insurer, priority, exclude, descr)
where c.code = 'FVI'
  and not exists (select 1 from sbm_insurer_rule x where x.company_id = c.id and x.description = r.descr);

insert into sbm_letter_rule (company_id, letter_type, segment, bucket, status, days_from_expiry, channel,
    template_code, description, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, l.type, l.segment, null, l.status, l.days, l.channel, l.template, l.descr, 'ACTIVE', 'mkttl', now(),
       now(), 'badmin'
from org_company c, (values
    ('REMINDER', null, 'FOR_RENEWAL', 60, 'EMAIL', 'SBM_REMINDER', 'E-mail reminder 60 days before expiry'),
    ('RENEWAL_NOTICE', 'CBG_MOTOR', 'RENEWAL_IN_PROGRESS', 45, 'PRINT', 'SBM_RENEWAL_NOTICE',
     'Printed renewal notice of motor policies 45 days before expiry'),
    ('RENEWAL_PROPOSAL', 'NONCBG_CORPORATE', 'RENEWAL_IN_PROGRESS', 30, 'BANK_COUNTERPART',
     'SBM_RENEWAL_PROPOSAL', 'Renewal proposal to the bank counterpart 30 days before expiry')
) as l(type, segment, status, days, channel, template, descr)
where c.code = 'FVI'
  and not exists (select 1 from sbm_letter_rule x where x.company_id = c.id and x.description = l.descr);

insert into sbm_approval_matrix (company_id, document, segment, tsi_from, tsi_to, level, permission,
    approver_username, signatory_title, record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, m.doc, null, m.tsi_from, m.tsi_to, m.level, m.permission, m.approver, m.title, 'ACTIVE', 'mkttl',
       now(), now(), 'badmin'
from org_company c, (values
    ('IAAF', 0.00, null::numeric, 1, 'IAAF_APPROVE', 'sbmchecker', 'Submitted Policies Checker'),
    ('IAAF', 20000000.00, null, 2, 'IAAF_APPROVE', 'mkttl', 'Head, Marketing'),
    ('TOR', 0.00, null, 1, 'TOR_APPROVE', 'tsu', 'Technical Services Head')
) as m(doc, tsi_from, tsi_to, level, permission, approver, title)
where c.code = 'FVI'
  and not exists (select 1 from sbm_approval_matrix x where x.company_id = c.id and x.document = m.doc
                  and x.level = m.level);

-- ---------- LAMD loan snapshot (PNs of the seed policies) --------------------------------------------
insert into sbm_lamd_loan (company_id, snapshot_date, pn_no, borrower_name, loan_status, amortised,
    maturity_date, originating_unit, balance, serial_no, motor_no, created_at, created_by)
select c.id, current_date - 3, l.pn, l.borrower, l.status, l.amortised, l.maturity, l.unit, l.balance,
       null, null, now(), 'SYSTEM'
from org_company c, (values
    ('PN-MTR-26001', 'Andres Villanueva', 'ACTIVE', true, date '2029-03-31', 'Auto Loans Makati', 812000.00),
    ('PN-MTR-26002', 'Bea Soriano', 'ACTIVE', true, date '2028-11-30', 'Auto Loans Ortigas', 560000.00),
    ('PN-MTR-26003', 'Carlo Mendoza', 'FULLY_PAID', false, date '2026-08-31', 'Auto Loans Cebu', 0.00),
    ('PN-MTR-26004', 'Dina Ramos', 'REMEDIAL', true, date '2028-05-31', 'Auto Loans Davao', 430000.00),
    ('PN-MTR-26005', 'Emilio Santos', 'ACTIVE', true, date '2030-01-31', 'Auto Loans Makati', 1250000.00),
    ('PN-MTR-26006', 'Faye Aquino', 'ACTIVE', true, date '2029-07-31', 'Auto Loans Pasig', 690000.00),
    ('PN-MTR-26007', 'Gabriel Cruz', 'ACTIVE', true, date '2027-12-31', 'Auto Loans Makati', 210000.00),
    ('PN-MTR-26008', 'Hazel Lim', 'ACTIVE', true, date '2029-09-30', 'Auto Loans Quezon City', 740000.00),
    ('PN-MTR-26009', 'Ivan Dela Cruz', 'ACTIVE', true, date '2030-06-30', 'Auto Loans Makati', 1900000.00),
    ('PN-MTR-26010', 'Jasmine Reyes', 'OPEN_MARKET', false, date '2026-06-30', 'Auto Loans Cebu', 0.00),
    ('PN-FIR-26001', 'Kristine Bautista', 'ACTIVE', true, date '2045-12-31', 'Home Loans Makati', 5200000.00),
    ('PN-FIR-26002', 'Leonardo Garcia', 'ACTIVE', true, date '2041-04-30', 'Home Loans Alabang', 3800000.00),
    ('PN-FIR-26003', 'Marites Flores', 'ACTIVE', true, date '2039-08-31', 'Home Loans Cebu', 2900000.00),
    ('PN-FIR-26004', 'Nestor Castillo', 'FULLY_PAID', false, date '2026-07-31', 'Home Loans Davao', 0.00),
    ('PN-FIR-26005', 'Olivia Navarro', 'ACTIVE', true, date '2043-02-28', 'Home Loans Pasig', 7400000.00)
) as l(pn, borrower, status, amortised, maturity, unit, balance)
where c.code = 'FVI'
  and not exists (select 1 from sbm_lamd_loan x where x.company_id = c.id and x.pn_no = l.pn);
