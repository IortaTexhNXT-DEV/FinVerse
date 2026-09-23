package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.reserves.domain.ReserveLineValues;
import com.iortatechnxt.finverse.reserves.domain.ReserveType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Technical reserves summary of a valuation compared with the previous posted one, per reserve and
 * line of business. DAC and UCR are shown on separate rows: DAC (gross), UCR (reinsurers' side: RI
 * share = unearned RI commission, net = −UCR), so DAC + UCR net = net deferred acquisition cost.
 *
 * @param currentRunId run of the valuation (null when none)
 * @param currentDate its valuation date
 * @param currentStatus its status
 * @param previousRunId previous posted run (null when none)
 * @param previousDate its valuation date
 * @param rows rows by reserve then line of business
 */
public record ReserveSummary(
    Long currentRunId,
    LocalDate currentDate,
    String currentStatus,
    Long previousRunId,
    LocalDate previousDate,
    List<Row> rows) {

  /** Reserves in display order (UCR is derived from the DAC lines). */
  public static final List<String> RESERVES =
      List.of("UPR", "DAC", "UCR", "OSLR", "IBNR", "ULAE", "MFAD", "PDR");

  /** Canonical constructor copying the rows. */
  public ReserveSummary {
    rows = List.copyOf(rows);
  }

  /**
   * Builds the rows from the lines of two runs.
   *
   * @param current lines of the current run
   * @param previous lines of the previous run
   * @return rows by reserve and line of business
   */
  public static List<Row> rows(List<ReserveLineValues> current, List<ReserveLineValues> previous) {
    Map<String, Row> byKey = new TreeMap<>();
    current.forEach(l -> add(byKey, l, true));
    previous.forEach(l -> add(byKey, l, false));
    List<Row> out = new ArrayList<>(byKey.values());
    out.sort(
        Comparator.comparingInt((Row r) -> RESERVES.indexOf(r.reserve()))
            .thenComparing(Row::businessLine));
    return out;
  }

  private static void add(Map<String, Row> byKey, ReserveLineValues l, boolean current) {
    if (l.type() == ReserveType.DAC) {
      merge(byKey, "DAC", l.key().businessLine(), new GrossRi(l.gross(), BigDecimal.ZERO), current);
      merge(byKey, "UCR", l.key().businessLine(), new GrossRi(BigDecimal.ZERO, l.ri()), current);
    } else {
      merge(
          byKey, l.type().name(), l.key().businessLine(), new GrossRi(l.gross(), l.ri()), current);
    }
  }

  private static void merge(
      Map<String, Row> byKey, String reserve, String line, GrossRi amount, boolean current) {
    Row delta =
        current
            ? new Row(reserve, line, amount, GrossRi.zero())
            : new Row(reserve, line, GrossRi.zero(), amount);
    byKey.merge(reserve + "|" + line, delta, Row::plus);
  }

  /**
   * One summary row.
   *
   * @param reserve reserve code (UPR, DAC, UCR, OSLR, IBNR, ULAE, MFAD, PDR)
   * @param businessLine line of business
   * @param current current valuation (gross, RI share)
   * @param previous previous valuation (gross, RI share)
   */
  public record Row(String reserve, String businessLine, GrossRi current, GrossRi previous) {

    /**
     * Sum of two rows of the same reserve and line.
     *
     * @param other other row
     * @return sum
     */
    public Row plus(Row other) {
      return new Row(
          reserve, businessLine, current.plus(other.current), previous.plus(other.previous));
    }

    /**
     * Change in the net reserve.
     *
     * @return current net − previous net
     */
    public BigDecimal netChange() {
      return current.net().subtract(previous.net());
    }
  }
}
