package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPath;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code DUPLICATE_CANDIDATE} (BRRN.005): no other open renewal of the same expiring policy, and no
 * live renewal account of it other than this renewal's own (for example one created by hand or by
 * the New Business path of another renewal).
 */
@Component
public class DuplicateCandidateCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "DUPLICATE_CANDIDATE";

  private static final Set<AccountStatus> DEAD =
      EnumSet.of(AccountStatus.VOIDED, AccountStatus.CANCELLED, AccountStatus.PLACEMENT_CANCELLED);

  private final RenewalCandidateRepository candidates;
  private final AccountRepository accounts;

  /**
   * Creates the check.
   *
   * @param candidates candidates
   * @param accounts accounts (renewal accounts)
   */
  public DuplicateCandidateCheck(
      RenewalCandidateRepository candidates, AccountRepository accounts) {
    this.candidates = candidates;
    this.accounts = accounts;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    RenewalCandidate c = context.candidate();
    String key = context.bibs() ? c.getExpiringArn() : c.getSourceRef();
    if (key == null) {
      return Verdict.notApplicable("No policy reference to compare");
    }
    List<String> others =
        candidates.findByExpiringArn(key).stream()
            .filter(o -> !o.getId().equals(c.getId()) && o.getStage().isOpen())
            .filter(o -> Objects.equals(o.getPolicyYear(), c.getPolicyYear()))
            .map(RenewalCandidate::getRenewalRef)
            .toList();
    if (!others.isEmpty()) {
      return Verdict.fail(
          "Another open renewal of the same policy: " + String.join(", ", others),
          String.join(",", others));
    }
    if (c.getPath() == RenewalPath.NB_PATH) {
      return Verdict.pass("No other open renewal of the policy");
    }
    List<String> live =
        accounts.findByClassificationRenewalOfRef(key).stream()
            .filter(a -> !DEAD.contains(a.getStatus()))
            .map(Account::getArn)
            .filter(arn -> !arn.equals(c.getRenewalArn()))
            .toList();
    return live.isEmpty()
        ? Verdict.pass("No other renewal of the policy")
        : Verdict.fail(
            "The policy is already renewed by account " + String.join(", ", live),
            String.join(",", live));
  }
}
