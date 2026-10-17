-- =====================================================================================
-- iNXT BrokerVerse - V1330 Renewal extension of the Walkthrough addendum (BRRN.037, 042 and the
-- updated BRRN.001; FR-RN-082, 083, 086). Renewal range V1010-V1019 is full; the extension of the
-- renewal for the BRD versions of 8-Oct-2026 uses V1330-V1339.
--   rnw_candidate     the hold cover of the renewal account (status and confirmed end, the
--                     effective expiry date R37-HC-01 to 04), the closing letter the renewal is
--                     routed to at its effective expiry (NAL to Operations when an RA was sent,
--                     NRL to the Marketing AO otherwise, R37-HC-07 to 10) and the closing letter
--                     sent (NAL and NRL exclude each other)
--   RNW_HOLD_COVER_DAYS     durations offered for a renewal hold cover (30, 60)
--   RNW_NRNS_WAITING_DAYS   days after the effective expiry date before the NRNS tag, per segment
-- =====================================================================================
alter table rnw_candidate add column hold_cover_status varchar(20);
alter table rnw_candidate add column hold_cover_until date;
alter table rnw_candidate add column closing_route varchar(10)
    constraint ck_rnw_candidate_closing_route check (closing_route in ('NAL', 'NRL'));
alter table rnw_candidate add column closing_letter varchar(10)
    constraint ck_rnw_candidate_closing_letter check (closing_letter in ('NAL', 'NFR'));

insert into sys_parameter (param_key, param_value, value_type, category, description, min_value,
    max_value, created_at, created_by)
values ('RNW_HOLD_COVER_DAYS', '30,60', 'INTEGER_LIST', 'RENEWAL',
        'Durations in days offered for the hold cover of a renewal; the first is the default',
        null, null, now(), 'SYSTEM'),
       ('RNW_NRNS_WAITING_DAYS', 'CBG=0,*=0', 'STRING', 'RENEWAL',
        'Days after the effective expiry date before an unrenewed renewal is tagged NRNS, per segment',
        null, null, now(), 'SYSTEM')
on conflict (param_key) do nothing;
