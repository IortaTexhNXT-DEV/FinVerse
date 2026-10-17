-- =====================================================================================
-- SEED DATA (seed profile only): inter-company relationship FVI <-> FVS, consolidation group
-- FVGRP (FVI parent, 80 % of FVS) and month-end USD AVERAGE rates (year-to-date average of the
-- daily SPOT rates) used to translate the subsidiary's income statement items. Other currencies
-- fall back to the mean of the SPOT rates at run time.
-- =====================================================================================

insert into ic_relationship (company_a_id, company_b_id, a_due_from_account, a_due_to_account,
    b_due_from_account, b_due_to_account, active, created_at, created_by)
select a.id, b.id, '1607', '2510', '1607', '2510', true, now(), 'SYSTEM'
from org_company a, org_company b
where a.code = 'FVI' and b.code = 'FVS';

insert into con_group (code, name, parent_company_id, currency, cta_account, nci_account,
    goodwill_account, active, created_at, created_by)
select 'FVGRP', 'BDOI Group', c.id, 'PHP', '3450', '3600', '1850', true, now(), 'SYSTEM'
from org_company c where c.code = 'FVI';

insert into con_group_member (group_id, company_id, ownership_pct, investment_account, equity_accounts)
select g.id, c.id, 80, '1506', '3100'
from con_group g, org_company c
where g.code = 'FVGRP' and c.code = 'FVS';

insert into cur_exchange_rate (currency_code, rate_type, effective_date, rate, created_at, created_by)
select r.currency_code, 'AVERAGE', m.month_end, round(avg(r.rate), 6), now(), 'SYSTEM'
from generate_series(1, 9) as g(mo)
cross join lateral (select (make_date(2026, g.mo, 1) + interval '1 month - 1 day')::date as month_end) m
join cur_exchange_rate r
  on r.currency_code = 'USD' and r.rate_type = 'SPOT' and r.effective_date between date '2026-01-01' and m.month_end
group by r.currency_code, m.month_end
on conflict (currency_code, rate_type, effective_date) do nothing;
