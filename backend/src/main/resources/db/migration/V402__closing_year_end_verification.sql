-- =====================================================================================
-- iNXT BrokerVerse - V402 Year-end close verification (BRD-5 FRBS 2.7.1, wave A1-GL).
-- After the close, the nominal (income and expense) balances as of the year end and the trial
-- balance difference are computed and stored on the close record: both must be zero.
-- The parameters CLOSE_ONLY_PREVIOUS_MONTH and YEAR_END_CLOSE_DEADLINE are seeded in V890.
-- =====================================================================================
alter table yec_year_end_close add column nominal_balance numeric(19, 2);
alter table yec_year_end_close add column tb_difference numeric(19, 2);
alter table yec_year_end_close add column verified boolean;
alter table yec_year_end_close add column verified_at timestamptz;
