-- =====================================================================================
-- SIT/UAT seed data (seed profile only; never loaded in production): the accounts of the Enterprise
-- SSO simulator, the Windows IDs of the SIT users they stand for, their directory details, and a
-- few provisioning events with their outcome, so that the Identity Synchronisation screen, the
-- single sign-on through the simulator and the joiner, mover and leaver flows show realistic rows.
--   Accounts already linked to SIT users: a013000101 to a013000104 and the Auditor.
--   Joiners not yet in the system: a013000301 (Bea Santos), a013000302 (Carlo Reyes).
--   An account that is disabled in the Enterprise SSO platform: a013000303 (Dante Lim).
-- =====================================================================================

update sec_user set windows_id = v.windows_id
from (values ('a013000101', 'BDO\sit.enrolled'),
             ('a013000102', 'BDO\sit.leaver'),
             ('a013000103', 'BDO\sit.transferee'),
             ('a013000104', 'BDO\sit.teamlead'),
             ('auditor',    'BDO\sit.auditor')) as v(username, windows_id)
where sec_user.username = v.username and sec_user.windows_id is null;

insert into idn_sim_directory_account (windows_id, user_id, email, first_name, last_name,
    display_name, ad_group, status, team_leader_name, team_head_name, section_head_name,
    unit_head_name, unit_segment, department, location, uidm_request_no, updated_at)
values
    ('BDO\sit.enrolled', 'a013000101', 'a013000101@brokerverse-seed.ph', 'SIT', 'Enrolled User',
     'SIT Enrolled User', 'BIBS Users', 'ACTIVE', 'Teresa Lopez', 'Hector Villanueva',
     'Sonia Ramos', 'Ulysses Bautista', 'Retail Marketing', 'Marketing', 'Makati', 'UIDM-2026-004101',
     now() - interval '9 day'),
    ('BDO\sit.leaver', 'a013000102', 'a013000102@brokerverse-seed.ph', 'SIT', 'Leaver',
     'SIT Leaver', 'BIBS Users', 'ACTIVE', 'Teresa Lopez', 'Hector Villanueva', 'Sonia Ramos',
     'Ulysses Bautista', 'Retail Marketing', 'Marketing', 'Makati', 'UIDM-2026-004102',
     now() - interval '30 day'),
    ('BDO\sit.transferee', 'a013000103', 'a013000103@brokerverse-seed.ph', 'SIT', 'Transferee',
     'SIT Transferee', 'BIBS Users', 'ACTIVE', 'Ramon Cruz', 'Hector Villanueva', 'Sonia Ramos',
     'Ulysses Bautista', 'Technical Support Unit', 'Operations', 'CCO', 'UIDM-2026-004103',
     now() - interval '20 day'),
    ('BDO\sit.teamlead', 'a013000104', 'a013000104@brokerverse-seed.ph', 'SIT', 'Team Lead',
     'SIT Team Lead', 'BIBS Users', 'ACTIVE', null, 'Hector Villanueva', 'Sonia Ramos',
     'Ulysses Bautista', 'Retail Marketing', 'Marketing', 'Makati', 'UIDM-2026-004104',
     now() - interval '15 day'),
    ('BDO\sit.auditor', 'auditor', 'auditor@brokerverse-seed.ph', 'Audrey', 'Auditor',
     'Audrey Auditor', 'BIBS Users', 'ACTIVE', null, null, null, 'Internal Audit Head',
     'Internal Audit', 'Internal Audit', 'CCO', null, now() - interval '40 day'),
    ('BDO\bsantos', 'a013000301', 'bea.santos@brokerverse-seed.ph', 'Bea', 'Santos', 'Bea Santos',
     'BIBS Users', 'ACTIVE', 'Teresa Lopez', 'Hector Villanueva', 'Sonia Ramos',
     'Ulysses Bautista', 'Retail Marketing', 'Marketing', 'Makati', 'UIDM-2026-004301', now()),
    ('BDO\creyes', 'a013000302', 'carlo.reyes@brokerverse-seed.ph', 'Carlo', 'Reyes',
     'Carlo Reyes', 'BIBS Users', 'ACTIVE', 'Ramon Cruz', 'Hector Villanueva', 'Sonia Ramos',
     'Ulysses Bautista', 'Technical Support Unit', 'Operations', 'CCO', 'UIDM-2026-004302', now()),
    ('BDO\dlim', 'a013000303', 'dante.lim@brokerverse-seed.ph', 'Dante', 'Lim', 'Dante Lim',
     'BIBS Users', 'DISABLED', 'Ramon Cruz', 'Hector Villanueva', 'Sonia Ramos',
     'Ulysses Bautista', 'Technical Support Unit', 'Operations', 'Cebu', 'UIDM-2026-004303',
     now() - interval '2 day');

