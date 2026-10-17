package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

/**
 * Fields of a claim whose every change is kept in the claim timeline ({@code bcl_claim_event},
 * CLAIMS_BROKING_DESIGN 5.2; BRCLM.004/006/015/018/019/021/039, NFR p.37).
 */
public enum ClaimField {
  /** Reported date correction (BRCLM.004, CL1-A). */
  REPORTED_DATE,
  /** Claimant override (BRCLM.006, CL1-A). */
  CLAIMANT,
  /** Adjuster / appraiser of the claim (BRCLM.018). */
  ADJUSTER,
  /** Next follow-up date override (BRCLM.019). */
  FOLLOW_UP,
  /** Next action plan summary (BRCLM.021). */
  ACTION_PLAN,
  /** Requested type of settlement, amount and date (BRCLM.015). */
  SETTLEMENT,
  /** Cover version used by the claim (BRCLM.039, CL1-A). */
  COVER_VERSION,
  /** Claims authorization code (BRCLM.001, CL1-A). */
  AUTHORIZATION,
  /** Claims handler (reassignment, NFR p.37). */
  HANDLER,
  /** Plate number and preferred repair shop (FRCLM.008.02, 007.02). */
  MOTOR,
  /** Claim of an unpaid policy returned to the Account Officer (FRCLM.008.04). */
  RETURNED_TO_AO,
  /** Follow-up recorded with its action taken and next plan (FRCLM.009.04). */
  FOLLOW_UP_RECORDED
}
