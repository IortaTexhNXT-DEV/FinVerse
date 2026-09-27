package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRunRepository;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A job that follows the end-of-day runs (design 9): it selects the runs up to the business date
 * that still need its work and processes each in its own transaction; a run that fails is logged
 * and retried on the next execution.
 */
public abstract class EodRunJob implements ManagedJob {

  private static final Logger LOG = LoggerFactory.getLogger(EodRunJob.class);

  /** EOD runs. */
  protected final EodRunRepository runs;

  private final TransactionTemplate tx;

  /**
   * Creates the job.
   *
   * @param runs EOD runs
   * @param txManager transactions
   */
  protected EodRunJob(EodRunRepository runs, PlatformTransactionManager txManager) {
    this.runs = runs;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * The runs to process.
   *
   * @param businessDate business date
   * @return runs
   */
  protected abstract List<EodRun> select(LocalDate businessDate);

  /**
   * Processes one run.
   *
   * @param run run
   * @return items processed
   */
  protected abstract int process(EodRun run);

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<Long> ids = tx.execute(s -> select(businessDate).stream().map(EodRun::getId).toList());
    int runsDone = 0;
    int items = 0;
    for (Long id : ids == null ? List.<Long>of() : ids) {
      try {
        Integer n = tx.execute(s -> process(runs.findById(id).orElseThrow()));
        items += n == null ? 0 : n;
        runsDone++;
      } catch (BusinessRuleException ex) {
        LOG.warn("{} skipped EOD run {}: {}", name(), id, ex.getMessage());
      }
    }
    return new JobOutcome(items, runsDone + " run(s), " + items + " item(s)");
  }
}
