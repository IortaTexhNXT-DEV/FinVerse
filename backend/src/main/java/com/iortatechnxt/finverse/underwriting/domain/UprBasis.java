package com.iortatechnxt.finverse.underwriting.domain;

/** Method used to earn premium over the policy period (unearned premium reserve basis). */
public enum UprBasis {
  /** Daily pro-rata, 1/365 (the period's actual number of days). */
  DAYS_365,
  /** Monthly pro-rata, 1/24 (twenty-fourths method). */
  TWENTY_FOURTHS,
  /** Quarterly pro-rata, 1/8 (eighths method). */
  EIGHTHS
}
