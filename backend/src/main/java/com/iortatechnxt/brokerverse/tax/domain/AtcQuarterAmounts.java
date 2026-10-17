package com.iortatechnxt.brokerverse.tax.domain;

import java.math.BigDecimal;

/**
 * Income payments of one payee under one ATC for a quarter, split by month of the quarter — one
 * line of Part II of BIR Form 2307.
 *
 * @param atc alphanumeric tax code
 * @param incomeNature nature of the income payment
 * @param month1 income paid in the 1st month of the quarter
 * @param month2 income paid in the 2nd month
 * @param month3 income paid in the 3rd month
 * @param tax tax withheld for the quarter
 */
public record AtcQuarterAmounts(
    String atc,
    String incomeNature,
    BigDecimal month1,
    BigDecimal month2,
    BigDecimal month3,
    BigDecimal tax) {

  /**
   * Total income of the quarter.
   *
   * @return month1 + month2 + month3
   */
  public BigDecimal total() {
    return month1.add(month2).add(month3);
  }
}
