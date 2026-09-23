-- =====================================================================================
-- iNXT FinVerse - V421 Permissions for actuarial reserve valuation runs.
-- RESERVE_PREPARE: prepare, recalculate and submit a valuation run (maker).
-- Approving, posting and cancelling a run require PERIOD_END_RUN (checker); reserve
-- parameters and takaful settings use MASTER_MAINTAIN / MASTER_AUTHORIZE.
-- =====================================================================================
insert into sec_role_permission (role_id, permission)
select r.id, 'RESERVE_PREPARE' from sec_role r
where r.code in ('FIN_MANAGER', 'ACCOUNTANT');
