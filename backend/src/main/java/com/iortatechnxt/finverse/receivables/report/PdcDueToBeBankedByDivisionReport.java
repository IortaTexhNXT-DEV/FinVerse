package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import org.springframework.stereotype.Component;

/** FIN-PDC-RCV-DUEBANK-DDB (Src FR2583) PDC Due to be Banked by Division/Department and Bank. */
@Component
public class PdcDueToBeBankedByDivisionReport extends AbstractPdcReceivedReport {

  /**
   * Creates the report.
   *
   * @param pdcs PDC register
   * @param organization organization service
   */
  public PdcDueToBeBankedByDivisionReport(PdcQueries pdcs, OrganizationService organization) {
    super(
        pdcs,
        organization,
        "FIN-PDC-RCV-DUEBANK-DDB",
        "PDC Due to be Banked by Division/Department and Bank",
        Selection.DUE_TO_BANK,
        true);
  }
}
