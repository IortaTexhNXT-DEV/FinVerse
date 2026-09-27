package com.iortatechnxt.brokerverse.reinsurance.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.time.LocalDate;

/**
 * Quarter covered by a statement of account and the statement date.
 *
 * @param year calendar year
 * @param quarter quarter 1-4
 * @param statementDate date printed on the statement (and accounting date of its adjustments)
 */
public record SoaPeriod(int year, int quarter, LocalDate statementDate) {

  private static final int QUARTERS = 4;
  private static final int MONTHS_PER_QUARTER = 3;

  /** Canonical constructor validating the quarter. */
  public SoaPeriod {
    if (quarter < 1 || quarter > QUARTERS) {
      throw new BusinessRuleException("SOA_QUARTER", "Quarter must be 1 to 4");
    }
  }

  /**
   * First day of the quarter.
   *
   * @return date
   */
  public LocalDate from() {
    return LocalDate.of(year, (quarter - 1) * MONTHS_PER_QUARTER + 1, 1);
  }

  /**
   * Last day of the quarter.
   *
   * @return date
   */
  public LocalDate to() {
    return from().plusMonths(MONTHS_PER_QUARTER).minusDays(1);
  }

  /**
   * The same quarter one year earlier (premium reserves are held for twelve months).
   *
   * @return period
   */
  public SoaPeriod yearBefore() {
    return new SoaPeriod(year - 1, quarter, statementDate.minusYears(1));
  }

  /**
   * The previous quarter.
   *
   * @return period
   */
  public SoaPeriod previous() {
    return quarter == 1
        ? new SoaPeriod(year - 1, QUARTERS, from().minusDays(1))
        : new SoaPeriod(year, quarter - 1, from().minusDays(1));
  }
}
