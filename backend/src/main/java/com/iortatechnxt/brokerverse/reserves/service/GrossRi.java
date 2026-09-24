package com.iortatechnxt.brokerverse.reserves.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;

/**
 * A gross reserve amount and the reinsurers' share of it.
 *
 * @param gross gross amount
 * @param ri reinsurers' share
 */
public record GrossRi(BigDecimal gross, BigDecimal ri) {

  /**
   * Zero amounts.
   *
   * @return zero
   */
  public static GrossRi zero() {
    return new GrossRi(Money.zero(), Money.zero());
  }

  /**
   * Sum of two amounts.
   *
   * @param other other amount
   * @return sum
   */
  public GrossRi plus(GrossRi other) {
    return new GrossRi(gross.add(other.gross), ri.add(other.ri));
  }

  /**
   * Both amounts multiplied by a percentage, rounded to cents.
   *
   * @param pct percentage (12.5 = 12.5 %)
   * @return scaled amounts
   */
  public GrossRi percent(BigDecimal pct) {
    return new GrossRi(Percent.of(gross, pct), Percent.of(ri, pct));
  }

  /**
   * Net amount.
   *
   * @return gross − reinsurers' share
   */
  public BigDecimal net() {
    return gross.subtract(ri);
  }
}
