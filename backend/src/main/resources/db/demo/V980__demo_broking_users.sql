-- =====================================================================================
-- iNXT BrokerVerse - V980 Demo users for the BDOI broking roles (password for all:
-- Brokerverse@2026). DEMO ONLY - never load in production.
-- Two users for roles with maker-checker steps (Marketing AO, TSU) so four-eyes flows can
-- be demonstrated.
-- =====================================================================================
insert into sec_user (username, full_name, email, password_hash, authorization_limit, home_branch_id,
    created_at, created_by)
select u.username, u.full_name, u.email,
       '$2b$12$Ymr.EsVEy57GidFnteUV2OU/MuNbjvTH4sjMM08B220/9bNvZgdp2', u.lim,
       (select id from org_branch where code = 'HO' order by id limit 1), now(), 'SYSTEM'
from (values ('ao',       'Aileen Account Officer',   'ao@brokerverse-demo.ph',       null::numeric),
             ('ao2',      'Arnel Account Officer',    'ao2@brokerverse-demo.ph',      null),
             ('mkttl',    'Marites Marketing Lead',   'mkttl@brokerverse-demo.ph',    null),
             ('tsu',      'Teodoro TSU Analyst',      'tsu@brokerverse-demo.ph',      null),
             ('tsulead',  'Teresa TSU Lead',          'tsulead@brokerverse-demo.ph',  null),
             ('proc',     'Paolo Processor',          'proc@brokerverse-demo.ph',     null),
             ('proctl',   'Patricia Processing Lead', 'proctl@brokerverse-demo.ph',   null),
             ('approver', 'Andres Approver',          'approver@brokerverse-demo.ph', 5000000),
             ('epol',     'Elena E-policy Sender',    'epol@brokerverse-demo.ph',     null),
             ('adjust',   'Adrian Adjustment',        'adjust@brokerverse-demo.ph',   null),
             ('badmin',   'Bea Business Admin',       'badmin@brokerverse-demo.ph',   null))
     as u(username, full_name, email, lim);

insert into sec_user_role (user_id, role_id)
select u.id, r.id from sec_user u join sec_role r on r.code = case u.username
    when 'ao' then 'MKT_AO' when 'ao2' then 'MKT_AO' when 'mkttl' then 'MKT_TL'
    when 'tsu' then 'TSU' when 'tsulead' then 'TSU' when 'proc' then 'PROCESSOR'
    when 'proctl' then 'PROCESSING_TL' when 'approver' then 'NB_APPROVER'
    when 'epol' then 'EPOLICY_SENDER' when 'adjust' then 'ADJUSTMENT'
    when 'badmin' then 'BUSINESS_ADMIN' end
where u.username in ('ao', 'ao2', 'mkttl', 'tsu', 'tsulead', 'proc', 'proctl', 'approver', 'epol',
                     'adjust', 'badmin');
