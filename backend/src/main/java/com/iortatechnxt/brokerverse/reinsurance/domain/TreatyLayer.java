package com.iortatechnxt.brokerverse.reinsurance.domain;

import com.iortatechnxt.brokerverse.common.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * One layer of an excess of loss treaty: losses above the priority are recovered up to the limit,
 * per claim; the yearly aggregate is the limit times (1 + reinstatements). Owned by {@link Treaty}.
 */
@Embeddable
public class TreatyLayer {

  @Column(name = "layer_no", nullable = false)
  private int layerNo;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal priority;

  @Column(name = "layer_limit", nullable = false, precision = 19, scale = 2)
  private BigDecimal layerLimit;

  @Column(name = "min_deposit_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal minDepositPremium;

  @Column(nullable = false)
  private int reinstatements;

  /** For JPA. */
  protected TreatyLayer() {}

  /**
   * Creates a layer.
   *
   * @param layerNo layer number (1 = lowest)
   * @param priority retention per claim (deductible)
   * @param layerLimit cover per claim above the priority
   * @param minDepositPremium minimum and deposit premium for the year
   * @param reinstatements number of reinstatements of the limit
   */
  public TreatyLayer(
      int layerNo,
      BigDecimal priority,
      BigDecimal layerLimit,
      BigDecimal minDepositPremium,
      int reinstatements) {
    this.layerNo = layerNo;
    this.priority = Money.round(priority);
    this.layerLimit = Money.round(layerLimit);
    this.minDepositPremium = Money.round(Money.nz(minDepositPremium));
    this.reinstatements = reinstatements;
  }

  /**
   * Yearly aggregate cover: the limit reinstated {@code reinstatements} times.
   *
   * @return aggregate limit
   */
  public BigDecimal aggregateLimit() {
    return layerLimit.multiply(BigDecimal.valueOf(1L + reinstatements));
  }

  public int getLayerNo() {
    return layerNo;
  }

  public BigDecimal getPriority() {
    return priority;
  }

  public BigDecimal getLayerLimit() {
    return layerLimit;
  }

  public BigDecimal getMinDepositPremium() {
    return minDepositPremium;
  }

  public int getReinstatements() {
    return reinstatements;
  }
}
