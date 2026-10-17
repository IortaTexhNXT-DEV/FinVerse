-- =====================================================================================
-- iNXT BrokerVerse - V1151 Platform party code of the broker itself (range V1150-V1159). A claim
-- status that waits on the broker (Claims Setup, attribute waiting_on) names the party BROKER, a
-- platform code; screens show the short name of the company (client profile, V1150). The value
-- written by V1020 under the client's name is renamed. The history of attribute changes keeps the
-- value that was recorded at the time.
-- =====================================================================================

update bcl_lov_attribute
set value = 'BROKER', updated_at = now(), updated_by = 'SYSTEM'
where attribute = 'waiting_on'
  and value = 'BDOI';
