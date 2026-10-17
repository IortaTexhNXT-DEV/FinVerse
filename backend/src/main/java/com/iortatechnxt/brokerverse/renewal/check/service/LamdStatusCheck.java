package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.LamdLine;
import com.iortatechnxt.brokerverse.renewal.domain.LamdStatus;
import org.springframework.stereotype.Component;

/**
 * {@code LAMD_STATUS} (BRRN.029/039): the latest LAMD line matched by PN. A paid-off loan fails
 * with the system severity (tag Not for Renewal, reason Loan fully paid); an RMU loan fails as well
 * (tag, or transfer to the RMU unit when {@code RNW_RMU_UNIT} is set).
 */
@Component
public class LamdStatusCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "LAMD_STATUS";

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    LamdLine latest = context.lamdLines().stream().findFirst().orElse(null);
    if (latest == null) {
      return Verdict.notApplicable("No LAMD report names this renewal");
    }
    String detail = latest.getLoanStatus().name();
    return latest.getLoanStatus() == LamdStatus.PAID_OFF
        ? Verdict.fail("LAMD reports the loan of PN " + latest.getPnNo() + " fully paid", detail)
        : Verdict.fail("LAMD reports the loan of PN " + latest.getPnNo() + " under RMU", detail);
  }
}
