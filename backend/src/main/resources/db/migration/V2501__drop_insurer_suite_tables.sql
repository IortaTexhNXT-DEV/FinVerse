-- =====================================================================================
-- iNXT BrokerVerse - V2501 Insurer suite removal (range V2500-V2509): tables of the insurer modules.
-- BIBS is specific to BDOI as an insurance broker (client decision of 8 October 2026,
-- docs/development/CODEBASE_RELEVANCE_AUDIT.md A1-A6). The code of the insurer modules is removed;
-- this migration drops their tables, children first:
--   * actuarial reserves (V420): rsv_*;
--   * reinsurance treaties, cessions, facultative placements and statements (V300, V301): ri_*;
--   * insurer claims (V200, immutability guard of clm_movement V1191): clm_*;
--   * underwriting products, quotations, open covers, policies and endorsements (V100, V101): uw_*;
--   * inter-company and group consolidation (V600): ic_*, con_*. The budget tables of V600 stay.
-- No table of a kept module references these tables. Journals the insurer modules posted stay in
-- the ledger.
-- Version: above every seed script (db/seed up to V2090), so on a new database the seed scripts
-- (V920-V961) run while these tables still exist.
-- =====================================================================================

-- Actuarial reserves
drop table rsv_takaful_line;
drop table rsv_upr_detail;
drop table rsv_run_line;
drop table rsv_valuation_run;
drop table rsv_takaful_setting;
drop table rsv_parameter;

-- Reinsurance
drop table ri_claim_share;
drop table ri_claim_movement;
drop table ri_soa;
drop table ri_cession_line;
drop table ri_fac_participant;
drop table ri_fac_placement;
drop table ri_cession;
drop table ri_treaty_layer;
drop table ri_treaty_participant;
drop table ri_treaty;

-- Insurer claims
drop table clm_movement;
drop table clm_lpo;
drop table clm_recovery;
drop table clm_settlement;
drop table clm_reserve_change;
drop table clm_claim_party;
drop table clm_claim;

-- Underwriting (a quotation and its policy reference each other)
alter table uw_quotation drop constraint fk_uw_quotation_policy;
drop table uw_endorsement;
drop table uw_policy_risk;
drop table uw_policy;
drop table uw_quotation_iteration;
drop table uw_quotation;
drop table uw_open_cover;
drop table uw_product;

-- Inter-company and consolidation
drop table con_run_line;
drop table con_run;
drop table con_group_member;
drop table con_group;
drop table ic_transaction;
drop table ic_relationship;
