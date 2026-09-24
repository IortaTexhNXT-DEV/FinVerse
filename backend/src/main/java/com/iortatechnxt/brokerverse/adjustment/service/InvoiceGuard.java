package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService.FlagChange;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.NegativeAdjustmentPending;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The hold of an endorsement request on its invoice in the Operations ledger:
 *
 * <ul>
 *   <li>the lock (RMTID.040, ADJID.001): taken when the request is raised, so remittance cannot
 *       extract the invoice, released when the last open request of the invoice is posted or
 *       cancelled;
 *   <li>the {@code PENDING_NEG_ADJ} flag with the {@link NegativeAdjustmentPending} event
 *       (RMTID.020/035): raised when a request reducing the invoice is submitted, cleared when the
 *       last one is posted or cancelled, kept while an AR Insurer or a re-application is pending.
 * </ul>
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class InvoiceGuard {

  private final InvoiceLedgerService ledger;
  private final EndorsementRequestRepository requests;
  private final ApplicationEventPublisher events;
  private final CurrentUser currentUser;

  /**
   * Creates the guard.
   *
   * @param ledger ledger writes (lock, flags)
   * @param requests requests (other open requests of the invoice)
   * @param events event publisher
   * @param currentUser current user
   */
  public InvoiceGuard(
      InvoiceLedgerService ledger,
      EndorsementRequestRepository requests,
      ApplicationEventPublisher events,
      CurrentUser currentUser) {
    this.ledger = ledger;
    this.requests = requests;
    this.events = events;
    this.currentUser = currentUser;
  }

  /**
   * Locks the invoice of a request (fails with {@code INVOICE_LOCKED} when another module holds
   * it).
   *
   * @param request request
   */
  public void hold(EndorsementRequest request) {
    ledger.lock(
        invoiceOf(request),
        Adjustments.MODULE,
        "Endorsement request " + request.getRequestNo() + " open");
  }

  /**
   * Releases the lock unless another request on the invoice is still open.
   *
   * @param request request that ends
   */
  public void release(EndorsementRequest request) {
    if (!otherOpen(request, false)) {
      ledger.unlock(
          invoiceOf(request),
          Adjustments.MODULE,
          "Endorsement request " + request.getRequestNo() + " " + request.getStage());
    }
  }

  /**
   * Raises the pending negative adjustment of a request that reduces the invoice.
   *
   * @param request request
   */
  public void flagNegative(EndorsementRequest request) {
    if (!request.isNegative()) {
      return;
    }
    ledger.setFlag(
        new FlagChange(
            invoiceOf(request),
            InvoiceFlag.PENDING_NEG_ADJ,
            true,
            Adjustments.MODULE,
            "Negative adjustment requested: " + request.getRequestNo()));
    publish(request, true);
  }

  /**
   * Clears the pending negative adjustment unless another reducing request is still open.
   *
   * @param request request that ends
   */
  public void clearNegative(EndorsementRequest request) {
    if (!request.isNegative() || otherOpen(request, true)) {
      return;
    }
    ledger.setFlag(
        new FlagChange(
            invoiceOf(request),
            InvoiceFlag.PENDING_NEG_ADJ,
            false,
            Adjustments.MODULE,
            "Negative adjustment " + request.getRequestNo() + " " + request.getStage()));
    publish(request, false);
  }

  private void publish(EndorsementRequest request, boolean pending) {
    events.publishEvent(
        new NegativeAdjustmentPending(
            request.getCompanyId(),
            invoiceOf(request),
            request.getRequestNo(),
            pending,
            request.getCreatedBy() == null ? currentUser.username() : request.getCreatedBy()));
  }

  private boolean otherOpen(EndorsementRequest request, boolean negativeOnly) {
    return requests.findBySubjectInvoiceNoOrderByIdDesc(invoiceOf(request)).stream()
        .filter(r -> !r.getId().equals(request.getId()))
        .filter(r -> r.getStage().isOpen())
        .anyMatch(r -> !negativeOnly || r.isNegative());
  }

  private static String invoiceOf(EndorsementRequest request) {
    return request.getSubject().invoiceNo();
  }
}
