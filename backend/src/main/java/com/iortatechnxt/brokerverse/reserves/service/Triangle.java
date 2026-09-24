package com.iortatechnxt.brokerverse.reserves.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cumulative claims development triangle. Row i is one accident period; its values are the
 * cumulative amounts at development periods 0, 1, ... up to the latest observed one, so rows get
 * shorter for recent accident periods.
 *
 * @param accidentLabels label of each accident period (oldest first)
 * @param rows cumulative amounts per accident period
 */
public record Triangle(List<String> accidentLabels, List<List<BigDecimal>> rows) {

  /** Canonical constructor copying the lists. */
  public Triangle {
    accidentLabels = List.copyOf(accidentLabels);
    rows = rows.stream().map(List::copyOf).toList();
  }

  /**
   * Number of development periods (length of the longest row).
   *
   * @return columns
   */
  public int developmentPeriods() {
    return rows.stream().mapToInt(List::size).max().orElse(0);
  }

  /**
   * Latest cumulative amount (the diagonal) of an accident period.
   *
   * @param row accident period index
   * @return latest amount, zero for an empty row
   */
  public BigDecimal latest(int row) {
    List<BigDecimal> r = rows.get(row);
    return r.isEmpty() ? BigDecimal.ZERO : r.get(r.size() - 1);
  }
}
