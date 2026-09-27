-- =====================================================================================
-- iNXT BrokerVerse - V1940 Seed Customer Servicing Facility (BRD-9) (seed profile only; password
-- for all users: Brokerverse@2026). SEED DATA ONLY - never load in production.
--   * Users of the personas (FRS BRD-9 section 3): csfagent and csfagent2 (Contact Center
--     Agents), csfsup (Contact Center Supervisor) and csfmgmt (Contact Center Management).
--   * The storyline of client CL-2026-000001 (V981): a passed caller verification with the
--     applied change of the mobile number (kept in the outbox for QPS and EBIX, not configured),
--     a failed verification, a refused change of the civil status of CL-2026-000005 and the
--     agents' activity of the last days. The renewal advice of the Renewal Advice tab is stored
--     at start-up by csf.seed.CsfSeedData through the real document services.
-- =====================================================================================

insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', null,
       (select b.id from org_branch b join org_company c on c.id = b.company_id
        where c.code = 'FVI' and b.code = 'HO'),
       now(), 'SYSTEM'
from (values ('csfagent',  'Clarissa Contact Center Agent',   'csfagent@brokerverse-seed.ph'),
             ('csfagent2', 'Carlos Contact Center Agent',     'csfagent2@brokerverse-seed.ph'),
             ('csfsup',    'Soledad Contact Center Supervisor', 'csfsup@brokerverse-seed.ph'),
             ('csfmgmt',   'Manuel Contact Center Head',      'csfmgmt@brokerverse-seed.ph'))
     as u(username, full_name, email)
where not exists (select 1 from sec_user x where lower(x.username) = u.username);

insert into sec_user_role (user_id, role_id)
select u.id, r.id
from sec_user u
join (values ('csfagent', 'CSF_AGENT'), ('csfagent2', 'CSF_AGENT'), ('csfsup', 'CSF_SUPERVISOR'),
             ('csfmgmt', 'CSF_MANAGEMENT'))
     as g(username, role_code) on g.username = u.username
join sec_role r on r.code = g.role_code
where not exists (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id);

-- ---------- Verifications ------------------------------------------------------------------------
insert into csf_verification (company_id, client_id, client_code, channel, matches, required, result,
    verified_at, valid_until, agent, remarks, created_at, created_by)
select c.company_id, c.id, c.client_code, v.channel, v.matches, 2, v.result,
       now() - v.age, now() - v.age + interval '30 minutes', v.agent, v.remarks, now() - v.age, v.agent
from crm_client c
join (values ('CL-2026-000001', 'HOTLINE', 3, 'PASSED', interval '2 days', 'csfagent',
              'Client called to update the mobile number'),
             ('CL-2026-000005', 'HOTLINE', 1, 'FAILED', interval '1 day', 'csfagent2',
              'Caller could not confirm the address of the insured property'))
     as v(client_code, channel, matches, result, age, agent, remarks) on v.client_code = c.client_code
where not exists (select 1 from csf_verification x where x.client_id = c.id);

insert into csf_verification_check (verification_id, check_index, check_code, matched)
select v.id, k.idx, k.code, k.matched
from csf_verification v
join (values ('CL-2026-000001', 0, 'ADDRESS', true), ('CL-2026-000001', 1, 'CONTACT_NUMBER', false),
             ('CL-2026-000001', 2, 'EMAIL', true), ('CL-2026-000001', 3, 'INSURED_PROPERTY', true),
             ('CL-2026-000005', 0, 'ADDRESS', false), ('CL-2026-000005', 1, 'CONTACT_NUMBER', true),
             ('CL-2026-000005', 2, 'EMAIL', false), ('CL-2026-000005', 3, 'INSURED_PROPERTY', false))
     as k(client_code, idx, code, matched) on k.client_code = v.client_code
where not exists (select 1 from csf_verification_check x where x.verification_id = v.id);

-- ---------- Contact changes ----------------------------------------------------------------------
insert into csf_contact_change (company_id, change_no, client_id, client_code, client_name,
    verification_id, channel, reason_code, remarks, status, applied_at, agent, sync_status,
    created_at, created_by)
select c.company_id, 'CSF-2026-000001', c.id, c.client_code, c.display_name, v.id, 'HOTLINE',
       'CLIENT_REQUEST', 'New mobile number after the client changed networks', 'APPLIED',
       v.verified_at + interval '4 minutes', 'csfagent', 'NOT_CONFIGURED',
       v.verified_at + interval '4 minutes', 'csfagent'
