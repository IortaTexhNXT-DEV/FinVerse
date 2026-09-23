package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import org.springframework.stereotype.Component;

/** FIN-PDC-RCV-DUEBANK (Src FPD003) PDC Due to be Banked: on hand and due as of a date. */
@Component
public class PdcDueToBeBankedReport extends AbstractPdcReceivedReport {

  /**
   * Creates the report.
   *
   * @param pdcs PDC register
   * @param organization organization service
   */
  public PdcDueToBeBankedReport(PdcQueries pdcs, OrganizationService organization) {
    super(
        pdcs,
        organization,
        "FIN-PDC-RCV-DUEBANK",
        "PDC Due to be Banked",
        Selection.DUE_TO_BANK,
        false);
  }
}
