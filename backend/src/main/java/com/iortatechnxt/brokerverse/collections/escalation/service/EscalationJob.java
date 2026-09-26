package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRepository;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@code CLX_ESCALATION} (BRCLXN.049, cron {@code brokerverse.jobs.clx-escalation-cron}, 23:00 PHT
 * after the promise check): applies every authorized rule in force and escalates the accounts that
 * meet it, once per rule, invoice and month; then closes the open escalations whose accounts are
 * collected. Each escalation is raised or closed in its own transaction.
 */
@Component
public class EscalationJob implements ManagedJob {

  /** Job name. */
  public static final String JOB_NAME = "CLX_ESCALATION";

  private static final Logger LOG = LoggerFactory.getLogger(EscalationJob.class);

  private final EscalationEngine engine;
  private final EscalationService escalations;
  private final EscalationRepository repository;
  private final TransactionTemplate tx;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param engine rules
   * @param escalations escalation cases
   * @param repository escalations (open ones)
   * @param txManager transactions
   * @param cron schedule ({@code brokerverse.jobs.clx-escalation-cron})
   */
  public EscalationJob(
      EscalationEngine engine,
      EscalationService escalations,
      EscalationRepository repository,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.jobs.clx-escalation-cron:-}") String cron) {
    this.engine = engine;
    this.escalations = escalations;
    this.repository = repository;
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
    return "Escalates the accounts meeting the escalation rules and closes collected ones (BRCLXN.049)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    int raised = 0;
    for (EscalationRule rule : engine.rulesOn(businessDate)) {
      for (Candidate account : engine.matches(rule, businessDate)) {
        if (Boolean.TRUE.equals(
            safely(
                account.invoiceNo(),
                () -> escalations.raiseForRule(rule, account, businessDate).isPresent()))) {
          raised++;
        }
      }
    }
    int closed = 0;
    for (Long id : repository.idsIn(EscalationService.OPEN)) {
      if (Boolean.TRUE.equals(safely(id, () -> escalations.autoCloseIfCollected(id)))) {
        closed++;
      }
    }
    return new JobOutcome(
        raised + closed, raised + " escalation(s) raised, " + closed + " closed as collected");
  }

  private Boolean safely(Object key, Supplier<Boolean> work) {
    try {
      return tx.execute(s -> work.get());
    } catch (BusinessRuleException | ResourceNotFoundException ex) {
      LOG.warn("Escalation of {} not processed: {}", key, ex.getMessage());
      return Boolean.FALSE;
    }
  }
}
