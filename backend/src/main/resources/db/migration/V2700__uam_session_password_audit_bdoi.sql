-- User Access Maintenance in BDOI's FRS (v1.3): session dialog and timeout page, password history of
-- ten, password reset rules, and the source address and roles of the actor on every audit entry.
-- Each point where BDOI's FRS and the earlier behaviour differ is a setting (default: BDOI's FRS).

-- ---------- Settings --------------------------------------------------------------------------
update sys_parameter
set param_value = '10',
    description = 'Previous passwords a local account may not reuse (BDOI''s FRS: at least the last ten)',
    updated_at = now(), updated_by = 'SYSTEM'
where param_key = 'PASSWORD_HISTORY_COUNT';

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('SESSION_BDOI_DIALOG', 'true', 'BOOLEAN', 'SECURITY',
     'true: the inactivity warning reads "You have been inactive for x minutes ... stay logged in or log out?" with the buttons Stay Logged In and Log Out (BDOI''s FRS, conflict C08); false: the earlier wording with Stay Signed In and Sign Out Now',
     null, null, now(), 'SYSTEM'),
    ('SESSION_TIMEOUT_PAGE', 'true', 'BOOLEAN', 'SECURITY',
     'true: after the inactivity sign-out the page "Your session timed out. Please log in again to continue" with the Log In button (BDOI''s FRS, conflict C07); false: a message on the Login page',
     null, null, now(), 'SYSTEM'),
    ('PASSWORD_RESET_BDOI_RULES', 'true', 'BOOLEAN', 'SECURITY',
     'true: no forgotten-password link on the Login page, and a password is reset only by another System Administrator, with single sign-on only for the break-glass accounts (BDOI''s FRS, conflict C11); false: the self-service reset link for local accounts and the administrator reset of any local account',
     null, null, now(), 'SYSTEM');

-- ---------- Source address and roles of the actor (conflict C19) -------------------------------
alter table audit_log
    add column ip_address varchar(45),
    add column role_names varchar(300),
    add column old_value  varchar(500),
    add column new_value  varchar(500);

alter table sec_access_change_log
    add column ip_address varchar(45),
    add column role_names varchar(300);

alter table nba_access_request_event
    add column ip_address varchar(45),
    add column role_names varchar(300);

create index ix_audit_action_time on audit_log (action, occurred_at);
