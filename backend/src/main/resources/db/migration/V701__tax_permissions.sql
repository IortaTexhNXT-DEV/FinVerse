-- =====================================================================================
-- iNXT FinVerse - V701 Tax & Statutory permissions.
--   TAX_VIEW   : worksheets, returns register, 2307 register, IC schedules and tax reports.
--   TAX_MANAGE : maintain tax masters and IC mappings, prepare / file / pay returns, generate
--                2307 certificates. Masters are authorized with MASTER_AUTHORIZE (maker-checker);
--                a return is filed by a user other than its preparer (four eyes).
-- =====================================================================================
insert into sec_role_permission (role_id, permission)
select r.id, 'TAX_VIEW' from sec_role r
where r.code in ('FIN_ADMIN', 'FIN_MANAGER', 'ACCOUNTANT', 'AUTHORIZER', 'AUDITOR');

insert into sec_role_permission (role_id, permission)
select r.id, 'TAX_MANAGE' from sec_role r
where r.code in ('FIN_ADMIN', 'FIN_MANAGER', 'ACCOUNTANT', 'AUTHORIZER');
