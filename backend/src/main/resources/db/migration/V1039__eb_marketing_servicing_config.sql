-- =====================================================================================
-- iNXT BrokerVerse - V1039 Employee Benefits (BRD-8) waves E1-B / E1-C: configuration of the
-- marketing and servicing steps built on V1037 and V1038.
--   Parameter EB_FRANCHISE_GRACE_DAYS (working days after the franchise decision date before the
--   job EB_FRANCHISE_EXPIRY expires a request; EBQ07)
--   Exception code EB_FRANCHISE_ADVICE_LATE (outcome not advised within EB_FRANCHISE_ADVICE_DAYS)
--   Notification events EB_COMPARATIVE_DECIDED, EB_ROSTER_STAGED, EB_SUBMISSION_SENT
--   Templates EB_MEMBER_CHANGE_RELAY, EB_SUBMISSION_COVER, EB_SOA_RELEASE (e-mail wording until
--   BDOI supplies its layouts, EBQ21)
--   Reporting indexes of the EB reports
--   Design: EMPLOYEE_BENEFITS_DESIGN sections 8 and 16.8.
-- =====================================================================================

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by) values
    ('EB_FRANCHISE_GRACE_DAYS', '2', 'INTEGER', 'EMPLOYEE_BENEFITS',
     'Working days after the franchise decision date before an undecided franchise request expires',
     0, 30, now(), 'SYSTEM')
on conflict (param_key) do nothing;

insert into alt_exception_code (code, name, description, module, severity, threshold_amount,
    threshold_days, created_at, created_by)
select 'EB_FRANCHISE_ADVICE_LATE', 'EB franchise outcome not advised',
       'The client was not advised of an insurer franchise decision within EB_FRANCHISE_ADVICE_DAYS working days.',
       'EMPLOYEE_BENEFITS', 'LOW', null, 2, now(), 'SYSTEM'
where not exists (select 1 from alt_exception_code where code = 'EB_FRANCHISE_ADVICE_LATE');

insert into msg_notification_event (code, name, module, description, default_in_app, default_email, sort_order)
select v.code, v.name, 'EMPLOYEE_BENEFITS', v.description, true, false, v.sort_order
from (values
    ('EB_COMPARATIVE_DECIDED', 'EB comparative decided',
     'A comparative of your programme was signed off, approved above the threshold or returned', 535),
    ('EB_ROSTER_STAGED', 'EB master list staged',
     'A master list of your programme was loaded and waits for your review', 565),
    ('EB_SUBMISSION_SENT', 'EB documents submitted',
     'Documents of an Employee Benefits programme were submitted to an insurer', 575)
) as v(code, name, description, sort_order)
where not exists (select 1 from msg_notification_event e where e.code = v.code);

insert into doc_template (code, version_no, title, body, effective_from, created_at, created_by)
values
('EB_MEMBER_CHANGE_RELAY', 1, 'Member change {{changeNo}} - {{clientName}}',
 'Dear {{insurerName}},

Please process the following member changes of {{clientName}} ({{programmeName}}), policy {{policyNo}}:

{{memberLines}}

Please send us the billing of the changes and the member cards where applicable.

Thank you,
{{aoName}}
BDO Insurance and Reinsurance Brokers, Inc. - Employee Benefits', date '2020-01-01', now(), 'SYSTEM'),
('EB_SUBMISSION_COVER', 1, '{{process}} documents - {{clientName}}',
 'Dear {{insurerName}},

Please find attached the documents of {{clientName}} ({{programmeName}}) for {{process}}:

{{documents}}

{{remarks}}

Thank you,
{{aoName}}
BDO Insurance and Reinsurance Brokers, Inc. - Employee Benefits', date '2020-01-01', now(), 'SYSTEM'),
('EB_SOA_RELEASE', 1, 'Statement of account {{soaNo}} - {{programmeName}}',
 'Dear {{contactName}},

Please find attached the statement of account {{soaNo}} of {{insurerName}} for {{programmeName}}, covering {{period}}, for {{amount}}.

For payment arrangements, please reply to this e-mail.

Thank you,
{{aoName}}
BDO Insurance and Reinsurance Brokers, Inc. - Employee Benefits', date '2020-01-01', now(), 'SYSTEM');

-- Reporting indexes (EB-PLACEMENT, EB-FRANCHISE, EB-RENEWAL).
create index ix_eb_franchise_programme on eb_franchise_request (programme_id, status);
create index ix_eb_insurer_request_cycle on eb_insurer_request (cycle_id, status);
create index ix_eb_confirmation_cycle on eb_client_confirmation (cycle_id, status);
create index ix_eb_soa_programme on eb_soa (programme_id, status);

-- The accounts of each programme (current accounts of its lines and accounts placed by its
-- cycles), read by the EB reports to join the booked invoices.
create view eb_programme_arn_v as
select l.programme_id, l.current_arn as arn, l.benefit_line
from eb_programme_line l
where l.current_arn is not null
union
select c.programme_id, ca.arn, null
from eb_cycle_account ca
join eb_cycle c on c.id = ca.cycle_id;
