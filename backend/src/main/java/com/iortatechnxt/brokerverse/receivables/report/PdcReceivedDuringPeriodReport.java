package com.iortatechnxt.brokerverse.receivables.report;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.receivables.service.PdcQueries;
import org.springframework.stereotype.Component;

/** FIN-PDC-RCV-PERIOD (Src FPD002) PDC Received during a Period, any status. */
@Component
public class PdcReceivedDuringPeriodReport extends AbstractPdcReceivedReport {

  /**
   * Creates the report.
   *
   * @param pdcs PDC register
   * @param organization organization service
   */
  public PdcReceivedDuringPeriodReport(PdcQueries pdcs, OrganizationService organization) {
    super(
        pdcs,
        organization,
        "FIN-PDC-RCV-PERIOD",
        "PDC Received during a Period",
        Selection.PERIOD,
        false);
  }
}
