package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.service.IcScheduleService;
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
