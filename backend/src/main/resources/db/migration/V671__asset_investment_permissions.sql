-- =====================================================================================
-- iNXT BrokerVerse - V671 Permissions for fixed asset and investment transactions
-- (disposals, transfers, coupon receipts, maturities, sales, fair value updates).
-- Masters use MASTER_*; month-end runs use PERIOD_END_RUN.
-- =====================================================================================
insert into sec_role_permission (role_id, permission)
select r.id, p from sec_role r, unnest(array['ASSET_MANAGE', 'INVESTMENT_MANAGE']) as p
where r.code in ('FIN_MANAGER', 'ACCOUNTANT', 'AUTHORIZER');
