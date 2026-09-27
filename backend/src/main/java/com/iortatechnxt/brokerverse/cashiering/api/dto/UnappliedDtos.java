package com.iortatechnxt.brokerverse.cashiering.api.dto;

import com.iortatechnxt.brokerverse.cashiering.domain.Disposition;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionTypeRule;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Request and response records of the unapplied payments workbench (CSHID.024/025). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class UnappliedDtos {

  private UnappliedDtos() {}

  /**
   * An unapplied item.
   *
   * @param id id
   * @param reference UNP- reference
   * @param origin origin
   * @param receiptId AR
   * @param paymentId payment
   * @param invoiceNo invoice it came from
   * @param clientCode client
   * @param payorName payor
   * @param salesUnit marketing unit
   * @param currency currency
   * @param amount amount
   * @param balance balance
   * @param stage workflow stage (tab)
   * @param dispositionHint suggested disposition
   * @param sourceModule source module
   * @param sourceRef source reference
   * @param remarks remarks
   * @param createdAt created
   * @param current current disposition, may be null
   */
  public record UnappliedResponse(
      Long id,
      String reference,
      String origin,
      Long receiptId,
      Long paymentId,
      String invoiceNo,
      String clientCode,
      String payorName,
      String salesUnit,
      String currency,
      BigDecimal amount,
      BigDecimal balance,
      String stage,
      String dispositionHint,
      String sourceModule,
      String sourceRef,
      String remarks,
      Instant createdAt,
      DispositionResponse current) {

    /**
     * Maps an item.
     *
     * @param u item
     * @param current current disposition, may be null
     * @return response
     */
    public static UnappliedResponse from(Unapplied u, Disposition current) {
      return new UnappliedResponse(
          u.getId(),
          u.getReference(),
          u.getOrigin().name(),
          u.getReceiptId(),
          u.getPaymentId(),
          u.getInvoiceNo(),
          u.getClientCode(),
          u.getPayorName(),
          u.getSalesUnit(),
          u.getCurrency(),
          u.getAmount(),
          u.getBalance(),
          u.getStage(),
          u.getDispositionHint(),
          u.getSourceModule(),
          u.getSourceRef(),
          u.getRemarks(),
          u.getCreatedAt(),
          current == null ? null : DispositionResponse.from(current));
    }
  }

  /**
   * A disposition.
   *
   * @param id id
   * @param dispositionType type
   * @param action action
   * @param amount amount
   * @param targetInvoiceNo invoice to apply to
   * @param targetClientCode client to reclass to
   * @param targetUnit unit to transfer to
   * @param payeeName refund payee
   * @param remarks remarks
   * @param status status
   * @param requestedBy requester
   * @param requestedAt requested
   * @param approvedBy approver
   * @param approvedAt approved
   * @param completedAt completed
   * @param disbursementRequestNo refund request
   * @param journalBatchNo journal
   * @param reversalReason reversal reason
   */
  public record DispositionResponse(
      Long id,
      String dispositionType,
      String action,
      BigDecimal amount,
      String targetInvoiceNo,
      String targetClientCode,
      String targetUnit,
      String payeeName,
      String remarks,
      String status,
      String requestedBy,
      Instant requestedAt,
      String approvedBy,
      Instant approvedAt,
      Instant completedAt,
      String disbursementRequestNo,
      String journalBatchNo,
      String reversalReason) {

    /**
     * Maps a disposition.
     *
     * @param d disposition
     * @return response
     */
    public static DispositionResponse from(Disposition d) {
      return new DispositionResponse(
          d.getId(),
          d.getDispositionType(),
          d.getAction().name(),
          d.getAmount(),
          d.getTargetInvoiceNo(),
          d.getTargetClientCode(),
          d.getTargetUnit(),
          d.getPayeeName(),
          d.getRemarks(),
          d.getStatus().name(),
          d.getCreatedBy(),
          d.getCreatedAt(),
          d.getApprovedBy(),
          d.getApprovedAt(),
          d.getCompletedAt(),
          d.getDisbursementRequestNo(),
          d.getJournalBatchNo(),
          d.getReversalReason());
    }
  }

  /**
   * A disposition type and its rule.
   *
   * @param code type code
   * @param action action
   * @param requiresApproval needs the team leader
   * @param description description
   */
  public record DispositionTypeResponse(
      String code, String action, boolean requiresApproval, String description) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return response
     */
    public static DispositionTypeResponse from(DispositionTypeRule r) {
      return new DispositionTypeResponse(
          r.getTypeCode(), r.getAction().name(), r.isRequiresApproval(), r.getDescription());
    }
  }

  /**
   * A disposition to assign or update.
   *
   * @param dispositionType type (LOV DISPOSITION_TYPE)
   * @param amount amount
   * @param targetInvoiceNo invoice to apply to
   * @param targetClientCode client to reclass to
   * @param targetUnit unit to transfer to
   * @param payeeName refund payee
   * @param remarks remarks
   */
  public record DispositionRequest(
      @NotBlank String dispositionType,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @Size(max = 40) String targetInvoiceNo,
      @Size(max = 30) String targetClientCode,
      @Size(max = 40) String targetUnit,
      @Size(max = 250) String payeeName,
      @Size(max = 250) String remarks) {}

  /**
   * A bulk action on items.
   *
   * @param ids items
   */
  public record BulkRequest(@NotEmpty List<Long> ids) {}

  /**
   * Outcome of a bulk action.
   *
   * @param done items processed
   * @param failures failure message per item reference
   */
  public record BulkResult(List<String> done, List<String> failures) {}
}
