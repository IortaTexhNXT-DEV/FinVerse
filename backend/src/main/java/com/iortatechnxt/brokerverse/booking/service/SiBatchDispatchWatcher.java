package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.SiBatchRepository;
import com.iortatechnxt.brokerverse.booking.service.SiBatchService.SiBatchQueued;
import com.iortatechnxt.brokerverse.messaging.service.MailDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Follows the e-mail of a service-invoice batch once the batch has committed (G09): delivers it now
 * when immediate dispatch is on and records the outcome (sent, or failed with the reason) on the
 * batch. A message waiting for a retry is delivered by the MAIL_DISPATCH job; the batch screen then
 * records the outcome.
 */
@Component
public class SiBatchDispatchWatcher {

  private final SiBatchRepository batches;
  private final SiBatchService service;
  private final MailDispatcher dispatcher;
  private final TransactionTemplate tx;
  private final boolean dispatchOnCommit;

  /**
   * Creates the watcher.
   *
   * @param batches batches
   * @param service outcome recording
   * @param dispatcher mail delivery
   * @param txManager transaction manager
   * @param dispatchOnCommit deliver right after the commit ({@code
   *     brokerverse.mail.dispatch-on-commit})
   */
  public SiBatchDispatchWatcher(
      SiBatchRepository batches,
      SiBatchService service,
      MailDispatcher dispatcher,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.mail.dispatch-on-commit:true}") boolean dispatchOnCommit) {
    this.batches = batches;
    this.service = service;
    this.dispatcher = dispatcher;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.dispatchOnCommit = dispatchOnCommit;
  }

  /**
   * Delivers and records the outcome of a queued batch e-mail.
   *
   * @param event queued e-mail
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onQueued(SiBatchQueued event) {
    if (!dispatchOnCommit) {
      return;
    }
    dispatcher.dispatch(event.messageId());
    tx.executeWithoutResult(
        s -> batches.findById(event.batchId()).ifPresent(service::recordOutcome));
  }
}
