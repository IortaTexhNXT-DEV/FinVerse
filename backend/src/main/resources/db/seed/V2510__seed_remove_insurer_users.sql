-- =====================================================================================
-- iNXT BrokerVerse - V2510 Seed data: the insurer SIT/UAT users are removed.
-- SEED DATA ONLY - never load in production.
-- The insurer modules are removed (V2500-V2502, docs/development/CODEBASE_RELEVANCE_AUDIT.md R1), so
-- the SIT/UAT users of the insurer stories (seed V900) are no longer needed: uw (underwriter),
-- claims (insurer claims officer) and reinsurer (reinsurance officer). Their roles were removed by
-- V2502; their memberships and data scope go with them. The finance users fmanager, accountant,
-- checker and auditor stay.
-- Version: after the removal migrations V2500-V2502 and after every other seed script.
-- =====================================================================================

delete from sec_user_role
where user_id in (select id from sec_user where username in ('uw', 'claims', 'reinsurer'));
delete from sec_user_data_scope
where user_id in (select id from sec_user where username in ('uw', 'claims', 'reinsurer'));
delete from sec_user where username in ('uw', 'claims', 'reinsurer');
