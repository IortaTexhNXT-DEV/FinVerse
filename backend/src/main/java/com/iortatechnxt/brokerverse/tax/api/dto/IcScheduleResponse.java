package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleLine;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleResult;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleResult.RbcSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A computed Insurance Commission schedule.
 *
 * @param schedule schedule
 * @param title title
 * @param from period start
 * @param asOf period end / balance date
 * @param lines lines
 * @param total total
 * @param rbc RBC summary (RBC schedule only)
 */
public record IcScheduleResponse(
    IcSchedule schedule,
    String title,
    LocalDate from,
    LocalDate asOf,
    List<IcScheduleLine> lines,
    BigDecimal total,
    RbcSummary rbc) {

  /**
   * Maps a result.
   *
   * @param r result
   * @return response
   */
  public static IcScheduleResponse from(IcScheduleResult r) {
    return new IcScheduleResponse(
        r.schedule(), r.schedule().title(), r.from(), r.asOf(), r.lines(), r.total(), r.rbc());
  }
}
