package com.iortatechnxt.brokerverse.renewal.alert;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistory;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistoryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renewal alerts of the daily alert run (FR-RN-102, BRRN.036): renewals at risk (not yet accepted
 * within {@code RNW_ESCALATION_DAYS} of expiry, by segment), renewals in the Exception bucket for
 * longer than {@code RNW_EXCEPTION_AGEING_DAYS}, and insurer batches past their reply date.
 */
@Component
@Transactional(readOnly = true)
public class RenewalAlertCheck implements AlertCheck {

  private static final Set<RenewalStage> BEFORE_ACCEPTANCE =
      EnumSet.of(
          RenewalStage.UNASSIGNED,
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.TRANSFER_PENDING,
          RenewalStage.FOR_TL_REVIEW,
          RenewalStage.FOR_PROCESSING,
          RenewalStage.IN_PROCESSING,
          RenewalStage.WITH_INSURER,
          RenewalStage.RA_READY,
          RenewalStage.RA_GENERATED,
          RenewalStage.RA_SENT);

  private final RenewalCandidateRepository candidates;
  private final BucketHistoryRepository buckets;
  private final InsurerBatchRepository batches;
  private final RenewalParameters parameters;

  /**
   * Creates the check.
   *
   * @param candidates renewals
   * @param buckets bucket history
   * @param batches insurer batches
   * @param parameters thresholds
   */
  public RenewalAlertCheck(
      RenewalCandidateRepository candidates,
      BucketHistoryRepository buckets,
      InsurerBatchRepository batches,
      RenewalParameters parameters) {
    this.candidates = candidates;
    this.buckets = buckets;
    this.batches = batches;
    this.parameters = parameters;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    List<AlertSignal> signals = new ArrayList<>();
    for (RenewalCandidate c : candidates.findByStageIn(BEFORE_ACCEPTANCE)) {
      atRisk(c, asOf, signals);
      ageing(c, asOf, signals);
    }
    for (InsurerBatch b :
        batches.findByStatusInAndReplyDueBefore(
            List.of(InsurerBatchStatus.SENT, InsurerBatchStatus.PARTIALLY_RESPONDED), asOf)) {
      signals.add(
          new AlertSignal(
              RenewalCodes.ALERT_INSURER_OVERDUE,
              new AlertFacts(
                  b.getCompanyId(),
                  null,
                  RenewalCodes.ENTITY_BATCH,
                  b.getBatchNo(),
                  "Insurer "
                      + b.getInsurerCode()
                      + " has not answered batch "
                      + b.getBatchNo()
                      + " due "
                      + b.getReplyDue(),
                  null,
                  RenewalCodes.ALERT_INSURER_OVERDUE + ":" + b.getBatchNo())));
    }
    return signals;
  }

  private void atRisk(RenewalCandidate c, LocalDate asOf, List<AlertSignal> signals) {
    String segment = c.getSnapshot().product() == null ? null : c.getSnapshot().product().segment();
    long days = c.daysToExpiry(asOf);
    if (days < 0 || days > parameters.escalationDays(segment)) {
      return;
    }
    signals.add(
        new AlertSignal(
            RenewalCodes.ALERT_AT_RISK,
            facts(
                c,
                "Renewal "
                    + c.getRenewalRef()
                    + " of "
                    + c.getSnapshot().clientName()
                    + " expires in "
                    + days
                    + " day(s) and is "
                    + c.getStage().label(),
                RenewalCodes.ALERT_AT_RISK + ":" + c.getRenewalRef())));
  }

  private void ageing(RenewalCandidate c, LocalDate asOf, List<AlertSignal> signals) {
    if (c.getBucket() == Bucket.EXCEPTION) {
      LocalDate since =
          buckets.findByCandidateIdOrderByIdDesc(c.getId()).stream()
              .filter(h -> h.getToBucket() == Bucket.EXCEPTION)
              .map(BucketHistory::getCreatedAt)
              .map(BusinessClock::dateOf)
              .findFirst()
              .orElse(asOf);
      if (!since.plusDays(parameters.exceptionAgeingDays()).isAfter(asOf)) {
        signals.add(
            new AlertSignal(
                RenewalCodes.ALERT_EXCEPTION_AGEING,
                facts(
                    c,
                    "Renewal " + c.getRenewalRef() + " is in the Exception bucket since " + since,
                    RenewalCodes.ALERT_EXCEPTION_AGEING + ":" + c.getRenewalRef() + ":" + since)));
      }
    }
  }

  private static AlertFacts facts(RenewalCandidate c, String message, String key) {
    return new AlertFacts(
        c.getCompanyId(), null, RenewalCodes.ENTITY, c.getRenewalRef(), message, null, key);
  }
}
