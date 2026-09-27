-- =====================================================================================
-- iNXT BrokerVerse - V1066 User Access Maintenance (BRD-11): the grants of the Information Security
-- Officer. The Information Security Officer runs the user access reports (and the report runner),
-- authorises the separation-of-duties rules and approves the changes of the security parameters.
--
-- The role INFOSEC_OFFICER is created by V1101 (document storage), which runs after this version on a new
-- database and before it on a database that already has V1101 (out-of-order migrations). Both cases are
-- covered: the grants are inserted now when the role exists, and a trigger on sec_role inserts them when the
-- role is created later. The grants are idempotent.
-- =====================================================================================

create or replace function uam_grant_information_security() returns trigger
language plpgsql as $$
begin
    insert into sec_role_permission (role_id, permission)
    select new.id, g.permission
    from (values ('UAM_REPORT_VIEW'), ('REPORT_VIEW'), ('UAM_SOD_AUTHORIZE'), ('SECURITY_PARAMETER_APPROVE'))
         as g(permission)
    where not exists (select 1 from sec_role_permission x where x.role_id = new.id and x.permission = g.permission);
    return new;
end;
$$;

create trigger trg_uam_grant_information_security
    after insert on sec_role
    for each row when (new.code = 'INFOSEC_OFFICER')
    execute function uam_grant_information_security();

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values
    ('INFOSEC_OFFICER', 'UAM_REPORT_VIEW'), ('INFOSEC_OFFICER', 'REPORT_VIEW'),
    ('INFOSEC_OFFICER', 'UAM_SOD_AUTHORIZE'), ('INFOSEC_OFFICER', 'SECURITY_PARAMETER_APPROVE')
) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);
