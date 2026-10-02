-- =====================================================================================
-- iNXT BrokerVerse - V1961 Seed data of User Access Maintenance: insurer story roles.
-- SEED DATA ONLY - never load in production.
-- V1064 withdraws the insurer-only permissions from every role and deactivates UNDERWRITER,
-- CLAIMS_OFFICER and RI_OFFICER. Until the insurer modules are removed (the later waves of
-- docs/development/CODEBASE_RELEVANCE_AUDIT.md), their seed stories and automated tests still run
-- as the SIT/UAT users uw, claims, reinsurer, fmanager, accountant, checker and auditor. This seed
-- gives those users the same insurer access as before V1064 through seed-only roles SIT_INS_<role>:
--   * SIT_INS_UNDERWRITER, SIT_INS_CLAIMS_OFFICER, SIT_INS_RI_OFFICER: copies of the deactivated
--     roles with all their permissions;
--   * SIT_INS_<finance role>: the insurer-only permissions the V2 finance role held (V2, V300,
--     V421, seed V910 and V920), with RESERVE_VIEW and INSURER_TAX_VIEW where the role held
--     REPORT_FINANCIAL / TAX_VIEW and RESERVE_APPROVE where it held PERIOD_END_RUN.
-- Each copy has the members of its role. The roles hold insurer-only permissions, so the User
-- Access screens do not list them.
-- =====================================================================================

insert into sec_role (code, name, description, created_at, created_by)
select 'SIT_INS_' || r.code, r.name || ' - insurer stories (SIT/UAT)',
       'Seed-only role: insurer suite access of the SIT/UAT users until the insurer modules are removed',
       now(), 'SYSTEM'
from sec_role r
where r.code in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER', 'FIN_ADMIN', 'FIN_MANAGER',
                 'ACCOUNTANT', 'AUTHORIZER', 'BRANCH_FINANCE', 'AUDITOR', 'READ_ONLY')
  and not exists (select 1 from sec_role x where x.code = 'SIT_INS_' || r.code);

-- Copies of the three insurer roles.
insert into sec_role_permission (role_id, permission)
select s.id, p.permission
from sec_role r
join sec_role_permission p on p.role_id = r.id
join sec_role s on s.code = 'SIT_INS_' || r.code
where r.code in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER')
on conflict do nothing;

-- Insurer access of the finance roles.
insert into sec_role_permission (role_id, permission)
select s.id, g.permission
from (values
    ('FIN_ADMIN', 'RESERVE_VIEW'), ('FIN_ADMIN', 'INSURER_TAX_VIEW'),
    ('FIN_MANAGER', 'POLICY_VIEW'), ('FIN_MANAGER', 'POLICY_AUTHORIZE'),
    ('FIN_MANAGER', 'CLAIM_VIEW'), ('FIN_MANAGER', 'CLAIM_AUTHORIZE'),
    ('FIN_MANAGER', 'REINSURANCE_VIEW'), ('FIN_MANAGER', 'REINSURANCE_AUTHORIZE'),
    ('FIN_MANAGER', 'RESERVE_PREPARE'), ('FIN_MANAGER', 'RESERVE_VIEW'),
    ('FIN_MANAGER', 'RESERVE_APPROVE'), ('FIN_MANAGER', 'CONSOLIDATION_RUN'),
    ('FIN_MANAGER', 'INSURER_TAX_VIEW'),
    ('ACCOUNTANT', 'POLICY_VIEW'), ('ACCOUNTANT', 'CLAIM_VIEW'), ('ACCOUNTANT', 'REINSURANCE_VIEW'),
    ('ACCOUNTANT', 'RESERVE_PREPARE'), ('ACCOUNTANT', 'RESERVE_VIEW'),
    ('ACCOUNTANT', 'INSURER_TAX_VIEW'),
    ('AUTHORIZER', 'POLICY_VIEW'), ('AUTHORIZER', 'CLAIM_VIEW'), ('AUTHORIZER', 'CLAIM_AUTHORIZE'),
    ('AUTHORIZER', 'REINSURANCE_VIEW'), ('AUTHORIZER', 'REINSURANCE_AUTHORIZE'),
    ('AUTHORIZER', 'RESERVE_VIEW'), ('AUTHORIZER', 'INSURER_TAX_VIEW'),
    ('BRANCH_FINANCE', 'POLICY_VIEW'), ('BRANCH_FINANCE', 'CLAIM_VIEW'),
    ('AUDITOR', 'POLICY_VIEW'), ('AUDITOR', 'CLAIM_VIEW'), ('AUDITOR', 'REINSURANCE_VIEW'),
    ('AUDITOR', 'RESERVE_VIEW'), ('AUDITOR', 'INSURER_TAX_VIEW'),
    ('READ_ONLY', 'POLICY_VIEW'), ('READ_ONLY', 'CLAIM_VIEW'))
    as g(role_code, permission)
join sec_role s on s.code = 'SIT_INS_' || g.role_code
on conflict do nothing;

-- Members: every holder of the original role.
insert into sec_user_role (user_id, role_id)
select ur.user_id, s.id
from sec_user_role ur
join sec_role r on r.id = ur.role_id
join sec_role s on s.code = 'SIT_INS_' || r.code
on conflict do nothing;
