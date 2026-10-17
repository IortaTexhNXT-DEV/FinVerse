-- Report subscriptions (Sprint 3, G04): the time of day of a run (business time; empty for the
-- default 06:00), the last working day of the month as a frequency (holiday calendar of the head
-- office of the company of the report), and a relative date period (previous day, month to date,
-- previous month, quarter to date, previous quarter, year to date, previous year) that sets the
-- date parameters of each run from its business date.
alter table report_subscription add column run_time time;
alter table report_subscription add column date_period varchar(20);
alter table report_subscription alter column frequency type varchar(20);
alter table report_subscription drop constraint ck_report_subscription_frequency;
alter table report_subscription add constraint ck_report_subscription_frequency
    check (frequency in ('DAILY', 'WEEKLY', 'MONTHLY', 'LAST_WORKING_DAY'));
alter table report_subscription add constraint ck_report_subscription_period
    check (date_period is null or date_period in ('PREVIOUS_DAY', 'MONTH_TO_DATE', 'PREVIOUS_MONTH',
        'QUARTER_TO_DATE', 'PREVIOUS_QUARTER', 'YEAR_TO_DATE', 'PREVIOUS_YEAR'));
