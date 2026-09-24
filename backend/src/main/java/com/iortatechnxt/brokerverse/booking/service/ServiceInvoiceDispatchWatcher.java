package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.DispatchStatus;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceDispatch.ServiceInvoiceQueued;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.service.MailDispatcher;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Follows a service invoice e-mail once the booking has committed (BRNB.100b): delivers it now when
 * immediate dispatch is on (the outbox skips messages already delivered), records the outcome on
 * the service invoice and notifies its owner of success or of the failure with its reason. A
 * message still waiting for a retry is picked up by the MAIL_DISPATCH job; the register then shows
 * the live outcome.
 */
@Component
public class ServiceInvoiceDispatchWatcher {

  private final ServiceInvoiceRepository serviceInvoices;
  private final MessageService messages;
  private final MailDispatcher dispatcher;
  private final ServiceInvoiceDispatch dispatch;
  private final TransactionTemplate tx;
  private final boolean dispatchOnCommit;

  /**
   * Creates the watcher.
   *
   * @param serviceInvoices service invoices
   * @param messages outbox
   * @param dispatcher mail delivery
   * @param dispatch owner notifications
   * @param txManager transaction manager
   * @param dispatchOnCommit deliver right after the commit ({@code
   *     brokerverse.mail.dispatch-on-commit})
   */
  public ServiceInvoiceDispatchWatcher(
      ServiceInvoiceRepository serviceInvoices,
      MessageService messages,
      MailDispatcher dispatcher,
      ServiceInvoiceDispatch dispatch,
      PlatformTransactionManager txManager,
      @Value("${brokerverse.mail.dispatch-on-commit:true}") boolean dispatchOnCommit) {
    this.serviceInvoices = serviceInvoices;
    this.messages = messages;
    this.dispatcher = dispatcher;
    this.dispatch = dispatch;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.dispatchOnCommit = dispatchOnCommit;
  }

  /**
   * Delivers and records the outcome of a queued service invoice e-mail.
   *
   * @param event queued e-mail
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onQueued(ServiceInvoiceQueued event) {
    if (!dispatchOnCommit) {
      return;
    }
    dispatcher.dispatch(event.messageId());
    tx.executeWithoutResult(s -> record(event));
  }

  private void record(ServiceInvoiceQueued event) {
    ServiceInvoice si = serviceInvoices.findById(event.serviceInvoiceId()).orElse(null);
    if (si == null || si.getDispatchStatus() != DispatchStatus.QUEUED) {
      return;
    }
    OutboundMessage message = messages.get(event.messageId());
    if (message.getStatus() == MessageStatus.SENT) {
      si.dispatched(DispatchStatus.SENT, null);
      dispatch.notifyOwner(
          si,
          "Service invoice " + si.getSiNo() + " sent",
          "Sent to " + si.getRecipientName() + " (" + si.getRecipientEmail() + ")");
    } else if (message.getLastError() != null) {
      si.dispatched(DispatchStatus.FAILED, message.getLastError());
      dispatch.notifyOwner(
          si, "Service invoice " + si.getSiNo() + " not delivered", message.getLastError());
    }
  }
}
