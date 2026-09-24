package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
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
 * {@code HOLD_EXPIRY} (RMTID.021): releases the active holds whose hold-until date has passed, so
 * their invoices are extracted again, and warns the requestor and the assigned processor of holds
 * reaching their date the next day ({@code HOLD_EXPIRING}). Each hold is processed in its own
 * transaction; a hold that cannot be released (e.g. the invoice is locked by another team) is
 * retried on the next run.
 */
@Component
public class HoldExpiryJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "HOLD_EXPIRY";

  private static final Logger LOG = LoggerFactory.getLogger(HoldExpiryJob.class);

  private final HoldRequestRepository holds;
  private final HoldService service;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param holds hold requests
   * @param service hold actions
   * @param txManager transactions
   * @param cron schedule ({@code brokerverse.jobs.hold-expiry-cron})
   */
  public HoldExpiryJob(
      HoldRequestRepository holds,
      HoldService service,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.hold-expiry-cron:-}") String cron) {
    this.holds = holds;
    this.service = service;
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
    return "Releases expired remittance holds and warns of holds expiring tomorrow";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int expired =
        each(
            holds.findByStageAndHoldUntilBeforeOrderByIdAsc(HoldStage.ACTIVE, businessDate),
            service::expire);
    int warned =
        each(
            holds.findByStageAndHoldUntilLessThanEqualAndExpiryNotifiedFalseOrderByIdAsc(
                HoldStage.ACTIVE, businessDate.plusDays(1)),
            service::warnExpiring);
    return new JobOutcome(
        expired + warned, expired + " hold(s) released, " + warned + " expiry warning(s) sent");
  }

  private int each(List<HoldRequest> due, LongConsumer action) {
    int done = 0;
    for (HoldRequest hold : due) {
      try {
        tx.executeWithoutResult(s -> action.accept(hold.getId()));
        done++;
      } catch (BusinessRuleException | ResourceNotFoundException ex) {
        LOG.warn("Hold {} not processed: {}", hold.getRequestNo(), ex.getMessage());
      }
    }
    return done;
  }
}
