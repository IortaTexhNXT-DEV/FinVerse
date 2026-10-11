package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code RNW_REEVALUATE} (BRRN.020/022; cron {@code brokerverse.jobs.renewal-reevaluate-cron}):
 * runs the checks of every open renewal again before its Renewal Advice is sent, so that claims,
 * payments, endorsements and loan statuses received during the day move the bucket. Each renewal
 * runs in its own transaction; one failure does not stop the others.
 */
@Component
public class ReevaluateJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "RNW_REEVALUATE";

  private static final Logger LOG = LoggerFactory.getLogger(ReevaluateJob.class);

  private static final Set<RenewalStage> STAGES =
      EnumSet.of(
          RenewalStage.EXTRACTED,
          RenewalStage.UNASSIGNED,
          RenewalStage.TRANSFER_PENDING,
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.FOR_TL_REVIEW,
          RenewalStage.FOR_PROCESSING,
          RenewalStage.IN_PROCESSING,
          RenewalStage.WITH_INSURER,
          RenewalStage.RA_READY,
          RenewalStage.RA_GENERATED);

  private final RenewalCandidateRepository candidates;
  private final ReevaluationService reevaluation;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param candidates renewals
   * @param reevaluation checks
   * @param txManager transactions
   * @param cron schedule
   */
  public ReevaluateJob(
      RenewalCandidateRepository candidates,
      ReevaluationService reevaluation,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.renewal-reevaluate-cron:-}") String cron) {
    this.candidates = candidates;
    this.reevaluation = reevaluation;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Runs the checks of the open renewals again and moves their bucket";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<Long> ids =
        candidates.findByStageIn(STAGES).stream().map(RenewalCandidate::getId).toList();
    int done = 0;
    int failed = 0;
    for (Long id : ids) {
      try {
        tx.executeWithoutResult(
            s ->
                candidates
                    .findById(id)
                    .ifPresent(c -> reevaluation.reevaluate(c, CheckTrigger.NIGHTLY)));
        done++;
      } catch (RuntimeException e) {
        failed++;
        LOG.warn("Renewal {} not evaluated again: {}", id, e.getMessage());
      }
    }
    return new JobOutcome(done, done + " renewal(s) evaluated again, " + failed + " failed");
  }
}
