-- =====================================================================================
-- iNXT BrokerVerse - V1180 Platform authentication and session security: the token model.
--   1. Short access tokens with a sliding refresh: the access token lives ACCESS_TOKEN_MINUTES
--      (15 by default); the browser renews it with a rotating refresh token bound to the sign-in
--      session. The session keeps the hash of the current refresh token and of the one it replaced
--      (a replaced token presented again after the grace period ends the session: TOKEN_REUSED).
--   2. How the session was opened (PASSWORD, OIDC, SAML) and whether a second factor was checked.
--   3. METRICS_VIEW: the metrics endpoints on the application port (for a service credential);
--      the monitoring namespace scrapes the management port without a token.
--   4. Alert SECURITY_STORE_UNAVAILABLE: the token denylist, the session log or the rate limit
--      counters cannot be read; sign-ins are checked against the database or refused.
-- =====================================================================================

alter table sec_user_session add column refresh_hash varchar(64);
alter table sec_user_session add column previous_refresh_hash varchar(64);
alter table sec_user_session add column refreshed_at timestamptz;
alter table sec_user_session add column sign_in_method varchar(10) not null default 'PASSWORD';
alter table sec_user_session add column second_factor boolean not null default false;
alter table sec_user_session add constraint ck_sec_user_session_method
    check (sign_in_method in ('PASSWORD', 'OIDC', 'SAML'));
alter table sec_user_session drop constraint ck_sec_user_session_reason;
alter table sec_user_session add constraint ck_sec_user_session_reason check (end_reason is null or end_reason in
    ('LOGOUT', 'IDLE_TIMEOUT', 'EXPIRED', 'ADMIN_ENDED', 'LOCKED', 'TOKEN_REUSED'));
create unique index uq_sec_user_session_refresh on sec_user_session (refresh_hash) where refresh_hash is not null;
create index ix_sec_user_session_previous_refresh on sec_user_session (previous_refresh_hash)
    where previous_refresh_hash is not null;

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('ACCESS_TOKEN_MINUTES', '15', 'INTEGER', 'SECURITY',
     'Minutes an access token is valid; the web client renews it while the session is in use', 5, 60,
     now(), 'SYSTEM')
on conflict (param_key) do nothing;

insert into sec_permission_action (permission, area, action)
values ('METRICS_VIEW', 'ADMINISTRATION', 'VIEW')
on conflict (permission, action) do nothing;

insert into alt_exception_code (code, name, description, module, severity, threshold_amount, threshold_days,
    created_at, created_by) values
    ('SECURITY_STORE_UNAVAILABLE', 'Sign-in security store unavailable',
     'The token denylist, the session log or the sign-in rate limit counters cannot be read; sign-ins are checked against the database or refused until the store answers.',
     'SYSTEM', 'CRITICAL', null, null, now(), 'SYSTEM')
on conflict (code) do nothing;
