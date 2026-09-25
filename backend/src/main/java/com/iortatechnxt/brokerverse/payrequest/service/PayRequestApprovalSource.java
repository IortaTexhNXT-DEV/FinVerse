package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestTrail;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Requests waiting for a Marketing or HR approval in the approval inbox (MKT 1.16.0-1.16.3): FOR
 * APPROVAL for the holders of {@code PRQ_APPROVE}, HR APPROVAL for the holders of {@code
 * PRQ_HR_APPROVE}; never shown to the user who raised, submitted or endorsed the request (the
 * four-eyes rule of the approval).
 */
@Component
@Transactional(readOnly = true)
public class PayRequestApprovalSource implements PendingApprovalSource {

  private final PaymentRequestRepository requests;

  /**
   * Creates the source.
   *
   * @param requests requests
   */
  public PayRequestApprovalSource(PaymentRequestRepository requests) {
    this.requests = requests;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    boolean marketing = viewer.can("PRQ_APPROVE");
    boolean hr = viewer.can("PRQ_HR_APPROVE");
    if (!marketing && !hr) {
      return List.of();
    }
    return requests
        .findByStageInOrderByIdAsc(List.of(RequestStage.FOR_APPROVAL, RequestStage.HR_APPROVAL))
        .stream()
        .filter(r -> r.getStage() == RequestStage.FOR_APPROVAL ? marketing : hr)
        .filter(r -> mayApprove(viewer, r))
        .map(PayRequestApprovalSource::pending)
        .toList();
  }

  private static boolean mayApprove(ApprovalViewer viewer, PaymentRequest r) {
    RequestTrail t = r.trailOrNone();
    return Stream.of(r.getCreatedBy(), t.submittedBy(), t.reviewedBy(), t.approvedBy())
        .filter(Objects::nonNull)
        .allMatch(viewer::mayApproveItemOf);
  }

  private static PendingApproval pending(PaymentRequest r) {
    RequestTrail t = r.trailOrNone();
    return new PendingApproval(
        PayRequests.MODULE,
        r.getStage() == RequestStage.HR_APPROVAL ? "Cash advance (HR)" : kindLabel(r),
        r.getRequestNo(),
        r.getPayee().name(),
        r.getAmount(),
        r.getContent().currency(),
        t.reviewedBy() == null ? r.getCreatedBy() : t.reviewedBy(),
        t.reviewedAt() == null ? r.getCreatedAt() : t.reviewedAt(),
        r.getCompanyId(),
        PayRequests.link(r.getId()));
  }

  private static String kindLabel(PaymentRequest r) {
    return switch (r.getKind()) {
      case REFUND -> "Refund request";
      case CASH_ADVANCE -> "Cash advance";
      default -> "Check cancellation";
    };
  }
}
