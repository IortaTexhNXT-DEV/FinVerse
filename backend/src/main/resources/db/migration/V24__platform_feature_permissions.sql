-- =====================================================================================
-- iNXT FinVerse - V24 Permissions for attachments, alerts and system monitoring.
-- =====================================================================================

-- Attachments: everyone who can view records may view their documents.
insert into sec_role_permission (role_id, permission)
select id, 'ATTACHMENT_VIEW' from sec_role;

insert into sec_role_permission (role_id, permission)
select id, 'ATTACHMENT_MANAGE' from sec_role
where code in ('FIN_ADMIN', 'FIN_MANAGER', 'ACCOUNTANT', 'AUTHORIZER', 'BRANCH_FINANCE',
               'UNDERWRITER', 'CLAIMS_OFFICER', 'RI_OFFICER');

-- Alerts: finance control roles and auditors view; managers and checkers acknowledge.
insert into sec_role_permission (role_id, permission)
select id, 'ALERT_VIEW' from sec_role
where code in ('SYSADMIN', 'FIN_ADMIN', 'FIN_MANAGER', 'AUTHORIZER', 'AUDITOR');

insert into sec_role_permission (role_id, permission)
select id, 'ALERT_MANAGE' from sec_role where code in ('SYSADMIN', 'FIN_MANAGER', 'AUTHORIZER');

-- System monitoring (job monitor, application info, parameter view).
insert into sec_role_permission (role_id, permission)
select id, 'SYSTEM_MONITOR' from sec_role where code in ('SYSADMIN', 'FIN_MANAGER', 'AUDITOR');
