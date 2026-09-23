package com.iortatechnxt.finverse.payables.report;

import org.springframework.stereotype.Component;

/** FIN-PDC-ISS-PERIOD – PDC Issued during Period (Src FPD004), by bank. */
@Component
public class PdcIssuedDuringPeriodReport extends AbstractPdcIssuedReport {

  /**
   * Creates the report.
   *
   * @param query register read model
   */
  public PdcIssuedDuringPeriodReport(PdcRegisterQuery query) {
    super(
        query,
        new Variant(
            "FIN-PDC-ISS-PERIOD",
            "PDC Issued during Period",
            "Post-dated cheques issued in a period, by paying bank (FPD004)",
            false,
            false));
  }
}
