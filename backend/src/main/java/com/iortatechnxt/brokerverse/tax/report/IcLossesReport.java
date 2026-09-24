package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
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
