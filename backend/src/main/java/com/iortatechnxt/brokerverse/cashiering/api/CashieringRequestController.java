package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.dto.RequestDtos.AcceptBody;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RequestDtos.CollectorRequestResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RequestDtos.ConfirmBody;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RequestDtos.PaymentReversalResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RequestDtos.RefundValidationResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RequestDtos.RejectBody;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RequestDtos.RequestCounts;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.DispositionResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.UnappliedDtos.UnappliedResponse;
import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentReversal;
import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPaymentReversals;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringRefundValidationSource;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The requests Cashiering receives from other modules: collector disposition requests on unapplied
 * items from Collections (BRCLXN.030-033), refund validations from Payment Requests (MKT 1.11.0)
 * and payment reversals from ACSL (ACSL 2.6.0-2.6.1). Cashiers accept or reject the collector
 * requests and answer the validations ({@code CASH_DISPOSITION}); approvers decide the reversals
 * ({@code CASH_APPROVE}).
 */
@RestController
@RequestMapping("/api/v1/cashiering")
public class CashieringRequestController {

  private static final String VIEW =
      "hasAnyAuthority('CASH_DISPOSITION', 'CASH_DISPOSITION_APPROVE', 'CASH_APPROVE',"
          + " 'CASH_APPLY')";

  private final CollectorRequestService collectorRequests;
  private final CashieringRefundValidationSource validations;
  private final CashieringPaymentReversals reversals;

  /**
   * Creates the controller.
   *
   * @param collectorRequests collector requests
   * @param validations refund validations
   * @param reversals payment reversals
   */
  public CashieringRequestController(
      CollectorRequestService collectorRequests,
      CashieringRefundValidationSource validations,
      CashieringPaymentReversals reversals) {
    this.collectorRequests = collectorRequests;
    this.validations = validations;
    this.reversals = reversals;
  }

  /**
   * Open work per kind of request (tab counts).
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/requests/counts")
  @PreAuthorize(VIEW)
  public RequestCounts counts(@RequestParam Long companyId) {
    return new RequestCounts(
        collectorRequests.queuedCount(companyId),
        validations.openCount(companyId),
        reversals.submittedCount(companyId));
  }

  /**
   * Collector requests in some statuses.
   *
   * @param companyId company
   * @param status statuses (default QUEUED)
   * @param q request, invoice or requester
   * @param page page
   * @param size size
   * @return requests, newest first
   */
  @GetMapping("/collector-requests")
  @PreAuthorize(VIEW)
  public PageResponse<CollectorRequestResponse> collectorRequests(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<CollectorRequest.Status> status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<CollectorRequest> found =
        collectorRequests.list(
            companyId,
            status == null || status.isEmpty() ? List.of(CollectorRequest.Status.QUEUED) : status,
            q,
            CashAccess.page(page, size));
    Map<Long, Unapplied> items = collectorRequests.itemsOf(found.getContent());
    return PageResponse.of(
        found, r -> CollectorRequestResponse.from(r, items.get(r.getUnappliedId())));
  }

  /**
   * Collector requests of an unapplied item.
   *
   * @param id item
   * @return requests, newest first
   */
  @GetMapping("/unapplied/{id}/collector-requests")
  @PreAuthorize(CashAccess.VIEW)
  public List<CollectorRequestResponse> ofItem(@PathVariable Long id) {
    List<CollectorRequest> found = collectorRequests.forItem(id);
    Map<Long, Unapplied> items = collectorRequests.itemsOf(found);
    return found.stream()
        .map(r -> CollectorRequestResponse.from(r, items.get(r.getUnappliedId())))
        .toList();
  }

  /**
   * Accepts a collector request: assigns the disposition (and submits it when asked).
   *
   * @param id request
   * @param body disposition type and target fields
   * @return the disposition
   */
  @PostMapping("/collector-requests/{id}/accept")
  @PreAuthorize(CashAccess.DISPOSITION)
  public DispositionResponse accept(@PathVariable Long id, @Valid @RequestBody AcceptBody body) {
    return DispositionResponse.from(collectorRequests.accept(id, body.toAcceptance()));
  }

