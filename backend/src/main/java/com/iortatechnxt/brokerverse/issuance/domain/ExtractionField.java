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
  PREMIUM,
  /** Assured name (submitted policies). */
  ASSURED,
  /** Promissory note number of the loan (submitted policies). */
  PN_NO,
  /** Insurer as written on the policy (submitted policies). */
  INSURER,
  /** Sum insured (submitted policies). */
  SUM_INSURED,
  /** Unit description of a motor policy (submitted policies). */
  UNIT,
  /** Serial or chassis number (submitted policies). */
  SERIAL_NO,
  /** Motor or engine number (submitted policies). */
  MOTOR_NO,
  /** Plate number (submitted policies). */
  PLATE_NO,
  /** Location of the risk (submitted policies). */
  LOCATION
}
