package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.renewal.domain.OverrideKind;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * {@code OUTSTANDING_PREMIUM} (BRD 1.011; BRRN.031): the open premium of the expiring invoice
 * family, read live from the Operations ledger, above {@code RNW_OUTSTANDING_THRESHOLD}. The Team
 * Leader's override lets the renewal be posted; the check keeps failing (never Clean) until the
 * premium is paid.
 */
@Component
public class OutstandingPremiumCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "OUTSTANDING_PREMIUM";

  private final RenewalParameters parameters;

  /**
   * Creates the check.
   *
   * @param parameters renewal parameters
   */
  public OutstandingPremiumCheck(RenewalParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    if (!context.bibs()) {
      return Verdict.notApplicable("Balances are read from the BIBS ledger only");
    }
    BigDecimal open =
        context.family().stream()
            .map(OpsInvoice::premiumBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (open.compareTo(parameters.outstandingThreshold()) > 0) {
      String text = "Outstanding premium " + DisplayFormat.amount(open);
      return context.overridden(OverrideKind.OUTSTANDING_BALANCE, null)
          ? Verdict.fail(text + " (overridden by the Team Leader)", open.toPlainString())
          : Verdict.fail(text, open.toPlainString());
    }
    return Verdict.pass("No outstanding premium");
  }
}
