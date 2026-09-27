-- =====================================================================================
-- iNXT BrokerVerse - V1102 User Access Maintenance (BRD-11): the grants of the Information Security
-- Officer. The role is created by V1101 (document storage), so its grants follow it; the other
-- controls of the sign-off are in V1065.
--   The Information Security Officer runs the user access reports (and the report runner),
--   authorises the separation-of-duties rules and approves the changes of the security parameters.
-- =====================================================================================

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    ('INFOSEC_OFFICER', 'UAM_REPORT_VIEW'), ('INFOSEC_OFFICER', 'REPORT_VIEW'),
    ('INFOSEC_OFFICER', 'UAM_SOD_AUTHORIZE'), ('INFOSEC_OFFICER', 'SECURITY_PARAMETER_APPROVE')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);
