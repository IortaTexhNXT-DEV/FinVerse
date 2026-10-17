-- The case-insensitive lookups of Spring Data (findBy...IgnoreCase) compare upper(column) =
-- upper(?), which the lower(...) indexes of these tables do not serve: every sign-in, request
-- and SCIM call read sec_user, the session log and the access change log in full. Measured on a
-- copy with 600 users, 120,000 sessions and 60,000 change-log rows (perf/uam/README.md): the
-- sessions of a user 33 ms -> 0.1 ms, the access history of a user 19 ms -> 0.6 ms.
create index if not exists ix_sec_user_username_upper on sec_user (upper(username));
create index if not exists ix_sec_user_windows_id_upper on sec_user (upper(windows_id))
    where windows_id is not null;
create index if not exists ix_sec_user_session_user_upper
    on sec_user_session (upper(username), issued_at desc);
create index if not exists ix_sec_access_change_subject_upper
    on sec_access_change_log (subject_type, upper(subject), id desc);
create index if not exists ix_sec_user_mfa_user_upper on sec_user_mfa (upper(username), status);
create index if not exists ix_sec_mfa_recovery_code_user_upper on sec_mfa_recovery_code (upper(username));
create index if not exists ix_sec_password_history_user_upper
    on sec_password_history (upper(username), changed_at desc);
create index if not exists ix_sec_password_reset_user_upper
    on sec_password_reset_token (upper(username), requested_at desc);
create index if not exists ix_idn_profile_user_upper on idn_directory_profile (upper(username));
create index if not exists ix_nba_access_username_upper on nba_access_request (upper(username))
    where username is not null;
