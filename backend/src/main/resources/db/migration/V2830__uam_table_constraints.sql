-- Review of the User Access tables (sec_*, nba_access_request*, idn_*): the columns that hold a Java
-- enum get the check of its values, and the counters and amounts their range. The other enum
-- columns already had their check (request type and status, event action, approver decision,
-- change activity, session end reason, event source, type and status, simulated account status,
-- separation-of-duties rule kind, privilege level).

-- DirectoryStatus
alter table idn_identity_event add constraint ck_idn_event_ad_status
    check (ad_status is null or ad_status in ('ACTIVE', 'INACTIVE', 'LOCKED', 'DISABLED', 'DEACTIVATED'));
alter table idn_directory_profile add constraint ck_idn_profile_ad_status
    check (ad_status is null or ad_status in ('ACTIVE', 'INACTIVE', 'LOCKED', 'DISABLED', 'DEACTIVATED'));

-- IdentityEventType and IdentityEventSource of the last event applied to the profile
alter table idn_directory_profile add constraint ck_idn_profile_event_type
    check (last_event_type is null
        or last_event_type in ('JOINER', 'MOVER', 'LEAVER', 'REHIRE', 'STATUS', 'MEMBERSHIP'));
alter table idn_directory_profile add constraint ck_idn_profile_event_source
    check (last_event_source is null or last_event_source in ('UIDM_ISC', 'ENTERPRISE_SSO', 'ON_DEMAND'));

-- Counters and amounts
alter table sec_user add constraint ck_sec_user_failed_attempts check (failed_attempts >= 0);
alter table sec_user add constraint ck_sec_user_authorization_limit
    check (authorization_limit is null or authorization_limit >= 0);
alter table nba_access_request add constraint ck_nba_access_returned_count check (returned_count >= 0);
alter table nba_access_request add constraint ck_nba_access_authorization_limit
    check (authorization_limit is null or authorization_limit >= 0);
alter table idn_identity_event add constraint ck_idn_event_attempts check (attempts >= 0);
