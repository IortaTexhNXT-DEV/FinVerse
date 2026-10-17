package com.iortatechnxt.brokerverse.collections.installment.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.Arrays;

/**
 * The billing frequencies of LOV {@code CLX_BILLING_FREQUENCY} (BRCLXN.058) with the length of
 * their billing cycle. A value added to the LOV without a cycle length here cannot be used for a
 * plan until the code knows it (the frequencies to seed are CQ18).
 */
public enum BillingFrequency {
  /** One cycle per year. */
  ANNUAL(12),
  /** Two cycles per year. */
  SEMI_ANNUAL(6),
  /** Four cycles per year. */
  QUARTERLY(3),
  /** Twelve cycles per year. */
  MONTHLY(1);

  /** List of values holding the frequencies. */
  public static final String LOV = "CLX_BILLING_FREQUENCY";

  private final int months;

  BillingFrequency(int months) {
    this.months = months;
  }

  /**
   * Months of one billing cycle.
   *
   * @return months
   */
  public int months() {
    return months;
  }

  /**
   * The frequency of a LOV code.
   *
   * @param code LOV code
   * @return frequency
   */
  public static BillingFrequency of(String code) {
    return Arrays.stream(values())
        .filter(f -> f.name().equals(code))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "CLX_FREQUENCY_UNSUPPORTED",
                    "Billing frequency " + code + " has no cycle length defined"));
  }
}
