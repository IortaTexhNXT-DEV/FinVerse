package com.iortatechnxt.finverse.payables.report;

import org.springframework.stereotype.Component;

/** FIN-PDC-ISS-PERIOD-DDB – PDC Issued during a Period by Division/Department and Bank (FR2584). */
@Component
public class PdcIssuedDuringPeriodByDivisionReport extends AbstractPdcIssuedReport {

  /**
   * Creates the report.
   *
   * @param query register read model
   */
  public PdcIssuedDuringPeriodByDivisionReport(PdcRegisterQuery query) {
    super(
        query,
        new Variant(
            "FIN-PDC-ISS-PERIOD-DDB",
            "PDC Issued during a Period by Division/Department and Bank",
            "Post-dated cheques issued in a period with division, department and bank totals"
                + " (FR2584)",
            false,
            true));
  }
}
