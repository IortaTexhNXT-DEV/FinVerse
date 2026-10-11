package com.iortatechnxt.brokerverse.payables.report;

import org.springframework.stereotype.Component;

/** FIN-PDC-ISS-DUEPAY – PDC Issued Due for Payment (Src FPD005), by bank. */
@Component
public class PdcDueForPaymentReport extends AbstractPdcIssuedReport {

  /**
   * Creates the report.
   *
   * @param query register read model
   */
  public PdcDueForPaymentReport(PdcRegisterQuery query) {
    super(
        query,
        new Variant(
            "FIN-PDC-ISS-DUEPAY",
            "PDC Issued Due for Payment",
            "Post-dated cheques issued and not yet presented as of a date, by bank (FPD005)",
            true,
            false));
  }
}
