-- =====================================================================================
-- Business wording of the User Access Maintenance settings: the descriptions name the behaviour
-- of each value only, without document or decision references.
-- =====================================================================================

update sys_parameter set description =
    'Previous passwords a local account may not reuse (at least the last ten)'
where param_key = 'PASSWORD_HISTORY_COUNT';

update sys_parameter set description =
    'true: the inactivity warning reads "You have been inactive for x minutes ... stay logged in or log out?" with the buttons Stay Logged In and Log Out (the bank''s process); false: the earlier wording with Stay Signed In and Sign Out Now'
where param_key = 'SESSION_BDOI_DIALOG';

update sys_parameter set description =
    'true: after the inactivity sign-out the page "Your session timed out. Please log in again to continue" with the Log In button (the bank''s process); false: a message on the Login page'
where param_key = 'SESSION_TIMEOUT_PAGE';

update sys_parameter set description =
    'true: no forgotten-password link on the Login page, and a password is reset only by another System Administrator, with single sign-on only for the break-glass accounts (the bank''s process); false: the self-service reset link for local accounts and the administrator reset of any local account'
where param_key = 'PASSWORD_RESET_BDOI_RULES';

update sys_parameter set description =
    'true: the exported audit logs, user access reports and Product Maintenance files are named <name>_MMDDYYYY with the extraction date, for example Audit Logs_10092026.xlsx (the bank''s process); false: they are named by the report code'
where param_key = 'EXPORT_FILE_NAMES_BDOI';

update sys_parameter set description =
    'true: users are created, updated, deactivated and reactivated from the Enterprise SSO / UIDM-ISC events and from an active Enterprise SSO account (the bank''s process); false: only through approved access requests'
where param_key = 'UAM_SSO_PROVISIONING';

update sys_parameter set description =
    'true: the group profiles named in a UIDM-ISC event are given to the user; false: group profiles only through approved requests in the system'
where param_key = 'UAM_PROVISIONING_ROLES';

update sys_parameter set description =
    'true: the notices of access requests are also e-mailed to each user who chose e-mail in the notification preferences (the bank''s process); false: in the system only'
where param_key = 'UAM_REQUEST_NOTICE_EMAIL';
