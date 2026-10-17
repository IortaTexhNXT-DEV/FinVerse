-- =====================================================================================
-- iNXT BrokerVerse - V1009 Collections: the statements of account are prepared by Operations
-- (FR-CL-060, 061, 062; minutes of 27 April 2026 on BRD 58; CLR-CL-32).
--   CLX_SOA_ISSUE  runs the billing, generates, sends and cancels the statements of account;
--                  granted to the existing Cashiering Team Leader profile (no new profile). The
--                  Collections roles keep CLX_VIEW and see the statements, their lines and the
--                  e-mails sent, read only. CLX_BILLING keeps the installment plans of the
--                  collectors.
-- =====================================================================================
insert into sec_permission_action (permission, area, action)
values ('CLX_SOA_ISSUE', 'COLLECTIONS', 'CREATE');

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values ('CASHIER_TL', 'CLX_SOA_ISSUE')) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);
