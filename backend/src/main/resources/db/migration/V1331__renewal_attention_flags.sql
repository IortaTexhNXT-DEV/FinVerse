-- =====================================================================================
-- iNXT BrokerVerse - V1331 Renewal: attention flags of the centralised listing without automatic
-- escalation (Walkthrough addendum BRRN.036 and its annex; FR-RN-102).
--   rnw_candidate          attention flag (AGEING, OVERDUE, HIGH_RISK) with the rule that set it
--                          and the day it was first set; the daily re-evaluation keeps it
--   RNW_PRIORITY_SEGMENTS  portfolio priority segments named in the flag rule (IBG, Leasing)
--   RNW_RENEWAL_AT_RISK    the alert to the unit head is switched off: BIBS never escalates an
--                          account by itself, the AO escalates outside the system
-- =====================================================================================
alter table rnw_candidate add column attention_flag varchar(20)
    constraint ck_rnw_candidate_attention check (attention_flag in ('AGEING', 'OVERDUE', 'HIGH_RISK'));
alter table rnw_candidate add column attention_rule varchar(200);
alter table rnw_candidate add column attention_since date;
create index ix_rnw_candidate_attention on rnw_candidate (company_id, attention_flag)
    where attention_flag is not null;

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by)
values ('RNW_PRIORITY_SEGMENTS', 'IBG,LEASING', 'CODE_LIST', 'RENEWAL',
        'Market segments of the priority portfolio named in the attention flags of the renewal listing',
        null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;

update alt_exception_code
set active = false,
    description = 'Not raised: renewals at risk are shown with their attention flag in the renewal listing; escalation is done by the Account Officer.'
where code = 'RNW_RENEWAL_AT_RISK';
