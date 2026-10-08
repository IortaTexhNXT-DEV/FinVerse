-- =====================================================================================
-- iNXT BrokerVerse - V2502 Insurer suite removal (range V2500-V2509): reference data, permissions,
-- roles and product module switches of the insurer modules.
-- BIBS is specific to BDOI as an insurance broker (client decision of 8 October 2026,
-- docs/development/CODEBASE_RELEVANCE_AUDIT.md R1). This migration removes:
--   1. the accounting event types only the insurer modules published (premium endorsement and
--      cancellation, commission accrual and coinsurance of an insurer, claim reserve, settlement,
--      recovery and coinsurance, reinsurance cessions, recoveries and statements, technical reserve
--      provisions; V3, V200, V300, V420) with their accounting rules; the event log keeps its rows;
--   2. the exception codes of insurer claims and reinsurance (V200, V300) with their alerts;
--   3. the parameters RESERVES_AUTO_RUN_COMPANIES (V420) and OPEN_COVER_TRANSIT_DAYS (V1152), and
--      the run history of the jobs QUOTATION_EXPIRY, RESERVE_VALUATION and RI_ALLOCATION;
--   4. the insurer-only permissions (POLICY_*, CLAIM_*, REINSURANCE_*, RESERVE_*, CONSOLIDATION_RUN,
--      INSURER_TAX_VIEW) from every role and from the permission catalogue, with one ROLE_PERMISSIONS
--      row per changed role in the access change log;
--   5. the insurer roles UNDERWRITER, CLAIMS_OFFICER and RI_OFFICER (V2, inactive since V1064) and
--      the seed-only roles SIT_INS_* (seed V1961), with their memberships;
--   6. the product module switches of the insurer suite (V1160) and the profile COMPLETE_SUITE,
--      which no longer differs from the insurance broker profile.
-- Version: above every seed script (db/seed up to V2090), so on a new database the seed scripts that
-- use these rows (V901, V910-V940, V1961, V2020) run first.
-- =====================================================================================

-- ---------- 1. Accounting event types and their rules ---------------------------------------
create temporary table tmp_insurer_event (code varchar(40) primary key) on commit drop;
insert into tmp_insurer_event (code) values
    ('POLICY_ENDORSEMENT'), ('POLICY_CANCELLATION'), ('COMMISSION_ACCRUAL'), ('COINSURANCE_SHARE'),
    ('CLAIM_RESERVE'), ('CLAIM_SETTLEMENT'), ('CLAIM_RECOVERY'), ('CLAIM_COINSURANCE'),
    ('RI_PREMIUM_CEDED'), ('RI_CLAIM_RECOVERY'), ('RI_RESERVE_SHARE'), ('RI_SOA_ADJUSTMENT'),
    ('RI_BALANCE_OFFSET'), ('UPR_PROVISION'), ('DAC_PROVISION'), ('IBNR_PROVISION'),
    ('TAKAFUL_SURPLUS'), ('CLAIM_MARGIN_PROVISION'), ('PREMIUM_DEFICIENCY_PROVISION');

delete from acc_rule_line
where rule_id in (select r.id from acc_rule r where r.event_type in (select code from tmp_insurer_event));
delete from acc_rule where event_type in (select code from tmp_insurer_event);
delete from acc_event_type where code in (select code from tmp_insurer_event);

-- ---------- 2. Exception codes and their alerts ----------------------------------------------
delete from alt_alert
where exception_code in ('LARGE_CLAIM_RESERVE', 'LATE_CLAIM_NOTIFICATION', 'RI_FAC_UNPLACED',
                         'RI_TREATY_CAPACITY');
delete from alt_exception_code
where code in ('LARGE_CLAIM_RESERVE', 'LATE_CLAIM_NOTIFICATION', 'RI_FAC_UNPLACED',
               'RI_TREATY_CAPACITY');

-- ---------- 3. Parameters and job history ----------------------------------------------------
delete from sys_parameter where param_key in ('RESERVES_AUTO_RUN_COMPANIES', 'OPEN_COVER_TRANSIT_DAYS');
delete from sys_job_run where job_name in ('QUOTATION_EXPIRY', 'RESERVE_VALUATION', 'RI_ALLOCATION');

-- ---------- 4. Insurer-only permissions --------------------------------------------------------
create temporary table tmp_insurer_permission (permission varchar(50) primary key) on commit drop;
insert into tmp_insurer_permission (permission) values
    ('POLICY_VIEW'), ('POLICY_MAINTAIN'), ('POLICY_AUTHORIZE'),
    ('CLAIM_VIEW'), ('CLAIM_MAINTAIN'), ('CLAIM_AUTHORIZE'),
    ('REINSURANCE_VIEW'), ('REINSURANCE_MAINTAIN'), ('REINSURANCE_AUTHORIZE'),
    ('RESERVE_PREPARE'), ('RESERVE_VIEW'), ('RESERVE_APPROVE'),
    ('CONSOLIDATION_RUN'), ('INSURER_TAX_VIEW');

create temporary table tmp_insurer_role on commit drop as
select r.id, r.code from sec_role r
where r.code in ('UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER') or r.code like 'SIT\_INS\_%';

insert into sec_access_change_log (occurred_at, subject_type, subject, activity, attribute,
                                   from_value, to_value, done_by)
select now(), 'ROLE', r.code, 'ROLE_PERMISSIONS', 'permissions',
       (select string_agg(p.permission, ',' order by p.permission)
        from sec_role_permission p where p.role_id = r.id),
       (select string_agg(p.permission, ',' order by p.permission)
        from sec_role_permission p
        where p.role_id = r.id
          and p.permission not in (select permission from tmp_insurer_permission)
          and r.id not in (select id from tmp_insurer_role)),
       'SYSTEM'
from sec_role r
where exists (select 1 from sec_role_permission p
              where p.role_id = r.id
                and (p.permission in (select permission from tmp_insurer_permission)
                     or r.id in (select id from tmp_insurer_role)));

delete from sec_role_permission where permission in (select permission from tmp_insurer_permission);
delete from sec_permission_action where permission in (select permission from tmp_insurer_permission);

-- ---------- 5. Insurer roles ---------------------------------------------------------------------
delete from sec_role_permission where role_id in (select id from tmp_insurer_role);
delete from sec_user_role where role_id in (select id from tmp_insurer_role);
delete from bcl_status_access where role_code in (select code from tmp_insurer_role);
delete from sec_role where id in (select id from tmp_insurer_role);

-- ---------- 6. Product module switches -------------------------------------------------------
delete from sys_module_profile_module
where module_code in ('UNDERWRITING', 'INSURER_CLAIMS', 'REINSURANCE', 'ACTUARIAL_RESERVES',
                      'CONSOLIDATION', 'INSURER_TAX')
   or profile_id in (select id from sys_module_profile where code = 'COMPLETE_SUITE');
delete from sys_module_profile where code = 'COMPLETE_SUITE';
delete from sys_product_module
where code in ('UNDERWRITING', 'INSURER_CLAIMS', 'REINSURANCE', 'ACTUARIAL_RESERVES', 'CONSOLIDATION',
               'INSURER_TAX');
update sys_module_profile
set description = 'Broking, operations, finance and servicing modules on',
    updated_at = now(),
    updated_by = 'SYSTEM'
where code = 'INSURANCE_BROKER';
