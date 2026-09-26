package com.iortatechnxt.brokerverse.brokerclaims.domain;

/** How a claim reached BDOI (CLAIMS_BROKING_DESIGN 5.1; BRCLM.041, CLQ13 / CLQ14). */
public enum ClaimSource {
  /** Notice of loss received by BDOI from the client or Marketing. */
  BDOI_NOTICE,
  /** Claim first reported by the insurer. */
  INSURER_REPORTED,
  /** Claim migrated from the legacy EBIX / ISYS records (parked, CLQ14). */
  MIGRATED
}
