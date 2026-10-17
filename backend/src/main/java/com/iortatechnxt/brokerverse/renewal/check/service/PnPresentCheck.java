package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import org.springframework.stereotype.Component;

/**
 * {@code PN_PRESENT} (BRRN.029/031/039): a mortgaged renewal, or one on the loan-driven
 * straight-through path, of a line that needs it ({@code RNW_PN_REQUIRED_LINES}) must carry the PN
 * number, the key of the LAMD matching.
 */
@Component
public class PnPresentCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "PN_PRESENT";

  private final RenewalParameters parameters;

  /**
   * Creates the check.
   *
   * @param parameters renewal parameters
   */
  public PnPresentCheck(RenewalParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    CandidateSnapshot s = context.candidate().getSnapshot();
    String line = s.product() == null ? null : s.product().lineCode();
    String segment = s.product() == null ? null : s.product().segment();
    boolean mortgaged = s.mortgage() != null && s.mortgage().mortgaged();
    boolean needed =
        parameters.pnRequired(line) && (mortgaged || parameters.loanDriven(segment, line));
    if (!needed) {
      return Verdict.notApplicable("No PN is needed for this renewal");
    }
    return s.pnNos() == null || s.pnNos().isBlank()
        ? Verdict.fail("The PN number is missing", null)
        : Verdict.pass("PN " + s.pnNos());
  }
}
