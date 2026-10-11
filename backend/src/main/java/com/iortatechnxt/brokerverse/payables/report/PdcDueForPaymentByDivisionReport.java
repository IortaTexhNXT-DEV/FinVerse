package com.iortatechnxt.brokerverse.payables.report;

import org.springframework.stereotype.Component;

/** FIN-PDC-ISS-DUEPAY-DDB – PDC Due for Payment by Division/Department and Bank (FR2585). */
@Component
public class PdcDueForPaymentByDivisionReport extends AbstractPdcIssuedReport {

  /**
   * Creates the report.
   *
   * @param query register read model
   */
  public PdcDueForPaymentByDivisionReport(PdcRegisterQuery query) {
    super(
        query,
        new Variant(
            "FIN-PDC-ISS-DUEPAY-DDB",
            "PDC Due for Payment by Division/Department and Bank",
            "Outstanding post-dated cheques issued with division, department and bank totals"
                + " (FR2585)",
            true,
            true));
  }
}
