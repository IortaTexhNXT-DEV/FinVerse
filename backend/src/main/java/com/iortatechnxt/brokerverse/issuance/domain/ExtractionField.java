package com.iortatechnxt.brokerverse.issuance.domain;

/** Policy data read from an e-policy (BRNB.104). */
public enum ExtractionField {
  /** Policy number (several for a multi-year account, BRNB.112). */
  POLICY_NUMBER,
  /** Start of the period of insurance. */
  PERIOD_FROM,
  /** End of the period of insurance. */
  PERIOD_TO,
  /** Total (gross) premium. */
  PREMIUM
}
