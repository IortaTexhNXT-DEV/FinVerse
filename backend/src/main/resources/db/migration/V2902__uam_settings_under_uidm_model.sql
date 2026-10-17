-- User Access settings aligned with the UIDM-ISC model.
--
--   AUTH_MODE                no break-glass administrators any more: local passwords serve only the
--                            test accounts outside production.
--   UAM_PROVISIONING_ROLES   UIDM-ISC is the single source of group-profile membership, so the group
--                            profiles named in its user events are applied as well (on by default).
--   UAM_ANY_APPROVER         the agreed rule: any user other than the maker who holds the checker
--                            right decides a group-profile change (on by default).

update sys_parameter
   set description = 'Sign-in mode: OIDC or SAML (the Enterprise SSO identity provider), DIRECTORY (the client''s directory) or LOCAL (passwords held by this system, only for test accounts outside production)'
 where param_key = 'AUTH_MODE';

update sys_parameter
   set param_value = 'true',
       description = 'true: the group profiles named in a UIDM-ISC user event are given to the user, as with the group membership events; false: only the group membership events of UIDM-ISC change the group profiles of a user',
       updated_at = now(),
       updated_by = 'SYSTEM'
 where param_key = 'UAM_PROVISIONING_ROLES';

update sys_parameter
   set param_value = 'true',
       description = 'true: any user other than the maker who holds the checker right may decide a group-profile change; false: only the checker chosen by the maker',
       updated_at = now(),
       updated_by = 'SYSTEM'
 where param_key = 'UAM_ANY_APPROVER';
