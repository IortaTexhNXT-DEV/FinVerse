package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycleRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Automatch of uploaded insurer production (PRCID.024/025): re-runs the matching of every open
 * cycle that has insurer feedback (stages awaiting feedback and reconciling), so production booked
 * or pre-booked since the upload is paired. Manual until BDOI gives the frequency (OQ30, {@code
 * brokerverse.jobs.recon-automatch-cron}).
 */
@Component
public class ReconAutomatchJob implements ManagedJob {

  /** Job name. */
  public static final String NAME = "RECON_AUTOMATCH";

  private final ReconCycleRepository cycles;
  private final ReconMatchingService matching;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param cycles cycles
   * @param matching matching engine
   * @param txManager transaction manager
   * @param cron schedule
   */
  public ReconAutomatchJob(
      ReconCycleRepository cycles,
      ReconMatchingService matching,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.recon-automatch-cron:-}") String cron) {
    this.cycles = cycles;
    this.matching = matching;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.cron = cron;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String description() {
    return "Re-matches the insurer production of the open reconciliation cycles (PRCID.025)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<ReconCycle> open =
        cycles.findByClosedFalseAndStageInOrderByIdAsc(
            List.of(ReconCycle.SENT, ReconCycle.RECONCILING));
    int changed = 0;
    int skipped = 0;
    for (ReconCycle cycle : open) {
      try {
        Integer n = tx.execute(s -> matching.rematch(cycle.getId()));
        changed += n == null ? 0 : n;
      } catch (BusinessRuleException ex) {
        skipped++;
      }
    }
    return new JobOutcome(
        open.size(),
        open.size()
            + " cycle(s) matched, "
            + changed
            + " item(s) changed, "
            + skipped
            + " skipped");
  }
}
