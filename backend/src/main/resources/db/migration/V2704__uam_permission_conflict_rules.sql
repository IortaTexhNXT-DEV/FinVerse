-- Conflicting permission combinations (BDOI FRS FRUM.006.03; conflict C16): a separation-of-duties
-- rule names either two group profiles one user may not hold (as before) or two permissions that
-- may not be held together, neither in one group profile nor through the profiles of one user. The
-- permission pairs are maintained like the other rules (Business Administrator, authorised by
-- Information Security); BDOI lists the pairs it means.
alter table nba_sod_rule
    add column rule_kind varchar(15) not null default 'PROFILES',
    add constraint ck_nba_sod_rule_kind check (rule_kind in ('PROFILES', 'PERMISSIONS'));
