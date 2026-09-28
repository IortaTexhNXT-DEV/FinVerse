-- =====================================================================================
-- iNXT BrokerVerse - V1910 Seed Renewal (BRD-6) users and rules (seed profile only; the SIT/UAT
-- password is provided to testers separately). SEED DATA ONLY - never load in production.
--   Users of the Renewal personas (RENEWAL_DESIGN 6.2): rnwtl (Marketing Team Leader heading the
--   Corporate Team 1, T-CORP1, with ao2 as its officer), lamd (LAMD, loan status reports) and
--   contactc (Contact Center, follow-ups). The Marketing (ao, ao2, mkttl), Processing (proc,
--   proctl) and administration (badmin, approver) users are the New Business seed users.
--   Rules: one non-renewable risk code, the active bucket rule set version 1 and the active
--   decision matrix version 1 (institutional Clean accounts renew automatically; every other
--   account is disposed by Marketing), and two package map entries of migrated policies.
--   Design: docs/architecture/RENEWAL_DESIGN.md sections 6.2, 8 and 13.1.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select b.id from org_branch b join org_company c on c.id = b.company_id
        where c.code = 'FVI' and b.code = 'HO'),
       now(), 'SYSTEM'
from (values ('rnwtl',    'Rosario Renewal Team Lead',   'rnwtl@brokerverse-seed.ph'),
             ('lamd',     'Leonardo Loan Monitoring',    'lamd@brokerverse-seed.ph'),
             ('contactc', 'Cecilia Contact Center',      'contactc@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('rnwtl', 'MKT_TL'), ('lamd', 'LAMD'), ('contactc', 'CONTACT_CENTER'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

-- rnwtl heads the Corporate Team 1 and is one of its officers.
update cat_sales_unit u
set head_username = 'rnwtl', updated_at = now(), updated_by = 'SYSTEM'
where u.code = 'T-CORP1'
  and u.company_id = (select id from org_company where code = 'FVI')
  and u.head_username is null;

insert into cat_sales_officer (company_id, team_code, username, record_status, authorized_by,
    authorized_at, created_at, created_by)
select c.id, 'T-CORP1', 'rnwtl', 'ACTIVE', 'SYSTEM', now(), now(), 'SYSTEM'
from org_company c
where c.code = 'FVI'
  and not exists (select 1 from cat_sales_officer x where x.company_id = c.id and x.username = 'rnwtl');

-- ---------- Rules ------------------------------------------------------------------------------
insert into rnw_non_renewable_risk_code (company_id, risk_code, line_code, reason, effective_from,
    record_status, authorized_by, authorized_at, created_at, created_by)
select c.id, 'CAR07', 'ENGINEERING', 'One-off construction project cover, not renewed',
       date '2020-01-01', 'ACTIVE', 'approver', now(), now(), 'badmin'
from org_company c where c.code = 'FVI';

insert into rnw_bucket_rule_set (company_id, version_no, status, effective_from, description,
    submitted_by, submitted_at, approved_by, approved_at, created_at, created_by)
select c.id, 1, 'ACTIVE', date '2026-01-01', 'Standard bucket rules', 'badmin', now(), 'approver',
       now(), now(), 'badmin'
from org_company c where c.code = 'FVI'
  and not exists (select 1 from rnw_bucket_rule_set s where s.company_id = c.id);

insert into rnw_bucket_rule (rule_set_id, priority, check_code, severity, outcome, result_bucket,
    created_at, created_by)
select s.id, r.priority, null, r.severity, r.outcome, r.bucket, now(), 'badmin'
from rnw_bucket_rule_set s
join org_company c on c.id = s.company_id and c.code = 'FVI'
cross join (values (10, 'FAIL_EXCEPTION', 'FAIL', 'EXCEPTION'),
                   (20, 'FAIL_REVIEW', 'FAIL', 'REVIEW'),
                   (30, null, 'FAIL', 'REVIEW'),
                   (40, null, 'WARN', 'REVIEW')) as r(priority, severity, outcome, bucket)
where s.version_no = 1;

insert into rnw_decision_matrix (company_id, version_no, status, effective_from, description,
    submitted_by, submitted_at, approved_by, approved_at, created_at, created_by)
select c.id, 1, 'ACTIVE', date '2026-01-01', 'Standard decision matrix', 'badmin', now(), 'approver',
       now(), now(), 'badmin'
from org_company c where c.code = 'FVI'
  and not exists (select 1 from rnw_decision_matrix m where m.company_id = c.id);

insert into rnw_decision_rule (matrix_id, priority, segment, line_code, bucket, claims_condition,
    payment_condition, outcome_disposition, automation, letter_hint, created_at, created_by)
select m.id, r.priority, r.segment, r.line, r.bucket, r.claims, r.payment, r.outcome, r.automation,
       r.letter, now(), 'badmin'
from rnw_decision_matrix m
join org_company c on c.id = m.company_id and c.code = 'FVI'
cross join (values
    (10, 'INSTITUTIONAL', null, 'CLEAN', 'NONE', 'PAID', 'FOR_RENEWAL', 'AUTO', 'RA'),
    (20, null, null, null, 'OPEN', null, 'FOR_QUOTATION', 'MANUAL', null),
    (90, null, null, null, null, null, 'FOR_RENEWAL', 'MANUAL', 'RA'))
    as r(priority, segment, line, bucket, claims, payment, outcome, automation, letter)
where m.version_no = 1;

insert into rnw_package_map (company_id, legacy_package_code, legacy_package_version, action,
    product_code, product_version_no, source, remarks, record_status, authorized_by, authorized_at,
    created_at, created_by)
select c.id, p.code, p.version, p.action, p.product, p.product_version, 'MIGRATION', p.remarks,
       'ACTIVE', 'proctl', now(), now(), 'proc'
from org_company c
cross join (values
    ('QPS-MOTOR-A', null, 'MAP', 'MTR12', 1, 'Motor package of the legacy system'),
    ('QPS-HOME-OLD', null, 'REJECT', null, null, 'Withdrawn home package, renewed as new business'))
    as p(code, version, action, product, product_version, remarks)
where c.code = 'FVI';