insert into idn_directory_profile (username, windows_id, ad_email, ad_group, ad_status, ad_sync_at,
    first_name, last_name, display_name, team_leader_name, team_head_name, section_head_name,
    unit_head_name, unit_segment, department, location, uidm_request_no, last_event_type,
    last_event_at, last_event_source, sync_status, created_at, created_by)
select a.user_id, a.windows_id, a.email, a.ad_group, a.status, a.updated_at, a.first_name,
       a.last_name, a.display_name, a.team_leader_name, a.team_head_name, a.section_head_name,
       a.unit_head_name, a.unit_segment, a.department, a.location, a.uidm_request_no,
       case when a.user_id = 'a013000101' then 'JOINER' else 'MOVER' end, a.updated_at,
       'UIDM_ISC', 'SYNCED', a.updated_at, 'UIDM-ISC'
from idn_sim_directory_account a
where exists (select 1 from sec_user u where u.username = a.user_id);

insert into idn_identity_event (received_at, source, event_type, windows_id, user_id,
    uidm_request_no, ad_status, payload, status, username, message, processed_at, processed_by,
    attempts)
values
    (now() - interval '9 day', 'UIDM_ISC', 'JOINER', 'BDO\sit.enrolled', 'a013000101',
     'UIDM-2026-004101', 'ACTIVE', '{"windowsId":"BDO\\sit.enrolled","userId":"a013000101"}',
     'APPLIED', 'a013000101', 'Created user a013000101', now() - interval '9 day', 'UIDM-ISC', 1),
    (now() - interval '6 day', 'UIDM_ISC', 'JOINER', 'BDO\sit.transferee', 'a013000901',
     'UIDM-2026-004190', 'ACTIVE',
     '{"windowsId":"BDO\\sit.transferee","userId":"a013000901","email":"a013000901@brokerverse-seed.ph","firstName":"Second","lastName":"Account","status":"ACTIVE"}',
     'REFUSED', null, 'Windows ID BDO\sit.transferee belongs to another user',
     now() - interval '6 day', 'UIDM-ISC', 1),
    (now() - interval '3 day', 'UIDM_ISC', 'MOVER', 'BDO\sit.transferee', 'a013000103',
     'UIDM-2026-004103', 'ACTIVE', '{"windowsId":"BDO\\sit.transferee","userId":"a013000103"}',
     'APPLIED', 'a013000103', 'Updated 1 detail(s) of user a013000103', now() - interval '3 day',
     'UIDM-ISC', 1),
    (now() - interval '1 day', 'ENTERPRISE_SSO', 'STATUS', 'BDO\sit.auditor', 'auditor', null,
     'ACTIVE', '{"windowsId":"BDO\\sit.auditor","userId":"auditor","status":"ACTIVE"}',
     'NO_CHANGE', 'auditor', 'Reactivated user auditor for the Enterprise SSO status ACTIVE',
     now() - interval '1 day', 'Enterprise SSO', 1);

update sys_parameter set param_value = 'information.security@brokerverse-seed.ph',
    updated_at = now(), updated_by = 'SYSTEM'
where param_key = 'BREAK_GLASS_ALERT_RECIPIENTS';