  /**
   * Rejects a collector request.
   *
   * @param id request
   * @param body reason
   * @return the request
   */
  @PostMapping("/collector-requests/{id}/reject")
  @PreAuthorize(CashAccess.DISPOSITION)
  public CollectorRequestResponse rejectRequest(
      @PathVariable Long id, @Valid @RequestBody RejectBody body) {
    CollectorRequest r = collectorRequests.reject(id, body.reason());
    return CollectorRequestResponse.from(
        r, collectorRequests.itemsOf(List.of(r)).get(r.getUnappliedId()));
  }

  /**
   * Refund validations in some statuses.
   *
   * @param companyId company
   * @param status statuses (default OPEN)
   * @param page page
   * @param size size
   * @return validations, newest first
   */
  @GetMapping("/refund-validations")
  @PreAuthorize(VIEW)
  public PageResponse<RefundValidationResponse> refundValidations(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<RefundCheck.Status> status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        validations.list(
            companyId,
            status == null || status.isEmpty() ? List.of(RefundCheck.Status.OPEN) : status,
            CashAccess.page(page, size)),
        RefundValidationResponse::from);
  }

  /**
   * Unapplied items that may hold the premium of a refund validation.
   *
   * @param id validation
   * @return items, newest first
   */
  @GetMapping("/refund-validations/{id}/candidates")
  @PreAuthorize(VIEW)
  public List<UnappliedResponse> candidates(@PathVariable Long id) {
    return validations.candidates(id).stream().map(u -> UnappliedResponse.from(u, null)).toList();
  }

  /**
   * Confirms a refund validation.
   *
   * @param id validation
   * @param body unapplied item, new AR number and remarks
   * @return the validation
   */
  @PostMapping("/refund-validations/{id}/confirm")
  @PreAuthorize(CashAccess.DISPOSITION)
  public RefundValidationResponse confirm(
      @PathVariable Long id, @Valid @RequestBody ConfirmBody body) {
    return RefundValidationResponse.from(
        validations.confirm(id, body.unappliedId(), body.newArNo(), body.remarks()));
  }

  /**
   * Rejects a refund validation.
   *
   * @param id validation
   * @param body reason
   * @return the validation
   */
  @PostMapping("/refund-validations/{id}/reject")
  @PreAuthorize(CashAccess.DISPOSITION)
  public RefundValidationResponse rejectValidation(
      @PathVariable Long id, @Valid @RequestBody RejectBody body) {
    return RefundValidationResponse.from(validations.reject(id, body.reason()));
  }

  /**
   * Payment reversal requests in some statuses.
   *
   * @param companyId company
   * @param status statuses (default SUBMITTED)
   * @param page page
   * @param size size
   * @return requests, newest first
   */
  @GetMapping("/payment-reversals")
  @PreAuthorize(VIEW)
  public PageResponse<PaymentReversalResponse> paymentReversals(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<PaymentReversal.Status> status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        reversals.list(
            companyId,
            status == null || status.isEmpty() ? List.of(PaymentReversal.Status.SUBMITTED) : status,
            CashAccess.page(page, size)),
        PaymentReversalResponse::from);
  }

  /**
   * Approves a payment reversal: reverses the applications and moves the money to unapplied.
   *
   * @param id request
   * @return the request
   */
  @PostMapping("/payment-reversals/{id}/approve")
  @PreAuthorize(CashAccess.APPROVE)
  public PaymentReversalResponse approveReversal(@PathVariable Long id) {
    return PaymentReversalResponse.from(reversals.approve(id));
  }

  /**
   * Rejects a payment reversal.
   *
   * @param id request
   * @param body reason
   * @return the request
   */
  @PostMapping("/payment-reversals/{id}/reject")
  @PreAuthorize(CashAccess.APPROVE)
  public PaymentReversalResponse rejectReversal(
      @PathVariable Long id, @Valid @RequestBody RejectBody body) {
    return PaymentReversalResponse.from(reversals.reject(id, body.reason()));
  }
}
