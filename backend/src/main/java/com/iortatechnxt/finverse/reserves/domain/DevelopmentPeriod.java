package com.iortatechnxt.finverse.reserves.domain;

import java.time.LocalDate;

/** Length of the accident and development periods of a claims triangle. */
public enum DevelopmentPeriod {
  /** Calendar years. */
  YEAR(12),
  /** Calendar quarters. */
  QUARTER(3);

  private static final int MONTHS_PER_YEAR = 12;

  private final int months;

  DevelopmentPeriod(int months) {
    this.months = months;
  }

  /**
   * Sequential index of the period containing a date (comparable across years).
   *
   * @param date date
   * @return index, e.g. year × periods per year + period of the year
   */
  public int index(LocalDate date) {
    return date.getYear() * (MONTHS_PER_YEAR / months) + (date.getMonthValue() - 1) / months;
  }

  /**
   * First day of the period with an index.
   *
   * @param index index from {@link #index(LocalDate)}
   * @return first day
   */
  public LocalDate start(int index) {
    int perYear = MONTHS_PER_YEAR / months;
    return LocalDate.of(index / perYear, index % perYear * months + 1, 1);
  }

  /**
   * Label of the period with an index, e.g. "2026" or "2026-Q3".
   *
   * @param index index
   * @return label
   */
  public String label(int index) {
    LocalDate start = start(index);
    return this == YEAR
        ? Integer.toString(start.getYear())
        : start.getYear() + "-Q" + ((start.getMonthValue() - 1) / months + 1);
  }
}
