package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.service.IcScheduleService;
import org.springframework.stereotype.Component;

/** IC-COMM-LOB – Commissions (expense, reinsurance commission income, net) per line of business. */
@Component
public class IcCommissionsReport extends IcScheduleReport {

  /**
   * Creates the report.
   *
   * @param schedules IC schedules
   */
  public IcCommissionsReport(IcScheduleService schedules) {
    super(schedules, "IC-COMM-LOB", IcSchedule.COMMISSIONS);
  }
}
