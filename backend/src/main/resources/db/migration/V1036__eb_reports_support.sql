-- =====================================================================================
-- iNXT BrokerVerse - V1036 Employee Benefits (BRD-8) wave E1-C: reporting support, retention and
-- the notice of the Broker on Record decision.
--   BRID-022          read view eb_tat_v over eb_activity_log (received / released stamps of the
--                     TAT annex with the programme's team and AO) for the TAT report and EB Home
--   BRID-024 / NFR    retention rule EB_PROGRAMME (lost or inactive programmes: 5 years online,
--                     15 years in the archive; read by the retention review through
--                     EbProgrammeRetentionProvider). EB_MEMBER follows with the member roster.
--   BRID-008          notification event EB_BOR_DECIDED (the AO when a BOR is validated or rejected)
--   Design: EMPLOYEE_BENEFITS_DESIGN sections 3, 8.2 and 16.
-- =====================================================================================

create index ix_eb_cycle_programme_stage on eb_cycle (programme_id, stage);
create index ix_eb_activity_received on eb_activity_log (company_id, received_at);

create view eb_tat_v as
select a.id,
       a.company_id,
       a.programme_id,
       p.programme_no,
       p.client_code,
       p.client_name,
       p.team_code,
       p.account_officer,
       a.cycle_id,
       c.cycle_no,
       c.business_type,
       a.activity_code,
       a.reference,
       a.received_at,
       a.released_at,
       a.actor,
       case when a.released_at is null then null
            else extract(epoch from (a.released_at - a.received_at)) / 86400.0 end as elapsed_days
from eb_activity_log a
join eb_programme p on p.id = a.programme_id
left join eb_cycle c on c.id = a.cycle_id;

insert into nba_retention_rule (record_type, statuses, years_online, years_archive, action, active,
                                description, created_at, created_by)
select 'EB_PROGRAMME', 'LOST,INACTIVE', 5, 10, 'REVIEW', true,
       'Lost or inactive Employee Benefits programmes with their cycles, feedback and documents: 5 years online, 15 years in total',
       now(), 'SYSTEM'
where not exists (select 1 from nba_retention_rule where record_type = 'EB_PROGRAMME');

insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order)
select 'EB_BOR_DECIDED', 'EB Broker on Record decided', 'EMPLOYEE_BENEFITS',
       'A Broker on Record you uploaded was validated or rejected', true, false, 505
where not exists (select 1 from msg_notification_event where code = 'EB_BOR_DECIDED');
