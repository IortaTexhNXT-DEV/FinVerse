package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import org.springframework.stereotype.Component;

/** FIN-PDC-RCV-ONHAND (Src FPD001) PDC (Received) on Hand as of a date, by due date. */
@Component
public class PdcOnHandReport extends AbstractPdcReceivedReport {

  /**
   * Creates the report.
   *
   * @param pdcs PDC register
   * @param organization organization service
   */
  public PdcOnHandReport(PdcQueries pdcs, OrganizationService organization) {
    super(
        pdcs,
        organization,
        "FIN-PDC-RCV-ONHAND",
        "PDC (Received) on Hand",
        Selection.ON_HAND,
        false);
  }
}
