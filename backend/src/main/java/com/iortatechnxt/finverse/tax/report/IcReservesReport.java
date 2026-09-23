package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.tax.domain.IcSchedule;
import com.iortatechnxt.finverse.tax.service.IcScheduleService;
import org.springframework.stereotype.Component;

/**
 * IC-RESERVES – Technical reserves (UPR, outstanding losses, IBNR, less reinsurers' share) read
 * from the ledger balances, independent of the reserves module.
 */
@Component
public class IcReservesReport extends IcScheduleReport {

  /**
   * Creates the report.
   *
   * @param schedules IC schedules
   */
  public IcReservesReport(IcScheduleService schedules) {
    super(schedules, "IC-RESERVES", IcSchedule.RESERVES);
  }
}
