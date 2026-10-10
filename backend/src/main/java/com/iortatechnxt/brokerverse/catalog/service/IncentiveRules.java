package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;

/**
 * The Incentive Eligible rules of BDOI's FRS (FRPM.003.02, Annex A and E): when the indicator is
 * Yes, an incentive amount (greater than zero) or an incentive commission rate (above 0 % and not
 * above 100 %) is required.
 */
public final class IncentiveRules {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private IncentiveRules() {}

  /**
   * Checks the incentive of a record.
   *
   * @param eligible Incentive Eligible (null is No)
   * @param amount incentive amount
   * @param rate incentive commission rate
   */
  public static void check(Boolean eligible, BigDecimal amount, BigDecimal rate) {
    checkValues(amount, rate);
    if (Boolean.TRUE.equals(eligible) && amount == null && rate == null) {
      throw new BusinessRuleException(
          "PKG_INCENTIVE_REQUIRED",
          "An incentive-eligible record needs the incentive amount or the incentive commission rate");
    }
  }

  private static void checkValues(BigDecimal amount, BigDecimal rate) {
    if (amount != null && amount.signum() <= 0) {
      throw new BusinessRuleException(
          "PKG_INCENTIVE_AMOUNT", "The incentive amount must be greater than zero");
    }
    if (rate != null && (rate.signum() <= 0 || rate.compareTo(HUNDRED) > 0)) {
      throw new BusinessRuleException(
          "PKG_INCENTIVE_RATE", "The incentive commission rate must be above 0% and at most 100%");
    }
  }
}
