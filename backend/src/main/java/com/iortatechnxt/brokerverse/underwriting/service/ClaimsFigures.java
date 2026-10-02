package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Claims experience of one policy, company share, in the policy currency.
 *
 * @param claimCount number of claims registered
 * @param latestClaimNo number of the most recent claim, null when none
 * @param latestLossDate loss date of the most recent claim, null when none
 * @param reserve current estimate (reserve) of open claims
 * @param paid claims paid, net of recoveries
 * @param outstanding outstanding (reserve − paid) of open claims
 * @param netClaims incurred claims: paid + outstanding, net of recoveries
 */
public record ClaimsFigures(
    int claimCount,
    String latestClaimNo,
    LocalDate latestLossDate,
    BigDecimal reserve,
    BigDecimal paid,
    BigDecimal outstanding,
    BigDecimal netClaims) {

  /**
   * No claims.
   *
   * @return zero figures
   */
  public static ClaimsFigures none() {
    return new ClaimsFigures(0, null, null, Money.zero(), Money.zero(), Money.zero(), Money.zero());
  }
}
