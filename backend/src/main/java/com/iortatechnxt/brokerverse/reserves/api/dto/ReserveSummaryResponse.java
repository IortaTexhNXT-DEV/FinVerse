package com.iortatechnxt.brokerverse.reserves.api.dto;

import com.iortatechnxt.brokerverse.reserves.service.ReserveSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Technical reserves summary (dashboard): current vs previous valuation per reserve and line.
 *
 * @param currentRunId current run
 * @param currentDate current valuation date
 * @param currentStatus current run status
 * @param previousRunId previous posted run
 * @param previousDate previous valuation date
 * @param rows rows
 */
public record ReserveSummaryResponse(
    Long currentRunId,
    LocalDate currentDate,
    String currentStatus,
    Long previousRunId,
    LocalDate previousDate,
    List<Row> rows) {

  /**
   * Maps a summary.
   *
   * @param s summary
   * @return response
   */
  public static ReserveSummaryResponse from(ReserveSummary s) {
    return new ReserveSummaryResponse(
        s.currentRunId(),
        s.currentDate(),
        s.currentStatus(),
        s.previousRunId(),
        s.previousDate(),
        s.rows().stream()
            .map(
                r ->
                    new Row(
                        r.reserve(),
                        r.businessLine(),
                        r.current().gross(),
                        r.current().ri(),
                        r.current().net(),
                        r.previous().gross(),
                        r.previous().ri(),
                        r.previous().net()))
            .toList());
  }

  /**
   * One row.
   *
   * @param reserve reserve code
   * @param businessLine line of business
   * @param gross current gross
   * @param ri current reinsurers' share
   * @param net current net
   * @param previousGross previous gross
   * @param previousRi previous reinsurers' share
   * @param previousNet previous net
   */
  public record Row(
      String reserve,
      String businessLine,
      BigDecimal gross,
      BigDecimal ri,
      BigDecimal net,
      BigDecimal previousGross,
      BigDecimal previousRi,
      BigDecimal previousNet) {}
}
