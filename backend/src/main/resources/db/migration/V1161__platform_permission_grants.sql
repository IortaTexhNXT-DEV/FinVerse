-- =====================================================================================
-- iNXT BrokerVerse - V1161 Platform consistency: permissions checked by the server and granted to no
-- role. MIG_RESUBMIT_APPROVE (V1080, approval of a resubmission of corrected rejected rows) goes to
-- the Data Owner of a migration object, the checker of the design (DATA_MIGRATION_DESIGN section 15.1);
-- the console limits it to the user named as owner of the object, and the maker never approves.
-- PermissionConsistencyTest keeps every permission of a server check granted to a role.
-- =====================================================================================

insert into sec_role_permission (role_id, permission)
select r.id, g.permission
from sec_role r
join (values ('DATA_OWNER', 'MIG_RESUBMIT_APPROVE')) as g(role_code, permission) on g.role_code = r.code
where not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = g.permission);
