package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Status;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reports Disbursement progress on the Operations gateway request of a payment request, which
 * publishes {@code OpsLedgerEvents.DisbursementStatusChanged} (with the DV stage and instrument
 * status) for the source module (design 7.2): a DV stage or instrument status is tracked, approval
 * assigns the DV number (DV_ASSIGNED), a paid instrument marks it PAID, a rejected or returned
 * request goes back RETURNED and a cancelled DV CANCELLED. Requests that did not come through the
 * gateway (encoded, uploaded) have no Operations request and are skipped. A status the Operations
 * request no longer allows (changed on the Operations queue screen) is left as it is.
 */
@Component
public class GatewaySync {

  private static final Set<Status> OPEN = EnumSet.of(Status.SENT, Status.ACKNOWLEDGED);
  private static final Set<Status> TRACKABLE =
      EnumSet.of(Status.SENT, Status.ACKNOWLEDGED, Status.DV_ASSIGNED, Status.PAID);

  private final DisbursementQueueService queue;

  /**
   * Creates the helper.
   *
   * @param queue Operations payment requests
   */
  public GatewaySync(DisbursementQueueService queue) {
    this.queue = queue;
  }

  /**
   * Records the DV stage and instrument status without changing the gateway status.
   *
   * @param request payment request
   * @param dvStage DV stage, may be null
   * @param instrument instrument status, may be null
   */
  public void track(IntakeRequest request, String dvStage, String instrument) {
    gateway(request, TRACKABLE).ifPresent(r -> queue.track(r.getId(), dvStage, instrument));
  }

  /**
   * The DV was approved (DV_ASSIGNED, design 7.2).
   *
   * @param request payment request
   * @param dvNo DV number
   */
  public void approved(IntakeRequest request, String dvNo) {
    gateway(request, OPEN).ifPresent(r -> queue.assignDv(r.getId(), dvNo));
    track(request, "APPROVED", null);
  }

  /**
   * The instrument reached a paid status (PAID).
   *
   * @param request payment request
   * @param instrument instrument status
   */
  public void paid(IntakeRequest request, String instrument) {
    gateway(request, EnumSet.of(Status.DV_ASSIGNED)).ifPresent(r -> queue.markPaid(r.getId()));
    track(request, null, instrument);
  }

  /**
   * The request or its DV was rejected or returned to the source (RETURNED).
   *
   * @param request payment request
   * @param reason reason
   */
  public void returned(IntakeRequest request, String reason) {
    gateway(request, EnumSet.of(Status.SENT, Status.ACKNOWLEDGED, Status.DV_ASSIGNED))
        .ifPresent(r -> queue.returnToSource(r.getId(), reason));
  }

  /**
   * The DV was cancelled, also after approval (CANCELLED, DIS 2.20.0): the source restores its
   * records.
   *
   * @param request payment request
   * @param reason reason
   */
  public void cancelled(IntakeRequest request, String reason) {
    gateway(request, TRACKABLE).ifPresent(r -> queue.cancel(r.getId(), reason));
  }

  /**
   * Documents released without a voucher (BIR 2307 certificates): DV_ASSIGNED with the request
   * number, then PAID, so the source releases its batch.
   *
   * @param request payment request
   */
  public void released(IntakeRequest request) {
    gateway(request, OPEN).ifPresent(r -> queue.assignDv(r.getId(), request.getRequestNo()));
    gateway(request, EnumSet.of(Status.DV_ASSIGNED)).ifPresent(r -> queue.markPaid(r.getId()));
  }

  /**
   * The status of the linked Operations request.
   *
   * @param request payment request
   * @return status, null when not a gateway request
   */
  public Status status(IntakeRequest request) {
    return request.getGatewayRequestId() == null
        ? null
        : queue.get(request.getGatewayRequestId()).getStatus();
  }

  private Optional<DisbursementRequest> gateway(IntakeRequest request, Set<Status> allowed) {
    if (request.getGatewayRequestId() == null) {
      return Optional.empty();
    }
    DisbursementRequest r = queue.get(request.getGatewayRequestId());
    return allowed.contains(r.getStatus()) ? Optional.of(r) : Optional.empty();
  }
}
