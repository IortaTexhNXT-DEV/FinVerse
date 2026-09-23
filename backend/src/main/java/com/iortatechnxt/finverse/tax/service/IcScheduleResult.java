package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A computed Insurance Commission schedule.
 *
 * @param schedule schedule
 * @param from period start (movement lines)
 * @param asOf period end / balance date
 * @param lines computed lines in print order (per line of business for LOB schedules)
 * @param total schedule total (Σ signed amounts)
 * @param rbc RBC summary figures, null for other schedules
 */
public record IcScheduleResult(
    IcSchedule schedule,
    LocalDate from,
    LocalDate asOf,
    List<IcScheduleLine> lines,
    BigDecimal total,
    RbcSummary rbc) {

  /** Canonical constructor copying the lines. */
  public IcScheduleResult {
    lines = List.copyOf(lines);
  }

  /**
   * RBC summary figures.
   *
   * @param netWorth net worth (total of the NET_WORTH schedule), the available capital
   * @param requirement total RBC requirement (Σ line requirements)
   * @param ratio net worth / requirement × 100, null when the requirement is zero
   * @param hurdle minimum ratio required by the regulator (percent)
   */
  public record RbcSummary(
      BigDecimal netWorth, BigDecimal requirement, BigDecimal ratio, BigDecimal hurdle) {}
}
