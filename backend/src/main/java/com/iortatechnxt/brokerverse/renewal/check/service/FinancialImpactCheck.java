package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotPremium;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@code FINANCIAL_IMPACT} (BRRN.038; FR-RN-064): the renewal account, or the latest valid revision
 * of the insurer, against the expiring term - premium and sum insured. A difference above {@code
 * RNW_FIN_IMPACT_TOLERANCE} fails the check: the renewal goes to Review, the matrix may send it to
 * the New Business path, and the client's acceptance needs its evidence.
 */
@Component
public class FinancialImpactCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "FINANCIAL_IMPACT";

  private final RenewalParameters parameters;

  /**
   * Creates the check.
   *
   * @param parameters renewal parameters
   */
  public FinancialImpactCheck(RenewalParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    SnapshotPremium expiring = context.candidate().getSnapshot().premium();
    Optional<InsurerResponse> revision =
        context.responses().stream()
            .filter(r -> r.isLatestValid() && r.getResponse() == InsurerResponseCode.REVISE)
            .findFirst();
    Optional<Account> renewal = context.renewalAccount();
    if (expiring == null || revision.isEmpty() && renewal.isEmpty()) {
      return Verdict.notApplicable("No renewal terms to compare yet");
    }
    BigDecimal premium =
        revision
            .map(InsurerResponse::getRevisedPremium)
            .orElseGet(() -> renewal.map(a -> a.getPremium().grossPremium()).orElse(null));
    BigDecimal sumInsured =
        revision
            .map(InsurerResponse::getRevisedSumInsured)
            .orElseGet(() -> renewal.map(Account::getTotalSumInsured).orElse(null));
    List<String> changes = new ArrayList<>();
    compare("premium", expiring.grossPremium(), premium, changes);
    compare("sum insured", expiring.totalSumInsured(), sumInsured, changes);
    return changes.isEmpty()
        ? Verdict.pass("Renewal terms equal the expiring terms")
        : Verdict.fail(
            "Financial impact: " + String.join("; ", changes), String.join("; ", changes));
  }

  private void compare(String what, BigDecimal before, BigDecimal after, List<String> changes) {
    if (before == null || after == null) {
      return;
    }
    BigDecimal difference = after.subtract(before);
    if (difference.abs().compareTo(parameters.financialTolerance()) > 0) {
      changes.add(
          what
              + " "
              + before.toPlainString()
              + " to "
              + after.toPlainString()
              + " ("
              + (difference.signum() > 0 ? "+" : "")
              + difference.toPlainString()
              + ")");
    }
  }
}
