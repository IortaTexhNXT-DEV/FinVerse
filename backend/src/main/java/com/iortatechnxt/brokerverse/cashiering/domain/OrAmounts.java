package com.iortatechnxt.brokerverse.cashiering.domain;

import java.math.BigDecimal;

/**
 * Gross, output VAT and withholding tax of an official receipt or one of its lines (CSHID.002).
 *
 * @param gross gross amount
 * @param vat output VAT
 * @param wtax creditable withholding tax
 */
public record OrAmounts(BigDecimal gross, BigDecimal vat, BigDecimal wtax) {

  /**
   * Net amount received.
   *
   * @return gross + VAT - withholding tax
   */
  public BigDecimal net() {
    return gross.add(vat).subtract(wtax);
  }

  /**
   * Adds two amounts.
   *
   * @param other other amounts
   * @return sum
   */
  public OrAmounts plus(OrAmounts other) {
    return new OrAmounts(gross.add(other.gross), vat.add(other.vat), wtax.add(other.wtax));
  }
}
