package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Status;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.DisbursementStatusChanged;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Follows the payment of the requests sent to Disbursement (MKT 1.20.0, 2.26.0; DIS 2.20.0): every
 * {@code DisbursementStatusChanged} of a payrequest request is kept on the request (gateway status,
 * DV number and stage, instrument status); PAID moves it to DISBURSED and tells the requester;
 * RETURNED or CANCELLED before payment sends it back to its preparer; a cancellation after payment
 * is recorded on the request and on its check-cancellation request.
 */
@Component
public class PayRequestDisbursementFeedback {

  private final PaymentRequestRepository requests;
  private final WorkflowService workflow;
  private final PayRequestNotifier notifier;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param requests requests
   * @param workflow workflow engine
   * @param notifier notifications
   * @param audit audit trail
   * @param clock clock
   */
  public PayRequestDisbursementFeedback(
      PaymentRequestRepository requests,
      WorkflowService workflow,
      PayRequestNotifier notifier,
      AuditTrailService audit,
      Clock clock) {
    this.requests = requests;
    this.workflow = workflow;
    this.notifier = notifier;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * A payment request of payrequest changed status in Disbursement.
   *
   * @param event status change
   */
  @EventListener
  public void on(DisbursementStatusChanged event) {
    if (PayRequests.MODULE.equals(event.sourceModule())) {
      requests
          .findByRequestNo(PaymentRequest.requestNoOf(event.sourceRef()))
          .filter(r -> isCurrent(r, event))
          .ifPresent(r -> follow(r, event));
    }
  }

  private void follow(PaymentRequest request, DisbursementStatusChanged event) {
    request.track(
        request
            .trackOrNone()
            .status(event.requestNo(), event.status().name(), event.dvNo(), event.reason())
            .tracked(event.dvStatus(), event.instrumentStatus()));
    audit.record(
        PayRequests.ENTITY,
        request.getRequestNo(),
        AuditAction.UPDATE,
        "Disbursement " + event.status() + (event.dvNo() == null ? "" : ", DV " + event.dvNo()));
    boolean waiting = request.getStage() == RequestStage.SENT_TO_DISBURSEMENT;
    if (event.status() == Status.PAID && waiting) {
      request.disbursed(clock.instant());
      workflow.systemTransition(
          PayRequests.ENTITY, key(request), "disbursed", PayRequests.note(event.dvNo()));
      notifier.requester(request, "disbursed (DV " + event.dvNo() + ")");
    } else if (isBack(event.status()) && waiting) {
      workflow.systemTransition(
          PayRequests.ENTITY,
          key(request),
          "disbursement_returned",
          PayRequests.note(event.reason()));
      notifier.requester(request, "returned by Disbursement: " + event.reason());
    } else if (event.status() == Status.CANCELLED) {
      notifier.requester(request, "payment cancelled by Disbursement: " + event.reason());
    }
  }

  /** Events of an earlier sending (returned, then sent again) are ignored. */
  private static boolean isCurrent(PaymentRequest request, DisbursementStatusChanged event) {
    return request.currentSendRef().equals(event.sourceRef());
  }

  private static boolean isBack(Status status) {
    return status == Status.RETURNED || status == Status.CANCELLED;
  }

  private static String key(PaymentRequest request) {
    return String.valueOf(request.getId());
  }
}