from crm_client c join csf_verification v on v.client_id = c.id and v.result = 'PASSED'
where c.client_code = 'CL-2026-000001'
  and not exists (select 1 from csf_contact_change x where x.change_no = 'CSF-2026-000001');

insert into csf_contact_change (company_id, change_no, client_id, client_code, client_name,
    verification_id, channel, reason_code, remarks, status, applied_at, agent, sync_status,
    created_at, created_by)
select c.company_id, 'CSF-2026-000002', c.id, c.client_code, c.display_name, null, 'HOTLINE',
       'CLIENT_REQUEST', 'Client asked to change the civil status', 'REFUSED',
       now() - interval '1 day' + interval '10 minutes', 'csfagent2', 'NOT_REQUIRED',
       now() - interval '1 day' + interval '10 minutes', 'csfagent2'
from crm_client c
where c.client_code = 'CL-2026-000005'
  and not exists (select 1 from csf_contact_change x where x.change_no = 'CSF-2026-000002');

insert into csf_contact_change_field (change_id, field_index, field, old_value, new_value)
select ch.id, 0, f.field, f.old_value, f.new_value
from csf_contact_change ch
join (values ('CSF-2026-000001', 'MOBILE', '09175550199', '09175550101'),
             ('CSF-2026-000002', 'CIVIL_STATUS', null, 'WIDOWED'))
     as f(change_no, field, old_value, new_value) on f.change_no = ch.change_no
where not exists (select 1 from csf_contact_change_field x where x.change_id = ch.id);

insert into csf_sync_outbox (change_id, target_system, payload, status, attempts, created_at, created_by)
select ch.id, t.target,
       '{"changeNo":"CSF-2026-000001","clientCode":"CL-2026-000001","bankCif":"CIF-00550101","fields":'
       || '[{"field":"MOBILE","oldValue":"09175550199","newValue":"09175550101"}]}',
       'NOT_CONFIGURED', 0, ch.applied_at, 'csfagent'
from csf_contact_change ch, (values ('QPS'), ('EBIX')) as t(target)
where ch.change_no = 'CSF-2026-000001'
  and not exists (select 1 from csf_sync_outbox x where x.change_id = ch.id and x.target_system = t.target);

insert into document_sequence (sequence_key, next_value) values ('CSF-2026', 101)
on conflict (sequence_key) do update set next_value = greatest(document_sequence.next_value, 101);

-- ---------- Agent activity -----------------------------------------------------------------------
insert into csf_activity (company_id, agent, action, client_id, client_code, reference, detail,
    occurred_at, created_at, created_by)
select c.company_id, a.agent, a.action, c.id, c.client_code, a.reference, a.detail,
       now() - a.age, now() - a.age, a.agent
from crm_client c
join (values ('CL-2026-000001', 'csfagent', 'SEARCH', null, 'Name: Santos (1 found)', interval '2 days 10 minutes'),
             ('CL-2026-000001', 'csfagent', 'VIEW', null, 'Santos, Maria Clara Reyes', interval '2 days 9 minutes'),
             ('CL-2026-000001', 'csfagent', 'VERIFY', null, 'PASSED 3/4', interval '2 days'),
             ('CL-2026-000001', 'csfagent', 'CONTACT_CHANGE', 'CSF-2026-000001', 'Applied', interval '1 day 23 hours 56 minutes'),
             ('CL-2026-000005', 'csfagent2', 'SEARCH', null, 'Client ID: CL-2026-000005 (1 found)', interval '1 day 5 minutes'),
             ('CL-2026-000005', 'csfagent2', 'VIEW', null, 'Garcia, Antonio Luis Dizon', interval '1 day 4 minutes'),
             ('CL-2026-000005', 'csfagent2', 'VERIFY', null, 'FAILED 1/4', interval '1 day'),
             ('CL-2026-000005', 'csfagent2', 'CONTACT_CHANGE', 'CSF-2026-000002', 'Refused', interval '23 hours 50 minutes'))
     as a(client_code, agent, action, reference, detail, age) on a.client_code = c.client_code
where not exists (select 1 from csf_activity x where x.client_id = c.id and x.agent = a.agent
                  and x.action = a.action);
