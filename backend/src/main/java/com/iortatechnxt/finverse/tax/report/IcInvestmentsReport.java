package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.service.IcScheduleService;
import org.springframework.stereotype.Component;

/**
 * IC-INVEST – Investments by class (placements, FVPL, FVOCI, amortised cost, security deposit) read
 * from the ledger balances, independent of the investment module.
 */
@Component
public class IcInvestmentsReport extends IcScheduleReport {

  /**
   * Creates the report.
   *
   * @param schedules IC schedules
   */
  public IcInvestmentsReport(IcScheduleService schedules) {
    super(schedules, "IC-INVEST", IcSchedule.INVESTMENTS);
  }
}
