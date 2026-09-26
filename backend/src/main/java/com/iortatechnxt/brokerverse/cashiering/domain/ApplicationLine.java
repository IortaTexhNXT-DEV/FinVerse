package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/** The amount of an application on one premium component (CSHID.022). */
@Embeddable
public class ApplicationLine {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LedgerComponent component;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  protected ApplicationLine() {}

  /**
   * Creates a line.
   *
   * @param component premium component
   * @param amount amount applied
   */
  public ApplicationLine(LedgerComponent component, BigDecimal amount) {
    this.component = component;
    this.amount = amount;
  }

  public LedgerComponent getComponent() {
    return component;
  }

  public BigDecimal getAmount() {
    return amount;
  }
}
