-- =====================================================================================
-- iNXT BrokerVerse - V989 Seed New Business reports (seed profile only).
--   * The seed company becomes a broker: "BDO Insurance and Reinsurance Brokers, Inc." (code FVI kept,
--     so every seed reference stays valid), and its Singapore subsidiary likewise.
--   * Monthly production targets for 2026 per region, department, team and account officer of
--     the V982 sales organisation (BRNB.075). BDOI has not given its targets yet (Q41): these
--     are sample values only.
--   * One shared saved variant of the Account Status Report (BRNB.057).
-- =====================================================================================

update org_company set name = 'BDO Insurance and Reinsurance Brokers, Inc.' where code = 'FVI';
update org_company set name = 'BDO Insurance and Reinsurance Brokers (Singapore) Pte. Ltd.' where code = 'FVS';

insert into nbr_sales_target (company_id, unit_level, unit_code, period_from, period_to,
    target_count, target_premium, target_commission, created_at, created_by)
select c.id, t.lvl, t.unit, m.month_start, (m.month_start + interval '1 month - 1 day')::date,
       t.cnt, t.premium, t.commission, now(), 'SYSTEM'
from org_company c
cross join (values
    ('REGION', 'NCR', 14, 300000.00, 60000.00),
    ('REGION', 'VIS', 4, 80000.00, 16000.00),
    ('DEPARTMENT', 'CBG-NCR', 10, 180000.00, 36000.00),
    ('DEPARTMENT', 'CORP-NCR', 4, 120000.00, 24000.00),
    ('DEPARTMENT', 'CBG-VIS', 4, 80000.00, 16000.00),
    ('TEAM', 'T-CBG1', 10, 180000.00, 36000.00),
    ('TEAM', 'T-CORP1', 4, 120000.00, 24000.00),
    ('TEAM', 'T-VIS1', 4, 80000.00, 16000.00),
    ('OFFICER', 'ao', 6, 110000.00, 22000.00),
    ('OFFICER', 'ao2', 4, 120000.00, 24000.00),
    ('OFFICER', 'mkttl', 4, 70000.00, 14000.00)
) as t(lvl, unit, cnt, premium, commission)
cross join (select cast(generate_series(date '2026-01-01', date '2026-12-01', interval '1 month')
                   as date) as month_start) m
where c.code = 'FVI';

insert into nbr_report_variant (owner, report_code, name, parameters, shared, created_at, created_by)
values ('mkttl', 'NB-ACC-STATUS', 'SLA breaches - all stages', '{"exceptions":"SLA_BREACH","status":"ALL"}',
        true, now(), 'mkttl');
