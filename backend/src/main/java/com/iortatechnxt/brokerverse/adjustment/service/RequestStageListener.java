package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the {@code OPS_ENDORSEMENT} stage on the request, inside the transition's transaction,
 * and carries out the generic actions run from the workflow panel: a return records its reason and
 * tells the requester (ADJID.005/007); a cancellation releases the invoice's lock and pending
 * negative adjustment flag.
 */
@Component
public class RequestStageListener {

  private final EndorsementRequestRepository requests;
  private final InvoiceGuard guard;
  private final RequestNotifier notifier;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param requests requests
   * @param guard invoice lock and flag
   * @param notifier notifications
   * @param clock clock
   */
  public RequestStageListener(
      EndorsementRequestRepository requests,
      InvoiceGuard guard,
      RequestNotifier notifier,
      Clock clock) {
    this.requests = requests;
    this.guard = guard;
    this.notifier = notifier;
    this.clock = clock;
  }

  /**
   * Follows a stage change of a request's work case.
   *
   * @param event stage change
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (Adjustments.ENTITY.equals(event.entityType())) {
      requests.findById(Long.valueOf(event.entityId())).ifPresent(r -> follow(r, event));
    }
  }

  private void follow(EndorsementRequest request, WorkCaseTransitioned event) {
    RequestStage stage = RequestStage.valueOf(event.toStage());
    request.moveTo(stage);
    if (stage == RequestStage.RETURNED) {
      request.returned(event.reasonCode(), event.comment());
      notifier.requester(request, "returned (" + event.reasonCode() + ")");
    } else if (stage == RequestStage.CANCELLED) {
      guard.clearNegative(request);
      guard.release(request);
      request.closed(clock.instant());
      notifier.requester(request, "cancelled");
    }
  }
}
