package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.renewal.check.service.ClaimsCheck;
import com.iortatechnxt.brokerverse.renewal.check.service.Evaluation;
import com.iortatechnxt.brokerverse.renewal.check.service.Finding;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateEndorsementRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionMatrix;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionMatrixRepository;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionRule;
import com.iortatechnxt.brokerverse.renewal.domain.DecisionRule.Criteria;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionProposal;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RuleSetStatus;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates the active decision matrix on a Clean or Review renewal after initiation (BRRN.031,
 * 034; RENEWAL_DESIGN section 8.2): the first rule by priority whose criteria match gives the
 * proposed disposition and AUTO or MANUAL. Without an active matrix every renewal is manual (risk
 * 2). The condition TOTAL_LOSS never matches until Claims defines a total-loss indicator.
 */
@Component
@Transactional(readOnly = true)
public class DecisionMatrixEvaluator {

  private static final String NONE = "NONE";

  private final DecisionMatrixRepository matrices;
  private final CandidateEndorsementRepository endorsements;
  private final AccountRepository accounts;

  /**
   * Creates the evaluator.
   *
   * @param matrices decision matrices
   * @param endorsements endorsements linked to renewals
   * @param accounts accounts (payment arrangement)
   */
  public DecisionMatrixEvaluator(
      DecisionMatrixRepository matrices,
      CandidateEndorsementRepository endorsements,
      AccountRepository accounts) {
    this.matrices = matrices;
    this.endorsements = endorsements;
    this.accounts = accounts;
  }

  /**
   * The proposal of the matrix for a renewal.
   *
   * @param candidate candidate (bucket and flags of its latest evaluation)
   * @param evaluation latest evaluation
   * @param today business date
   * @return proposal, NONE when no active matrix or no rule matches
   */
  public DispositionProposal propose(
      RenewalCandidate candidate, Evaluation evaluation, LocalDate today) {
    if (candidate.getBucket() == Bucket.EXCEPTION) {
      return DispositionProposal.NONE;
    }
    Optional<DecisionMatrix> matrix =
        matrices.findByCompanyIdAndStatus(candidate.getCompanyId(), RuleSetStatus.ACTIVE).stream()
            .filter(m -> !m.getEffectiveFrom().isAfter(today))
            .findFirst();
    if (matrix.isEmpty()) {
      return DispositionProposal.NONE;
    }
    Facts facts = facts(candidate, evaluation, today);
    for (DecisionRule rule : matrix.get().getRules()) {
      if (matches(rule.criteria(), facts)) {
        return new DispositionProposal(
            rule.getOutcome(), rule.getAutomation(), matrix.get().getVersionNo(), rule.getId());
      }
    }
    return DispositionProposal.NONE;
  }

  private Facts facts(RenewalCandidate c, Evaluation evaluation, LocalDate today) {
    CandidateSnapshot s = c.getSnapshot();
    Finding claims = evaluation.finding(ClaimsCheck.CODE).orElse(null);
    String claimsCondition = NONE;
    if (claims != null && claims.failed()) {
      claimsCondition = "OPEN";
    } else if (claims != null && claims.detail() != null) {
      claimsCondition = "PAID";
    }
    String endorsement = NONE;
    if (c.getFlags().isEndorsementPending()) {
      endorsement = "PENDING";
    } else if (!endorsements.findByCandidateIdOrderByIdAsc(c.getId()).isEmpty()) {
      endorsement = "POSTED_IN_TERM";
    }
    boolean direct =
        c.getExpiringArn() != null
            && accounts
                .findByArn(c.getExpiringArn())
                .map(a -> a.getPaymentArrangement() == PaymentArrangement.DIRECT_TO_INSURER)
                .orElse(false);
    String payment = c.getFlags().isOutstanding() ? "OUTSTANDING" : "PAID";
    return new Facts(
        s.product() == null ? null : s.product().segment(),
        s.product() == null ? null : s.product().lineCode(),
        s.product() == null ? null : s.product().productCode(),
        s.mortgage() != null && s.mortgage().mortgaged(),
        c.getBucket(),
        claimsCondition,
        endorsement,
        direct ? "DP" : payment,
        c.daysToExpiry(today));
  }

  private static boolean matches(Criteria r, Facts f) {
    return classification(r, f) && conditions(r, f) && days(r, f);
  }

  private static boolean classification(Criteria r, Facts f) {
    return same(r.segment(), f.segment())
        && same(r.lineCode(), f.line())
        && same(r.productCode(), f.product())
        && mortgageAndBucket(r, f);
  }

  private static boolean mortgageAndBucket(Criteria r, Facts f) {
    return (r.mortgaged() == null || r.mortgaged() == f.mortgaged())
        && (r.bucket() == null || r.bucket() == f.bucket());
  }

  private static boolean conditions(Criteria r, Facts f) {
    return same(r.claims(), f.claims())
        && same(r.endorsement(), f.endorsement())
        && same(r.payment(), f.payment());
  }

  private static boolean days(Criteria r, Facts f) {
    return (r.daysFrom() == null || f.daysToExpiry() >= r.daysFrom())
        && (r.daysTo() == null || f.daysToExpiry() <= r.daysTo());
  }

  private static boolean same(String rule, String value) {
    return rule == null || Objects.equals(rule, value);
  }

  /** The facts a rule is matched against. */
  private record Facts(
      String segment,
      String line,
      String product,
      boolean mortgaged,
      Bucket bucket,
      String claims,
      String endorsement,
      String payment,
      long daysToExpiry) {}
}
