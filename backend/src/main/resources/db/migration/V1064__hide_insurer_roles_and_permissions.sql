-- =====================================================================================
-- iNXT BrokerVerse - V1064 User Access Maintenance (BRD-11): hide the insurer suite from BDOI.
-- BIBS does not use the insurer-company functions (underwriting, insurer claims, reinsurance
-- treaties and cessions, actuarial reserves, consolidation, insurer tax schedules); see
-- docs/development/CODEBASE_RELEVANCE_AUDIT.md, item R1 and step 1. This migration:
--   1. withdraws every insurer-only permission from every role other than the three insurer
--      roles (the generic finance roles of V2, FIN_MANAGER and AUDITOR included, and every BDOI
--      role), with one ROLE_PERMISSIONS row per changed role in the access change log;
--   2. deactivates UNDERWRITER, CLAIMS_OFFICER and RI_OFFICER (a deactivated role keeps its
--      permissions and members but grants nothing, BRD 3.002.3), with a DEACTIVATE_ROLE row each.
-- The insurer-only permissions are flagged in the code (Permission.isInsurerOnly): the User Access
-- screens (permission catalogue, access matrix, role-permission requests, group profile report,
-- role lists) neither list nor accept them, and a role holding one is not listed.
-- Insurer-only permissions: POLICY_VIEW, POLICY_MAINTAIN, POLICY_AUTHORIZE, CLAIM_VIEW,
-- CLAIM_MAINTAIN, CLAIM_AUTHORIZE, REINSURANCE_VIEW, REINSURANCE_MAINTAIN, REINSURANCE_AUTHORIZE,
-- RESERVE_PREPARE, RESERVE_VIEW, RESERVE_APPROVE, CONSOLIDATION_RUN, INSURER_TAX_VIEW (the last
-- three are new and granted to no role; they replace REPORT_FINANCIAL / PERIOD_END_RUN / TAX_VIEW on
-- the reserve, consolidation and insurer tax screens and reports).
-- The seed profile keeps the insurer stories of the SIT/UAT users through seed-only roles (V1961).
-- =====================================================================================

create temporary table tmp_insurer_permission (permission varchar(50) primary key) on commit drop;
insert into tmp_insurer_permission (permission) values
    ('POLICY_VIEW'), ('POLICY_MAINTAIN'), ('POLICY_AUTHORIZE'),
    ('CLAIM_VIEW'), ('CLAIM_MAINTAIN'), ('CLAIM_AUTHORIZE'),
    ('REINSURANCE_VIEW'), ('REINSURANCE_MAINTAIN'), ('REINSURANCE_AUTHORIZE'),
    ('RESERVE_PREPARE'), ('RESERVE_VIEW'), ('RESERVE_APPROVE'),
    ('CONSOLIDATION_RUN'), ('INSURER_TAX_VIEW');

-- ---------- 1. Withdraw the insurer-only permissions ------------------------------------------
insert into sec_access_change_log (occurred_at, subject_type, subject, activity, attribute,
                                   from_value, to_value, done_by)
select now(), 'ROLE', r.code, 'ROLE_PERMISSIONS', 'permissions',
       (select string_agg(p.permission, ',' order by p.permission)
        from sec_role_permission p where p.role_id = r.id),
       (select string_agg(p.permission, ',' order by p.permission)
        from sec_role_permission p
        where p.role_id = r.id
          and p.permission not in (select permission from tmp_insurer_permission)),
       'SYSTEM'
from sec_role r
where r.code not in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER')
  and exists (select 1 from sec_role_permission p
              where p.role_id = r.id
                and p.permission in (select permission from tmp_insurer_permission));

delete from sec_role_permission p
using sec_role r
where r.id = p.role_id
  and r.code not in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER')
  and p.permission in (select permission from tmp_insurer_permission);

-- ---------- 2. Deactivate the insurer roles ---------------------------------------------------
insert into sec_access_change_log (occurred_at, subject_type, subject, activity, attribute,
                                   from_value, to_value, done_by)
select now(), 'ROLE', r.code, 'DEACTIVATE_ROLE', 'active', 'true', 'false', 'SYSTEM'
from sec_role r
where r.code in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER')
  and r.active;

update sec_role
set active = false,
    deactivated_at = now(),
    deactivated_by = 'SYSTEM',
    description = coalesce(description,
        'Insurer suite role, not used by BIBS; deactivated by V1064'),
    updated_at = now(),
    updated_by = 'SYSTEM'
where code in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER')
  and active;
