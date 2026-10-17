-- UIDM-ISC orchestrates the membership of the group profiles (decision option A): the provisioning
-- interface applies the group profiles UIDM-ISC sends (PATCH /Groups/{id}, the groups of a User) as
-- a membership event, and the access requests no longer give, change or remove the group profiles
-- of a user, singly or in bulk. The group profiles themselves stay maintained through requests.

alter table idn_identity_event drop constraint ck_idn_event_type;
alter table idn_identity_event add constraint ck_idn_event_type
    check (event_type in ('JOINER', 'MOVER', 'LEAVER', 'REHIRE', 'STATUS', 'MEMBERSHIP'));

-- Open requests that would still change the group profiles of a user, and the open lines of bulk
-- batches, are cancelled with the reason in their history.
create temporary table uidm_cancelled_request on commit drop as
select id, status
from nba_access_request
where status in ('DRAFT', 'PENDING', 'PENDING_SECOND', 'RETURNED', 'SCHEDULED')
  and request_type in ('CREATE_USER', 'MODIFY_ROLES', 'MODIFY_USER', 'DISABLE_USER', 'ENABLE_USER')
  and (request_type = 'MODIFY_ROLES'
       or batch_id is not null
       or (user_type = 'INTERNAL' and coalesce(role_codes, '') <> ''));

update nba_access_request r
set status = 'CANCELLED',
    cancel_reason = 'Group profiles of users are assigned through UIDM-ISC',
    cancelled_by = 'SYSTEM',
    cancelled_at = now()
from uidm_cancelled_request c
where r.id = c.id;

insert into nba_access_request_event (request_id, action, from_status, to_status, remarks, actor, occurred_at)
select c.id, 'CANCEL', c.status, 'CANCELLED',
       'Group profiles of users are assigned through UIDM-ISC', 'SYSTEM', now()
from uidm_cancelled_request c;

update nba_access_request_batch
set status = 'CANCELLED', updated_at = now(), updated_by = 'SYSTEM'
where status in ('DRAFT', 'PENDING', 'RETURNED');
