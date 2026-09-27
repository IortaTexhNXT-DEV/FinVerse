package com.iortatechnxt.brokerverse.reinsurance.domain;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;

/**
 * An amount per claim (query projection).
 *
 * @param claimId claim
 * @param amount base currency amount (null sums become zero)
 */
public record ClaimAmount(Long claimId, BigDecimal amount) {

  /** Canonical constructor replacing a null sum by zero. */
  public ClaimAmount {
    amount = Money.round(Money.nz(amount));
  }
}
