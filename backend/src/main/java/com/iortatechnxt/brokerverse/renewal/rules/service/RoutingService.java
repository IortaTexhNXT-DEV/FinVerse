package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.check.service.Evaluation;
import com.iortatechnxt.brokerverse.renewal.check.service.Evaluation.SystemTag;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CurrentDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionProposal;
import com.iortatechnxt.brokerverse.renewal.domain.DispositionSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPath;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalDispositions;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Routes an initiated renewal after its checks (RENEWAL_DESIGN section 7.1; BRRN.009, 029, 031,
 * 034, 039) and applies the system actions of later evaluations:
 *
 * <ul>
 *   <li>a non-renewable risk code or a LAMD paid-off / RMU loan: Not for Renewal by the system
 *       (letter step), or the transfer of an RMU account to {@code RNW_RMU_UNIT};
 *   <li>the loan-driven straight-through path (CBG Motor and Fire, BRRN.039): For Renewal by the
 *       system, straight to Processing, never to an AO;
 *   <li>a Clean renewal with an AUTO matrix rule: the matrix disposition, skipping the Team Leader
 *       review when {@code RNW_STP_SKIP_TL_REVIEW} is on;
 *   <li>otherwise Unassigned Disposition, with the matrix proposal as the AO's default.
 * </ul>
 */
@Service
@Transactional
public class RoutingService {

  private static final Set<RenewalStage> TAGGABLE =
      EnumSet.of(
          RenewalStage.UNASSIGNED,
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.FOR_TL_REVIEW,
          RenewalStage.FOR_PROCESSING,
          RenewalStage.IN_PROCESSING);

  private static final String ROUTE_UNASSIGNED = "route_unassigned";

  private final DecisionMatrixEvaluator matrix;
  private final RenewalDispositions dispositions;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final Clock clock;

  /**
   * Creates the routing.
   *
   * @param matrix decision matrix
   * @param dispositions disposition writer
   * @param flow workflow
   * @param parameters renewal parameters
   * @param clock clock
   */
  public RoutingService(
      DecisionMatrixEvaluator matrix,
      RenewalDispositions dispositions,
      RenewalFlow flow,
      RenewalParameters parameters,
      Clock clock) {
    this.matrix = matrix;
    this.dispositions = dispositions;
    this.flow = flow;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Routes a renewal in EVALUATING.
   *
   * @param candidate candidate
   * @param evaluation its evaluation at initiation
   */
  public void route(RenewalCandidate candidate, Evaluation evaluation) {
    if (evaluation.systemTag().isPresent()) {
      applyTag(candidate, evaluation.systemTag().get(), "route_letter");
      return;
    }
    CandidateSnapshot s = candidate.getSnapshot();
    String segment = s.product() == null ? null : s.product().segment();
    String line = s.product() == null ? null : s.product().lineCode();
    if (parameters.loanDriven(segment, line)) {
      candidate.getFlags().setStp(true);
      candidate.takePath(RenewalPath.STP);
      dispositions.record(
          candidate,
          new CurrentDisposition(
              RenewalDisposition.FOR_RENEWAL,
              null,
              DispositionSource.SYSTEM_CHECK,
              "Loan-driven straight-through renewal",
              null),
          null,
          null);
      flow.system(candidate, "route_processing", "Straight-through renewal of a loan account");
      return;
    }
    DispositionProposal proposal =
        matrix.propose(candidate, evaluation, BusinessClock.today(clock));
    candidate.propose(proposal);
    if (proposal.isAuto() && candidate.getBucket() == Bucket.CLEAN) {
      automatic(candidate, proposal);
    } else {
      flow.system(candidate, ROUTE_UNASSIGNED, "For disposition by Marketing");
    }
  }

  private void automatic(RenewalCandidate candidate, DispositionProposal proposal) {
    candidate.getFlags().setStp(true);
    candidate.takePath(RenewalPath.STP);
    dispositions.record(
        candidate,
        new CurrentDisposition(
            proposal.disposition(), null, DispositionSource.MATRIX, "Decision matrix", null),
        proposal.matrixVersion(),
        proposal.ruleId());
    String comment = "Matrix v" + proposal.matrixVersion() + " rule " + proposal.ruleId();
    if (!parameters.stpSkipsReview()) {
      flow.system(candidate, "route_review", comment);
    } else if (proposal.disposition() == RenewalDisposition.FOR_RENEWAL) {
      flow.system(candidate, "route_processing", comment);
    } else if (proposal.disposition() == RenewalDisposition.NOT_FOR_RENEWAL) {
      flow.system(candidate, "route_letter", comment);
    } else {
      flow.system(candidate, "route_review", comment);
    }
  }

  /**
   * Applies the system action of a later evaluation to an open renewal of Marketing or Processing
   * (for example a LAMD report that arrives after initiation).
   *
   * @param candidate candidate
   * @param tag system action
   * @return true when applied
   */
  public boolean applyLater(RenewalCandidate candidate, SystemTag tag) {
    if (!TAGGABLE.contains(candidate.getStage()) || alreadyTagged(candidate, tag)) {
      return false;
    }
    applyTag(candidate, tag, "system_not_for_renewal");
    return true;
  }

  private static boolean alreadyTagged(RenewalCandidate candidate, SystemTag tag) {
    if (tag.transferUnit() != null) {
      return Objects.equals(candidate.getOwnerUnit(), tag.transferUnit());
    }
    CurrentDisposition d = candidate.getDisposition();
    return d.code() == RenewalDisposition.NOT_FOR_RENEWAL
        && Objects.equals(d.reasonCode(), tag.reasonCode());
  }

  private void applyTag(RenewalCandidate candidate, SystemTag tag, String action) {
    if (tag.transferUnit() != null) {
      candidate.moveToUnit(tag.transferUnit());
      candidate.getFlags().setTransferred(true);
      candidate.assignAo(null);
      if (candidate.getStage() == RenewalStage.EVALUATING) {
        flow.system(candidate, ROUTE_UNASSIGNED, "RMU account routed to " + tag.transferUnit());
      }
      return;
    }
    dispositions.record(
        candidate,
        new CurrentDisposition(
            RenewalDisposition.NOT_FOR_RENEWAL,
            tag.reasonCode(),
            tag.source(),
            tag.remarks(),
            null),
        null,
        null);
    flow.system(candidate, action, tag.remarks());
  }
}
