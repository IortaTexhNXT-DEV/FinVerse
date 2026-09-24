package com.iortatechnxt.brokerverse.receivables.report;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.receivables.service.PdcQueries;
import org.springframework.stereotype.Component;

/**
 * FIN-PDC-RCV-PERIOD-DDB (Src FR2582) PDC Received during the Period by Division/Department and
 * Bank.
 */
@Component
public class PdcReceivedByDivisionReport extends AbstractPdcReceivedReport {

  /**
   * Creates the report.
   *
   * @param pdcs PDC register
   * @param organization organization service
   */
  public PdcReceivedByDivisionReport(PdcQueries pdcs, OrganizationService organization) {
    super(
        pdcs,
        organization,
        "FIN-PDC-RCV-PERIOD-DDB",
        "PDC Received during the Period by Division/Department and Bank",
        Selection.PERIOD,
        true);
  }
}
