package com.iortatechnxt.brokerverse.adjustment.domain;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * Before and after of one invoice component (ADJID.014 recompute, ADJID.022 old and new details).
 *
 * @param component ledger component (premium components, DTIP, commission, VAT on commission)
 * @param before amount due before the request (booked plus earlier adjustments)
 * @param delta signed change of the request
 */
@Embeddable
public record ComponentChange(
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) LedgerComponent component,
    @Column(name = "before_amount", nullable = false, precision = 19, scale = 2) BigDecimal before,
    @Column(name = "delta_amount", nullable = false, precision = 19, scale = 2) BigDecimal delta) {

  /**
   * Amount after the request.
   *
   * @return before + delta
   */
  public BigDecimal after() {
    return before.add(delta);
  }
}
