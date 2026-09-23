package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.service.IcScheduleService;
import org.springframework.stereotype.Component;

/** IC-LOSS-LOB – Losses incurred (paid, reserve change, reinsurance share) per line of business. */
@Component
public class IcLossesReport extends IcScheduleReport {

  /**
   * Creates the report.
   *
   * @param schedules IC schedules
   */
  public IcLossesReport(IcScheduleService schedules) {
    super(schedules, "IC-LOSS-LOB", IcSchedule.LOSSES);
  }
}
