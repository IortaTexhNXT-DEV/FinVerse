package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.util.Money;
import java.math.BigDecimal;

/**
 * Two aggregated amounts (query projection); null sums become zero.
 *
 * @param first first amount
 * @param second second amount
 */
public record AmountPair(BigDecimal first, BigDecimal second) {

  /** Canonical constructor replacing null sums by zero. */
  public AmountPair {
    first = Money.round(Money.nz(first));
    second = Money.round(Money.nz(second));
  }
}
