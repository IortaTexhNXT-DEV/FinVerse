-- =====================================================================================
-- iNXT BrokerVerse - V1130 User Access Maintenance (BRD-11), extension range V1130-V1139: the grants of
-- the Information Security Officer. The role INFOSEC_OFFICER is created by V1101 (document storage),
-- which runs before this version. The Information Security Officer runs the user access reports (and
-- the report runner), authorises the separation-of-duties rules (V1065) and approves the changes of
-- the security parameters (V1065).
-- =====================================================================================

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    ('INFOSEC_OFFICER', 'UAM_REPORT_VIEW'), ('INFOSEC_OFFICER', 'REPORT_VIEW'),
    ('INFOSEC_OFFICER', 'UAM_SOD_AUTHORIZE'), ('INFOSEC_OFFICER', 'SECURITY_PARAMETER_APPROVE')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);
