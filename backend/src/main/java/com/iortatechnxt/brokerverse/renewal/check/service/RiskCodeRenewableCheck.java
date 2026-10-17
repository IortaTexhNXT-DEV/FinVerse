package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCodeRepository;
import org.springframework.stereotype.Component;

/**
 * {@code RISK_CODE_RENEWABLE} (BRRN.009; FR-RN-024): a renewal on a non-renewable risk code fails
 * with the system severity: the engine tags it Not for Renewal (reason Non-renewable Accounts,
 * source system check) and the NFR follows; it has no bucket effect.
 */
@Component
public class RiskCodeRenewableCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "RISK_CODE_RENEWABLE";

  private final NonRenewableRiskCodeRepository codes;

  /**
   * Creates the check.
   *
   * @param codes non-renewable risk codes
   */
  public RiskCodeRenewableCheck(NonRenewableRiskCodeRepository codes) {
    this.codes = codes;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    CandidateSnapshot s = context.candidate().getSnapshot();
    String risk = s.product() == null ? null : s.product().productCode();
    if (risk == null) {
      return Verdict.notApplicable("No risk code yet");
    }
    String line = s.product().lineCode();
    return codes.findByCompanyIdAndRiskCode(context.candidate().getCompanyId(), risk).stream()
        .filter(c -> !c.isRenewable() && c.appliesTo(risk, line, context.today()))
        .findFirst()
        .map(
            c ->
                Verdict.fail(
                    "Risk code " + risk + " is not renewable: " + c.getReason(), reasonOf(c)))
        .orElseGet(() -> Verdict.pass("Risk code " + risk + " is renewable"));
  }

  private static String reasonOf(NonRenewableRiskCode code) {
    return "code " + code.getRiskCode() + " from " + code.getEffectiveFrom();
  }
}
