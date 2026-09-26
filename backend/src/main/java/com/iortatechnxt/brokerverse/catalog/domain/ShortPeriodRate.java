package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.catalog.domain.CommissionRate.RateValidity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Short-period (short-term) table row (Appendix A): the percentage of the annual premium charged
 * for a cover of a number of months, e.g. cancellation before expiry.
 */
@Entity
@Table(name = "cat_short_period_rate")
public class ShortPeriodRate extends EffectiveDatedRecord {

  /** Months in a year: the table covers 1 to 12 months. */
  public static final int MONTHS_IN_YEAR = 12;

  private static final String LABEL = "The short-period percentage";

  @Column(name = "months_covered", nullable = false, updatable = false)
  private int monthsCovered;

  @Column(name = "percent_of_annual", nullable = false, precision = 19, scale = 8)
  private BigDecimal percentOfAnnual;

  protected ShortPeriodRate() {}

  /**
   * Creates a row, pending authorization.
   *
   * @param monthsCovered months covered (1 to 12)
   * @param validity percent of annual premium and effectivity
   */
  public ShortPeriodRate(int monthsCovered, RateValidity validity) {
    super(validity.effectiveFrom(), validity.effectiveTo());
    this.monthsCovered = requireMonths(monthsCovered);
    this.percentOfAnnual = requirePercent(validity.rate(), LABEL);
  }

  private static int requireMonths(int months) {
    if (months < 1 || months > MONTHS_IN_YEAR) {
      throw new BusinessRuleException(
          "SHORT_PERIOD_MONTHS_INVALID", "Months covered must be between 1 and 12");
    }
    return months;
  }

  /**
   * Changes the percentage or its effectivity; it must be authorized again.
   *
   * @param validity percent and effectivity
   */
  public void update(RateValidity validity) {
    this.percentOfAnnual = requirePercent(validity.rate(), LABEL);
    setEffectivity(validity.effectiveFrom(), validity.effectiveTo());
    markModified();
  }

  @Override
  public String catalogReference() {
    return "SHORT " + monthsCovered + "M " + getEffectiveFrom();
  }

  @Override
  public String catalogDescription() {
    return monthsCovered
        + " month(s): "
        + percentOfAnnual.stripTrailingZeros().toPlainString()
        + " % of annual";
  }

  public int getMonthsCovered() {
    return monthsCovered;
  }

  public BigDecimal getPercentOfAnnual() {
    return percentOfAnnual;
  }
}
