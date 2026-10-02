-- =====================================================================================
-- iNXT BrokerVerse - V1250 Renewal stage names as the screens show them (range V1250-V1259).
-- The Renewal FRS (BRD-06, table "Stages of RNW_CASE", column Screen label) names two stages
-- differently from the workflow set-up of V1010:
--   RA_SENT  "RA Sent / Awaiting Response" (was "Awaiting Response")
--   NB_PATH  "For Proposal / New Business Path" (was "New Business Path")
-- The stage codes are unchanged; only the names users read follow the FRS.
-- =====================================================================================
update wf_stage
set name = 'RA Sent / Awaiting Response'
where workflow_code = 'RNW_CASE'
  and stage_code = 'RA_SENT';

update wf_stage
set name = 'For Proposal / New Business Path'
where workflow_code = 'RNW_CASE'
  and stage_code = 'NB_PATH';
