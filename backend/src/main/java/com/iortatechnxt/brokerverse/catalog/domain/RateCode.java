package com.iortatechnxt.brokerverse.catalog.domain;

/** Effective-dated rates of the premium calculation (Appendix A), in percent. */
public enum RateCode {
  /** Documentary stamp tax on the net / basic premium. */
  DST,
  /** Premium tax (Appendix A: fire PTX 12 % of net premium, Q43). */
  PREMIUM_TAX,
  /** Value added tax on the premium. */
  VAT_PREMIUM,
  /** Fire service tax. */
  FIRE_SERVICE_TAX,
  /** VAT on the broker's commission. */
  VAT_COMMISSION,
  /** Motor OD/Theft coverage as % of TSI, annual cover (90 %). */
  MOTOR_OD_ANNUAL,
  /** Motor OD/Theft coverage as % of TSI, multi-year cover (81 %, Q35). */
  MOTOR_OD_MULTI_YEAR
}
