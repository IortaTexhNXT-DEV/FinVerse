-- =====================================================================================
-- iNXT BrokerVerse - V1962 Seed data of the separation-of-duties rules (BRD-11 sign-off, V1065).
-- SEED DATA ONLY - never load in production.
--   SOD-900001  active:  the Requestor and the Approver profiles are not held by one user
--               (created by badmin, authorised by infosec).
--   SOD-900002  waiting for the authorisation of Information Security: the Requestor and the
--               Second Approver profiles (created by badmin).
-- =====================================================================================

insert into nba_sod_rule (rule_code, profile_a, profile_b, description, record_status, pending_action,
                          authorized_by, authorized_at, created_at, created_by, updated_at, updated_by)
select v.rule_code, v.profile_a, v.profile_b, v.description, v.record_status, v.pending_action,
       v.authorized_by, case when v.authorized_by is null then null else now() end,
       now(), 'badmin', now(), 'badmin'
from (values
    ('SOD-900001', 'UAM_REQUESTOR', 'UAM_APPROVER',
     'The user who raises an access request may not also approve access requests',
     'ACTIVE', 'NONE', 'infosec'),
    ('SOD-900002', 'UAM_REQUESTOR', 'UAM_SECOND_APPROVER',
     'The user who raises an access request may not give the second approval of privileged changes',
     'PENDING_AUTHORIZATION', 'CREATE', null)
) as v(rule_code, profile_a, profile_b, description, record_status, pending_action, authorized_by)
where not exists (select 1 from nba_sod_rule x where x.rule_code = v.rule_code);
