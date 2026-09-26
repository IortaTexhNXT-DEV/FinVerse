-- =====================================================================================
-- iNXT BrokerVerse - V791 Broking administration: role-permission change requests (PMADD05).
--   PMADD05 AC3 / design 6.3  changes to the permissions of a role go through an access request
--                             of type MODIFY_ROLE_PERMISSIONS (role, permissions added and
--                             removed, justification), decided by ACCESS_APPROVE (never the
--                             requester) and applied through the security administration
--                             service. Whether the direct role maintenance of SYSADMIN is also
--                             restricted is PQ17.
--   Design: docs/architecture/PRODUCT_MAINTENANCE_DESIGN.md section 6.3.
-- =====================================================================================

alter table nba_access_request alter column request_type type varchar(30);
alter table nba_access_request alter column username drop not null;
alter table nba_access_request add column role_code varchar(40);
alter table nba_access_request add column permissions_added varchar(2000);
alter table nba_access_request add column permissions_removed varchar(2000);

alter table nba_access_request drop constraint ck_nba_access_type;
alter table nba_access_request add constraint ck_nba_access_type
    check (request_type in ('CREATE_USER', 'MODIFY_ROLES', 'DISABLE_USER', 'ENABLE_USER',
                            'MODIFY_ROLE_PERMISSIONS'));

-- A user request names the user; a role-permission request names the role and a change.
alter table nba_access_request add constraint ck_nba_access_subject
    check ((request_type = 'MODIFY_ROLE_PERMISSIONS'
            and role_code is not null
            and (permissions_added is not null or permissions_removed is not null))
        or (request_type <> 'MODIFY_ROLE_PERMISSIONS' and username is not null));

create index ix_nba_access_role on nba_access_request (role_code) where role_code is not null;
