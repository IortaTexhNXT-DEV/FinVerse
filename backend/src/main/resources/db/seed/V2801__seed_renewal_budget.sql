-- =====================================================================================
-- iNXT BrokerVerse - V2801 Seed Renewal Annual Budget (seed profile only; SIT/UAT data, never
-- loaded in production): the renewal, new and organic budgets of fiscal years 2026 and 2027 for the
-- Retail (region NCR, officer ao), CBG (region NCR, no officer) and Corporate Banking (team T-CORP1,
-- officer ao2) units of the seed company, in basic premium and in gross commission, so that the
-- production widget of the Renewal dashboard shows actual against budget.
-- =====================================================================================

insert into rnw_budget (company_id, fiscal_year, measure, segment, region, team, sub_team, unit_head,
    section_head, team_head, team_lead, account_officer, natural_key, created_at, created_by)
select c.id, y.fy, m.measure, b.segment, b.region, b.team, b.sub_team, b.unit_head, null, null,
       b.team_lead, b.officer,
       concat_ws('|', y.fy::text, m.measure, b.segment, coalesce(b.region, ''), coalesce(b.team, ''),
                 coalesce(b.sub_team, ''), coalesce(upper(b.officer), '')),
       now(), 'SYSTEM'
from org_company c
cross join (values (2026), (2027)) as y(fy)
cross join (values ('PREMIUM'), ('COMMISSION')) as m(measure)
cross join (values ('RETAIL', 'NCR', null, null, 'mkttl', 'mkttl', 'ao'),
                   ('CBG', 'NCR', null, null, 'mkttl', 'mkttl', null),
                   ('CORBANK', null, 'T-CORP1', 'T-CORP1-A', 'rnwtl', 'rnwtl', 'ao2'))
     as b(segment, region, team, sub_team, unit_head, team_lead, officer)
where c.code = 'FVI'
  and not exists (select 1 from rnw_budget x where x.company_id = c.id
                  and x.natural_key = concat_ws('|', y.fy::text, m.measure, b.segment, coalesce(b.region, ''),
                      coalesce(b.team, ''), coalesce(b.sub_team, ''), coalesce(upper(b.officer), '')));

-- Monthly amounts: basic premium budgets by segment, commission at 15% of premium; renewal months
-- weighted to the second half of the year.
insert into rnw_budget_month (budget_id, month_no, new_amount, renewal_amount, organic_amount)
select b.id, mo.n,
       round(base.amount * 0.30 * (case when b.measure = 'COMMISSION' then 0.15 else 1 end), 2),
       round(base.amount * (1 + (case when mo.n > 6 then 0.20 else 0 end))
             * (case when b.measure = 'COMMISSION' then 0.15 else 1 end), 2),
       round(base.amount * 0.12 * (case when b.measure = 'COMMISSION' then 0.15 else 1 end), 2)
from rnw_budget b
join (values ('RETAIL', 1200000.00), ('CBG', 2500000.00), ('CORBANK', 4000000.00))
     as base(segment, amount) on base.segment = b.segment
cross join generate_series(1, 12) as mo(n)
where b.created_by = 'SYSTEM'
  and not exists (select 1 from rnw_budget_month x where x.budget_id = b.id and x.month_no = mo.n);
