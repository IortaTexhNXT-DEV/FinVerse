package com.iortatechnxt.brokerverse.receivables.report;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.receivables.service.PdcQueries;
import org.springframework.stereotype.Component;

/** FIN-PDC-RCV-ONHAND-DDB (Src FR2581) PDC (Received) on Hand by Division/Department and Bank. */
@Component
public class PdcOnHandByDivisionReport extends AbstractPdcReceivedReport {

  /**
   * Creates the report.
   *
   * @param pdcs PDC register
   * @param organization organization service
   */
  public PdcOnHandByDivisionReport(PdcQueries pdcs, OrganizationService organization) {
    super(
        pdcs,
        organization,
        "FIN-PDC-RCV-ONHAND-DDB",
        "PDC (Received) on Hand by Division/Department and Bank",
        Selection.ON_HAND,
        true);
  }
}
