package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
import org.springframework.stereotype.Component;

/** IC-NETWORTH – Net worth: admitted assets less liabilities as of a date. */
@Component
public class IcNetWorthReport extends IcScheduleReport {

  /**
   * Creates the report.
   *
   * @param schedules IC schedules
   */
  public IcNetWorthReport(IcScheduleService schedules) {
    super(schedules, "IC-NETWORTH", IcSchedule.NET_WORTH);
  }
}
