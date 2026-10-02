package com.iortatechnxt.brokerverse.reserves.domain;

import java.math.BigDecimal;

/**
 * Values of one reserve line.
 *
 * @param type reserve type
 * @param key reporting unit
 * @param gross gross reserve (company share, base currency)
 * @param ri reinsurers' share (for DAC: the unearned RI commission, UCR)
 * @param base calculation base where one applies (IBNR: earned premium), else null
 * @param rate rate applied where one applies (IBNR rate %, ceded share), else null
 * @param method method or basis used (IBNR: RATE / CHAIN_LADDER), else null
 */
public record ReserveLineValues(
    ReserveType type,
    ReserveKey key,
    BigDecimal gross,
    BigDecimal ri,
    BigDecimal base,
    BigDecimal rate,
    String method) {

  /**
   * Line without base, rate or method.
   *
   * @param type reserve type
   * @param key reporting unit
   * @param gross gross
   * @param ri reinsurers' share
   * @return values
   */
  public static ReserveLineValues of(
      ReserveType type, ReserveKey key, BigDecimal gross, BigDecimal ri) {
    return new ReserveLineValues(type, key, gross, ri, null, null, null);
  }

  /**
   * Net reserve.
   *
   * @return gross − reinsurers' share
   */
  public BigDecimal net() {
    return gross.subtract(ri);
  }
}
