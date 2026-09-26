package com.iortatechnxt.brokerverse.claims.domain;

import java.math.BigDecimal;

/**
 * Company share of a claim's totals.
 *
 * @param estimate payment estimate (loss and expense)
 * @param paid amount settled (loss and expense)
 * @param recovered amount recovered
 */
public record OurShare(BigDecimal estimate, BigDecimal paid, BigDecimal recovered) {

  /**
   * Outstanding reserve: estimate - paid.
   *
   * @return outstanding
   */
  public BigDecimal outstanding() {
    return estimate.subtract(paid);
  }
}
