package com.iortatechnxt.brokerverse.cashiering.api.dto;

import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentReversal;
import com.iortatechnxt.brokerverse.cashiering.domain.RefundCheck;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService.Acceptance;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Request and response records of the requests Cashiering receives from other modules: collector
 * disposition requests (BRCLXN.030-033), refund validations (MKT 1.11.0) and payment reversals
 * (ACSL 2.6.0-2.6.1).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RequestDtos {

  private RequestDtos() {}

  /**
   * A collector request.
   *
   * @param id id
   * @param requestNo CRQ- number
   * @param unappliedId unapplied item
   * @param unappliedRef its reference
   * @param currency currency of the item
   * @param balance balance of the item
   * @param stage stage of the item
   * @param payorName payor of the item
   * @param action APPLY_TO_INVOICE, REFUND, RECLASS or TRANSFER
   * @param invoiceNo target invoice
   * @param amount amount, null for the whole balance
   * @param requestedBy collector
   * @param requestedAt time
   * @param source requesting module
   * @param sourceRef its reference
   * @param remarks remarks
   * @param status QUEUED, ACCEPTED, REJECTED or APPLIED
   * @param dispositionId disposition assigned from it
   * @param decidedBy cashier
   * @param decidedAt time
   * @param decisionNote reason or result
   */
  public record CollectorRequestResponse(
      Long id,
      String requestNo,
      Long unappliedId,
      String unappliedRef,
      String currency,
      BigDecimal balance,
      String stage,
      String payorName,
      String action,
      String invoiceNo,
      BigDecimal amount,
      String requestedBy,
      Instant requestedAt,
      String source,
      String sourceRef,
      String remarks,
      String status,
      Long dispositionId,
      String decidedBy,
      Instant decidedAt,
      String decisionNote) {

    /**
     * Maps a request.
     *
     * @param r request
     * @param item its unapplied item, may be null
     * @return response
     */
    public static CollectorRequestResponse from(CollectorRequest r, Unapplied item) {
      return new CollectorRequestResponse(
          r.getId(),
          r.getRequestNo(),
          r.getUnappliedId(),
          item == null ? null : item.getReference(),
          item == null ? null : item.getCurrency(),
          item == null ? null : item.getBalance(),
          item == null ? null : item.getStage(),
          item == null ? null : item.getPayorName(),
          r.getAction(),
          r.getInvoiceNo(),
          r.getAmount(),
          r.getRequestedBy(),
          r.getCreatedAt(),
          r.getSource(),
          r.getSourceRef(),
          r.getRemarks(),
          r.getStatus().name(),
          r.getDispositionId(),
          r.getDecidedBy(),
          r.getDecidedAt(),
          r.getDecisionNote());
    }
  }

  /**
   * A cashier's acceptance of a collector request.
   *
   * @param dispositionType disposition type, null for the default of the action
   * @param amount amount, null for the requested amount or the whole balance
   * @param targetClientCode client to reclass to
   * @param targetUnit marketing unit to transfer to
   * @param payeeName refund payee
   * @param remarks remarks
   * @param submit submit the disposition at once
   */
  public record AcceptBody(
      @Size(max = 40) String dispositionType,
      @DecimalMin(value = "0.01") BigDecimal amount,
      @Size(max = 30) String targetClientCode,
      @Size(max = 40) String targetUnit,
      @Size(max = 250) String payeeName,
      @Size(max = 250) String remarks,
      boolean submit) {

    /**
     * The service input.
     *
     * @return acceptance
     */
    public Acceptance toAcceptance() {
      return new Acceptance(
          dispositionType, amount, targetClientCode, targetUnit, payeeName, remarks, submit);
    }
  }

  /**
   * A reason (rejection).
   *
   * @param reason reason
   */
  public record RejectBody(@NotNull @Size(min = 1, max = 1000) String reason) {}

  /**
   * A refund validation.
   *
   * @param id id
   * @param taskNo RVL- number
   * @param sourceModule requesting module
   * @param sourceRef its reference
   * @param invoiceNo cancelled invoice
   * @param arNo AR of the payment to refund
   * @param clientCode client
   * @param currency currency
   * @param amount refund amount
   * @param requestedBy user
   * @param requestedAt time
   * @param requestRemarks remarks of the request
   * @param status OPEN, CONFIRMED or REJECTED
   * @param unappliedId unapplied item confirmed
   * @param newArNo new AR number
   * @param decidedBy cashier
   * @param decidedAt time
   * @param resultRemarks remarks of the answer
   */
  public record RefundValidationResponse(
      Long id,
      String taskNo,
      String sourceModule,
      String sourceRef,
      String invoiceNo,
      String arNo,
      String clientCode,
      String currency,
      BigDecimal amount,
      String requestedBy,
      Instant requestedAt,
      String requestRemarks,
      String status,
      Long unappliedId,
      String newArNo,
      String decidedBy,
      Instant decidedAt,
      String resultRemarks) {

    /**
     * Maps a validation.
     *
     * @param t validation
     * @return response
     */
    public static RefundValidationResponse from(RefundCheck t) {
      return new RefundValidationResponse(
          t.getId(),
          t.getTaskNo(),
          t.getSourceModule(),
          t.getSourceRef(),
          t.getInvoiceNo(),
          t.getArNo(),
          t.getClientCode(),
          t.getCurrency(),
          t.getAmount(),
          t.getRequestedBy(),
          t.getCreatedAt(),
          t.getRequestRemarks(),
          t.getStatus().name(),
          t.getUnappliedId(),
          t.getNewArNo(),
          t.getDecidedBy(),
          t.getDecidedAt(),
          t.getResultRemarks());
    }
  }

  /**
   * A cashier's confirmation of a refund validation.
   *
   * @param unappliedId unapplied item holding the premium
   * @param newArNo new AR number, null for the AR of the item
   * @param remarks remarks
   */
  public record ConfirmBody(
      @NotNull Long unappliedId,
      @Size(max = 40) String newArNo,
      @Size(max = 1000) String remarks) {}

  /**
   * A payment reversal request.
   *
   * @param id id
   * @param requestNo PRV- number
   * @param sourceModule requesting module
   * @param sourceRef its reference
   * @param invoiceNo invoice
   * @param receiptNo receipt
   * @param currency currency
   * @param amount amount, null for the whole application
   * @param valueDate value date
   * @param reason reason
   * @param requestedBy user
   * @param requestedAt time
   * @param status SUBMITTED, APPROVED or REJECTED
   * @param reversedAmount amount reversed
   * @param unappliedId unapplied item created
   * @param decidedBy approver
   * @param decidedAt time
   * @param decisionNote rejection reason
   */
  public record PaymentReversalResponse(
      Long id,
      String requestNo,
      String sourceModule,
      String sourceRef,
      String invoiceNo,
      String receiptNo,
      String currency,
      BigDecimal amount,
      LocalDate valueDate,
      String reason,
      String requestedBy,
      Instant requestedAt,
      String status,
      BigDecimal reversedAmount,
      Long unappliedId,
      String decidedBy,
      Instant decidedAt,
      String decisionNote) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return response
     */
    public static PaymentReversalResponse from(PaymentReversal r) {
      return new PaymentReversalResponse(
          r.getId(),
          r.getRequestNo(),
          r.getSourceModule(),
          r.getSourceRef(),
          r.getInvoiceNo(),
          r.getReceiptNo(),
          r.getCurrency(),
          r.getAmount(),
          r.getValueDate(),
          r.getReason(),
          r.getRequestedBy(),
          r.getCreatedAt(),
          r.getStatus().name(),
          r.getReversedAmount(),
          r.getUnappliedId(),
          r.getDecidedBy(),
          r.getDecidedAt(),
          r.getDecisionNote());
    }
  }

  /**
   * Open work of the requests screen (tab counts).
   *
   * @param collectorRequests queued collector requests
   * @param refundValidations open refund validations
   * @param paymentReversals submitted payment reversals
   */
  public record RequestCounts(
      long collectorRequests, long refundValidations, long paymentReversals) {}
}
