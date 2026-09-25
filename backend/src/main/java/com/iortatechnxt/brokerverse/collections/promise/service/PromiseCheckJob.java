package com.iortatechnxt.brokerverse.collections.promise.service;

import com.iortatechnxt.brokerverse.collections.installment.service.InstallmentPlanService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code CLX_PROMISE_CHECK} (BRCLXN.053/055, cron {@code brokerverse.jobs.clx-promise-check-cron},
 * 22:45 PHT after the ledger's end of day): allocates the ledger payments to the installments of
 * every live plan and flags the overdue ones, then evaluates the promises to pay whose date plus
 * the grace days has passed. A broken promise escalates through the BROKEN_PROMISES_COUNT rules.
 * Each plan and promise is handled in its own transaction, so one failure does not stop the others.
 */
@Component
public class PromiseCheckJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "CLX_PROMISE_CHECK";

  private static final Logger LOG = LoggerFactory.getLogger(PromiseCheckJob.class);

  private final InstallmentPlanService plans;
  private final PromiseService promises;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param plans installment plans
   * @param promises promises
   * @param txManager transactions
   * @param cron schedule ({@code brokerverse.jobs.clx-promise-check-cron})
   */
  public PromiseCheckJob(
      InstallmentPlanService plans,
      PromiseService promises,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.clx-promise-check-cron:-}") String cron) {
    this.plans = plans;
    this.promises = promises;
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
    return "Allocates payments to installments and checks the promises to pay due (BRCLXN.053/055)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int refreshed = each(plans.activePlanIds(), id -> plans.refresh(id, businessDate));
    int evaluated =
        each(promises.dueForEvaluation(businessDate), id -> promises.evaluate(id, businessDate));
    return new JobOutcome(
        refreshed + evaluated,
        refreshed + " plan(s) allocated, " + evaluated + " promise(s) evaluated");
  }

  private int each(List<Long> ids, LongConsumer action) {
    int done = 0;
    for (Long id : ids) {
      try {
        tx.executeWithoutResult(s -> action.accept(id));
        done++;
      } catch (BusinessRuleException | ResourceNotFoundException ex) {
        LOG.warn("Collections record {} not processed: {}", id, ex.getMessage());
      }
    }
    return done;
  }
}
