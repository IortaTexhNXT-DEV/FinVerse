package com.iortatechnxt.brokerverse.renewal.processing.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Creates the renewal account when a For Renewal renewal reaches processing (posted by the Team
 * Leader, routed straight through or uploaded), once the move is committed. A failure (missing
 * master data) is logged; Processing creates the account from the record page.
 */
@Component
public class RenewalAccountListener {

  private static final Logger LOG = LoggerFactory.getLogger(RenewalAccountListener.class);

  private final RenewalCandidateRepository candidates;
  private final RenewalAccountService accounts;
  private final TransactionTemplate tx;

  /**
   * Creates the listener.
   *
   * @param candidates renewals
   * @param accounts renewal accounts
   * @param txManager transactions
   */
  public RenewalAccountListener(
      RenewalCandidateRepository candidates,
      RenewalAccountService accounts,
      PlatformTransactionManager txManager) {
    this.candidates = candidates;
    this.accounts = accounts;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * A renewal reached processing.
   *
   * @param event stage change
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void on(WorkCaseTransitioned event) {
    if (!RenewalCodes.WORKFLOW.equals(event.workflowCode())
        || !RenewalStage.FOR_PROCESSING.name().equals(event.toStage())) {
      return;
    }
    try {
      tx.executeWithoutResult(
          s ->
              candidates
                  .findById(Long.valueOf(event.entityId()))
                  .filter(c -> c.getRenewalArn() == null)
                  .filter(c -> c.getDisposition().code() == RenewalDisposition.FOR_RENEWAL)
                  .ifPresent(accounts::create));
    } catch (RuntimeException e) {
      LOG.warn("Renewal account of renewal {} not created: {}", event.entityId(), e.getMessage());
    }
  }
}
