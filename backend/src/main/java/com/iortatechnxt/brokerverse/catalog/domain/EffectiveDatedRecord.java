package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maker-checker rate row valid from {@code effectiveFrom} to {@code effectiveTo} (inclusive, open
 * ended when null). Rates are kept as dated rows so a change never rewrites history (Appendix A:
 * rates are data, not constants).
 */
@MappedSuperclass
public abstract class EffectiveDatedRecord extends AuthorizableEntity implements CatalogRecord {

  /** Largest percentage accepted for a rate. */
  protected static final BigDecimal MAX_PERCENT = BigDecimal.valueOf(100);

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  /** For JPA. */
  protected EffectiveDatedRecord() {}

  /**
   * Creates a row valid over a period.
   *
   * @param from first valid date
   * @param to last valid date, null when open ended
   */
  protected EffectiveDatedRecord(LocalDate from, LocalDate to) {
    requireValidPeriod(from, to);
    this.effectiveFrom = from;
    this.effectiveTo = to;
  }

  /**
   * Changes the validity period.
   *
   * @param from first valid date
   * @param to last valid date, null when open ended
   */
  protected final void setEffectivity(LocalDate from, LocalDate to) {
    requireValidPeriod(from, to);
    this.effectiveFrom = from;
    this.effectiveTo = to;
  }

  private static void requireValidPeriod(LocalDate from, LocalDate to) {
    if (from == null) {
      throw new BusinessRuleException("EFFECTIVE_FROM_REQUIRED", "Enter the effective-from date");
    }
    if (to != null && to.isBefore(from)) {
      throw new BusinessRuleException(
          "EFFECTIVITY_INVALID", "The effective-to date is before the effective-from date");
    }
  }

  /**
   * Whether the row applies on a date: authorized, active and within its validity.
   *
   * @param date business date
   * @return true when usable
   */
  public boolean isEffectiveOn(LocalDate date) {
    return isActive()
        && !date.isBefore(effectiveFrom)
        && (effectiveTo == null || !date.isAfter(effectiveTo));
  }

  /**
   * Validates a percentage (0 to 100).
   *
   * @param rate rate
   * @param label field label for the message
   * @return the rate
   */
  protected static BigDecimal requirePercent(BigDecimal rate, String label) {
    if (rate == null || rate.signum() < 0 || rate.compareTo(MAX_PERCENT) > 0) {
      throw new BusinessRuleException(
          "RATE_INVALID", label + " must be a percentage between 0 and 100");
    }
    return rate;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }
}
