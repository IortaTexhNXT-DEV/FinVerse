package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.renewal.check.service.CheckEngine;
import com.iortatechnxt.brokerverse.renewal.check.service.Evaluation;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Re-evaluation of an open renewal (BRRN.023 AC 3; RENEWAL_DESIGN section 8.1): the checks run
 * again on ledger, account and booking events, on every upload and nightly; the bucket follows, a
 * system action (non-renewable risk code, LAMD) is applied to an initiated renewal, and the
 * assignees are told when it enters the Exception bucket.
 */
@Service
@Transactional
public class ReevaluationService {

  private final CheckEngine engine;
  private final RoutingService routing;
  private final RenewalNotices notices;

  /**
   * Creates the service.
   *
   * @param engine check engine
   * @param routing routing (system actions)
   * @param notices notifications
   */
  public ReevaluationService(CheckEngine engine, RoutingService routing, RenewalNotices notices) {
    this.engine = engine;
    this.routing = routing;
    this.notices = notices;
  }

  /**
   * Runs the checks of an open renewal again.
   *
   * @param candidate candidate
   * @param trigger what ran them
   * @return evaluation, empty when the renewal is closed or still being evaluated
   */
  public Optional<Evaluation> reevaluate(RenewalCandidate candidate, CheckTrigger trigger) {
    if (!candidate.getStage().isOpen() || candidate.getStage() == RenewalStage.EVALUATING) {
      return Optional.empty();
    }
    Bucket before = candidate.getBucket();
    Evaluation evaluation = engine.run(candidate, trigger);
    evaluation.systemTag().ifPresent(tag -> routing.applyLater(candidate, tag));
    if (evaluation.bucket() == Bucket.EXCEPTION && before != Bucket.EXCEPTION) {
      List<String> owners = new ArrayList<>();
      owners.add(candidate.getAssignedAo());
      owners.add(candidate.getAssignedPo());
      notices.users(
          owners,
          RenewalCodes.EVENT_ASSIGNED,
          candidate,
          new RenewalNotices.Text(
              candidate.getRenewalRef() + ": Exception",
              "A check failed on the renewal of " + candidate.getSnapshot().clientName()));
    }
    return Optional.of(evaluation);
  }
}
