package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
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
