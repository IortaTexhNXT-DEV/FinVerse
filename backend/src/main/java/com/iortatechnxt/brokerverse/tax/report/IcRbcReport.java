package com.iortatechnxt.brokerverse.tax.report;

import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.service.IcScheduleService;
import org.springframework.stereotype.Component;

/** IC-RBC – Simplified risk-based capital summary with the parameterised factors and the ratio. */
@Component
public class IcRbcReport extends IcScheduleReport {

  /**
   * Creates the report.
   *
   * @param schedules IC schedules
   */
  public IcRbcReport(IcScheduleService schedules) {
    super(schedules, "IC-RBC", IcSchedule.RBC);
  }
}
