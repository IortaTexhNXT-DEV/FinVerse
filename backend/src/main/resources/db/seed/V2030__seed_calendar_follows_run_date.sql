-- =====================================================================================
-- iNXT BrokerVerse - V2030 Seed calendar and exchange rates follow the date this script runs.
-- SEED DATA ONLY - never load in production.
-- V900 (FVI) and V960 (FVS) seed fiscal year 2026 with January-September OPEN and the rest FUTURE,
-- and V900 / V961 / V990 seed the SPOT, CLOSING, BOOK and AVERAGE rates up to 30 September 2026,
-- so postings dated after September were refused. This script, run on the date of the deployment:
--   1. opens every FUTURE period of the seed companies up to and including the current month
--      (periods in any other status, CLOSED included, are left as they are);
--   2. creates the later calendar fiscal years up to the current year the way V900 / V960 create
--      2026: past and current months OPEN, later months FUTURE;
--   3. extends the SPOT and CLOSING rates (same currencies and formula as V900) from 1 October 2026
--      to 90 days after the run date, the BOOK rates from them (V990) and the month-end USD AVERAGE
--      rates up to the current month (year-to-date mean of the SPOT rates, as V961).
-- Every statement skips what already exists, so it is safe on a database that already has some of it.
-- The run date is the later of the database date and the Manila date, so a database kept in UTC
-- still opens the month the application posts in during the first hours of a Manila month.
-- =====================================================================================

-- ---------- 2. Later fiscal years and their periods (before step 1, so it also opens them) -------
insert into per_fiscal_year (company_id, year_code, start_date, end_date, status, created_at, created_by)
select c.id, y, make_date(y, 1, 1), make_date(y, 12, 31), 'OPEN', now(), 'SYSTEM'
from org_company c
cross join generate_series(2027,
    extract(year from greatest(current_date, (now() at time zone 'Asia/Manila')::date))::int) as y
where c.code in ('FVI', 'FVS')
  and exists (select 1 from per_fiscal_year f where f.company_id = c.id and f.year_code = 2026)
on conflict (company_id, year_code) do nothing;

insert into per_period (fiscal_year_id, company_id, period_no, name, start_date, end_date, status,
    created_at, created_by)
select y.id, y.company_id, m, to_char(make_date(y.year_code, m, 1), 'YYYY-MM'), make_date(y.year_code, m, 1),
       (make_date(y.year_code, m, 1) + interval '1 month - 1 day')::date,
       case when make_date(y.year_code, m, 1)
                 <= greatest(current_date, (now() at time zone 'Asia/Manila')::date)
            then 'OPEN' else 'FUTURE' end,
       now(), 'SYSTEM'
from per_fiscal_year y
join org_company c on c.id = y.company_id and c.code in ('FVI', 'FVS'),
     generate_series(1, 12) as m
where y.year_code >= 2027
  and y.start_date = make_date(y.year_code, 1, 1)
on conflict (fiscal_year_id, period_no) do nothing;

-- ---------- 1. Open the seed periods up to and including the current month ----------------------
update per_period p
set status = 'OPEN',
    status_changed_by = 'SYSTEM',
    status_changed_at = now(),
    status_reason = 'Seed calendar opened up to the month of the deployment',
    version = p.version + 1,
    updated_at = now(),
    updated_by = 'SYSTEM'
from org_company c, per_fiscal_year y
where c.id = p.company_id
  and c.code in ('FVI', 'FVS')
  and y.id = p.fiscal_year_id
  and y.status <> 'CLOSED'
  and p.status = 'FUTURE'
  and p.start_date <= greatest(current_date, (now() at time zone 'Asia/Manila')::date);

-- ---------- 3. Exchange rates from 1 October 2026 to 90 days after the run date -----------------
insert into cur_exchange_rate (currency_code, rate_type, effective_date, rate, created_at, created_by)
select r.ccy, t.rt, d::date, round((r.base * (1 + 0.004 * sin(extract(doy from d) / 9.0)))::numeric, 6), now(), 'SYSTEM'
from (values ('USD', 57.85), ('EUR', 62.40), ('GBP', 73.10), ('JPY', 0.3850), ('SGD', 43.20),
             ('HKD', 7.41), ('AUD', 37.60), ('CNY', 8.02), ('INR', 0.6890)) as r(ccy, base),
     (values ('SPOT'), ('CLOSING')) as t(rt),
     generate_series(date '2026-10-01',
                     greatest(current_date, (now() at time zone 'Asia/Manila')::date) + 90,
                     interval '1 day') as d
where exists (select 1 from cur_currency k where k.code = r.ccy)
on conflict (currency_code, rate_type, effective_date) do nothing;

insert into cur_exchange_rate (currency_code, rate_type, effective_date, rate, created_at, created_by)
select s.currency_code, 'BOOK', s.effective_date, round(s.rate, 2), now(), 'SYSTEM'
from cur_exchange_rate s
where s.rate_type = 'SPOT'
  and s.effective_date >= date '2026-10-01'
  and round(s.rate, 2) > 0
on conflict (currency_code, rate_type, effective_date) do nothing;

insert into cur_exchange_rate (currency_code, rate_type, effective_date, rate, created_at, created_by)
select r.currency_code, 'AVERAGE', m.month_end, round(avg(r.rate), 6), now(), 'SYSTEM'
from generate_series(date '2026-10-01',
                     date_trunc('month', greatest(current_date, (now() at time zone 'Asia/Manila')::date)),
                     interval '1 month') as g(month_start)
cross join lateral (select (g.month_start + interval '1 month - 1 day')::date as month_end) m
join cur_exchange_rate r
  on r.currency_code = 'USD' and r.rate_type = 'SPOT'
 and r.effective_date between date_trunc('year', m.month_end)::date and m.month_end
group by r.currency_code, m.month_end
on conflict (currency_code, rate_type, effective_date) do nothing;
