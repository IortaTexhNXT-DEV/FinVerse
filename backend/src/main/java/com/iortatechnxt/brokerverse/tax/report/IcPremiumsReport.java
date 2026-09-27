package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
import org.springframework.stereotype.Component;

/** IC-PREM-LOB – Premiums written (gross, ceded, net) per line of business. */
@Component
public class IcPremiumsReport extends IcScheduleReport {

  /**
   * Creates the report.
   *
   * @param schedules IC schedules
   */
  public IcPremiumsReport(IcScheduleService schedules) {
    super(schedules, "IC-PREM-LOB", IcSchedule.PREMIUMS);
  }
}
