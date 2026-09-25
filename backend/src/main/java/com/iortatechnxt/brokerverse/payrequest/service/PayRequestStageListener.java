package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of a request's work case on the request, inside the transition's transaction
 * (MKT 2.26.0), and carries out the generic actions run from the workflow panel: a return records
 * its reason and tells the requester (MKT 1.8.0); a cancellation releases the AR numbers of the
 * refund lines (MKT 1.17.0, 2.23.0).
 */
@Component
public class PayRequestStageListener {

  private final PaymentRequestRepository requests;
  private final PayRequestNotifier notifier;

  /**
   * Creates the listener.
   *
   * @param requests requests
   * @param notifier notifications
   */
  public PayRequestStageListener(PaymentRequestRepository requests, PayRequestNotifier notifier) {
    this.requests = requests;
    this.notifier = notifier;
  }

  /**
   * Follows a stage change of a request's work case.
   *
   * @param event stage change
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (PayRequests.ENTITY.equals(event.entityType())) {
      requests.findLoaded(Long.valueOf(event.entityId())).ifPresent(r -> follow(r, event));
    }
  }

  private void follow(PaymentRequest request, WorkCaseTransitioned event) {
    RequestStage stage = RequestStage.valueOf(event.toStage());
    request.moveTo(stage);
    if ("return".equals(event.action())) {
      request.record(request.trailOrNone().returned(event.reasonCode(), event.comment()));
      notifier.requester(request, "returned (" + event.reasonCode() + ")");
    } else if (stage == RequestStage.CANCELLED) {
      notifier.requester(request, "cancelled");
    }
  }
}
