package com.iortatechnxt.brokerverse.cashiering.api.dto;

import com.iortatechnxt.brokerverse.cashiering.api.dto.ReceiptDtos.ApplicationResponse;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PdcStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.Payment;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItem;
import com.iortatechnxt.brokerverse.cashiering.domain.PickupRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Request and response records of payment intake, pre-booked, PDC and pick-up (CSHID.008/009/020).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class PaymentDtos {

  private PaymentDtos() {}

  /**
   * A preview request of the over-the-counter screen.
   *
   * @param companyId company
   * @param references invoice, ARN, policy or PN numbers
   * @param amount amount, may be null
   * @param currency currency, may be null
   * @param date payment date, today when null
   */
  public record PreviewRequest(
      @NotNull Long companyId,
      @NotEmpty List<@NotBlank String> references,
      BigDecimal amount,
      String currency,
      LocalDate date) {}

  /**
   * An over-the-counter payment (CSHID.001/020).
   *
   * @param companyId company
   * @param branchId receiving branch
   * @param references invoice, ARN, policy or PN numbers
   * @param payorCode payor code
   * @param payorName payor name
   * @param assuredName assured
   * @param currency currency
   * @param amount amount
   * @param paymentDate date
   * @param mode mode of payment
   * @param checkNo check number
   * @param checkBank bank
   * @param arClass AR class (OTC when absent)
   */
  public record ReceivePaymentRequest(
      @NotNull Long companyId,
      @NotNull Long branchId,
      List<@NotBlank String> references,
      @Size(max = 30) String payorCode,
      @NotBlank @Size(max = 250) String payorName,
      @Size(max = 250) String assuredName,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotNull LocalDate paymentDate,
      @NotNull PaymentMode mode,
      @Size(max = 40) String checkNo,
      @Size(max = 60) String checkBank,
      String arClass) {}

  /**
   * A payment.
   *
   * @param id id
   * @param paymentNo payment number
   * @param channel channel
   * @param batchRef upload job or batch
   * @param rowNo file row
   * @param reference main reference
   * @param otherRefs other references
   * @param payorName payor
   * @param amount amount
   * @param currency currency
   * @param valueDate date
   * @param matchCategory matching result
   * @param matchedRef invoice or ARN matched
   * @param appliedAmount applied
   * @param unappliedAmount not applied
   * @param receiptId AR
   * @param message message
   */
  public record PaymentResponse(
      Long id,
      String paymentNo,
      String channel,
      String batchRef,
      Integer rowNo,
      String reference,
      String otherRefs,
      String payorName,
      BigDecimal amount,
      String currency,
      LocalDate valueDate,
      String matchCategory,
      String matchedRef,
      BigDecimal appliedAmount,
      BigDecimal unappliedAmount,
      Long receiptId,
      String message) {

    /**
     * Maps a payment.
     *
     * @param p payment
     * @return response
     */
    public static PaymentResponse from(Payment p) {
      return new PaymentResponse(
          p.getId(),
          p.getPaymentNo(),
          p.getChannel().name(),
          p.getBatchRef(),
          p.getRowNo(),
          p.getReference(),
          p.getOtherRefs(),
          p.getPayorName(),
          p.getAmount(),
          p.getCurrency(),
          p.getValueDate(),
          p.getMatchCategory().name(),
          p.getMatchedRef(),
          p.getAppliedAmount(),
          p.getUnappliedAmount(),
          p.getReceiptId(),
          p.getMessage());
    }
  }

  /**
   * What happened to a payment received.
   *
   * @param payment payment
   * @param receiptId AR
   * @param receiptNo AR number
   * @param applications applications
   * @param unappliedId unapplied item, may be null
   * @param unappliedRef unapplied reference, may be null
   * @param prebookedId pre-booked item, may be null
   */
  public record IntakeResponse(
      PaymentResponse payment,
      Long receiptId,
      String receiptNo,
      List<ApplicationResponse> applications,
      Long unappliedId,
      String unappliedRef,
      Long prebookedId) {

    /**
     * Maps an intake result.
     *
     * @param r result
     * @return response
     */
    public static IntakeResponse from(IntakeResult r) {
      return new IntakeResponse(
          PaymentResponse.from(r.payment()),
          r.receipt() == null ? r.payment().getReceiptId() : r.receipt().getId(),
          r.receipt() == null ? null : r.receipt().getReceiptNo(),
          r.applications().stream().map(ApplicationResponse::from).toList(),
          r.unapplied() == null ? null : r.unapplied().getId(),
          r.unapplied() == null ? null : r.unapplied().getReference(),
          r.prebooked() == null ? null : r.prebooked().getId());
    }
  }

  /**
   * A pre-booked payment.
   *
   * @param id id
   * @param arn account
   * @param reference reference that matched
   * @param amount amount
   * @param currency currency
   * @param firstSeen payment date
   * @param ageDays days waiting
   * @param rematchCount attempts
   * @param lastRematch last attempt
   * @param status status
   * @param receiptId AR
   * @param paymentId payment
   * @param remarks remarks
   */
  public record PrebookedResponse(
      Long id,
      String arn,
      String reference,
      BigDecimal amount,
      String currency,
      LocalDate firstSeen,
      long ageDays,
      int rematchCount,
      Instant lastRematch,
      String status,
      Long receiptId,
      Long paymentId,
      String remarks) {

    /**
     * Maps an item.
     *
     * @param p item
     * @param today today
     * @return response
     */
    public static PrebookedResponse from(Prebooked p, LocalDate today) {
      return new PrebookedResponse(
          p.getId(),
          p.getArn(),
          p.getReference(),
          p.getAmount(),
          p.getCurrency(),
          p.getFirstSeen(),
          ChronoUnit.DAYS.between(p.getFirstSeen(), today),
          p.getRematchCount(),
          p.getLastRematch(),
          p.getStatus(),
          p.getReceiptId(),
          p.getPaymentId(),
          p.getRemarks());
    }
  }

  /**
   * A post-dated check.
   *
   * @param id id
   * @param warehouseNo PDCW- number
   * @param clientCode client
   * @param payorName payor
   * @param reference reference
   * @param checkNo check number
   * @param bankCode bank
   * @param checkBranch bank branch
   * @param maturityDate maturity
   * @param amount amount
   * @param currency currency
   * @param segment market segment
   * @param status status
   * @param statusReason reason
   * @param receiptNo AR at maturity
   */
  public record PdcResponse(
      Long id,
      String warehouseNo,
      String clientCode,
      String payorName,
      String reference,
      String checkNo,
      String bankCode,
      String checkBranch,
      LocalDate maturityDate,
      BigDecimal amount,
      String currency,
      String segment,
      String status,
      String statusReason,
      String receiptNo) {

    /**
     * Maps a check.
     *
     * @param p check
     * @return response
     */
    public static PdcResponse from(PdcItem p) {
      return new PdcResponse(
          p.getId(),
          p.getWarehouseNo(),
          p.getClientCode(),
          p.getPayorName(),
          p.getReference(),
          p.getCheckNo(),
          p.getBankCode(),
          p.getCheckBranch(),
          p.getMaturityDate(),
          p.getAmount(),
          p.getCurrency(),
          p.getSegment(),
          p.getStatus().name(),
          p.getStatusReason(),
          p.getReceiptNo());
    }
  }

  /**
   * A check to warehouse.
   *
   * @param companyId company
   * @param branchId branch
   * @param clientCode client
   * @param payorName payor
   * @param reference invoice, ARN, policy or PN
   * @param checkNo check number
   * @param bankCode bank
   * @param checkBranch bank branch
   * @param maturityDate maturity
   * @param amount amount
   * @param segment market segment
   */
  public record PdcRequest(
      @NotNull Long companyId,
      @NotNull Long branchId,
      @Size(max = 30) String clientCode,
      @NotBlank @Size(max = 250) String payorName,
      @NotBlank @Size(max = 80) String reference,
      @NotBlank @Size(max = 40) String checkNo,
      @NotBlank @Size(max = 30) String bankCode,
      @Size(max = 60) String checkBranch,
      @NotNull LocalDate maturityDate,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @Size(max = 40) String segment) {}

  /**
   * A check leaving the warehouse.
   *
   * @param outcome RETURNED, REPLACED or PULLED_OUT
   * @param reason reason
   */
  public record PdcReleaseRequest(
      @NotNull PdcStatus outcome, @NotBlank @Size(max = 250) String reason) {}

  /**
   * A check pick-up request.
   *
   * @param id id
   * @param collectionRef Collection reference
   * @param reference reference
   * @param clientCode client
   * @param payorName payor
   * @param pickupDate pick-up date
   * @param requestedAt requested
   * @param requestor Collection handler
   * @param amount amount
   * @param currency currency
   * @param checkNo check number
   * @param status status
   * @param receiptNo AR
   * @param printedAt printed
   */
  public record PickupResponse(
      Long id,
      String collectionRef,
      String reference,
      String clientCode,
      String payorName,
      LocalDate pickupDate,
      Instant requestedAt,
      String requestor,
      BigDecimal amount,
      String currency,
      String checkNo,
      String status,
      String receiptNo,
      Instant printedAt) {

    /**
     * Maps a request.
     *
     * @param p request
     * @return response
     */
    public static PickupResponse from(PickupRequest p) {
      return new PickupResponse(
          p.getId(),
          p.getCollectionRef(),
          p.getReference(),
          p.getClientCode(),
          p.getPayorName(),
          p.getPickupDate(),
          p.getRequestedAt(),
          p.getRequestor(),
          p.getAmount(),
          p.getCurrency(),
          p.getCheckNo(),
          p.getStatus().name(),
          p.getReceiptNo(),
          p.getPrintedAt());
    }
  }

  /**
   * A pick-up request to queue.
   *
   * @param companyId company
   * @param branchId branch that prints the AR
   * @param collectionRef Collection reference
   * @param reference invoice, ARN, policy or PN
   * @param clientCode client
   * @param payorName payor
   * @param pickupDate pick-up date
   * @param requestor Collection handler
   * @param amount amount
   * @param checkNo check number
   * @param checkBank bank
   */
  public record PickupRequestBody(
      @NotNull Long companyId,
      @NotNull Long branchId,
      @NotBlank @Size(max = 40) String collectionRef,
      @NotBlank @Size(max = 80) String reference,
      @Size(max = 30) String clientCode,
      @NotBlank @Size(max = 250) String payorName,
      @NotNull LocalDate pickupDate,
      @NotBlank @Size(max = 100) String requestor,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @Size(max = 40) String checkNo,
      @Size(max = 60) String checkBank) {}

  /**
   * A selection of records.
   *
   * @param companyId company
   * @param ids ids
   * @param criteria how they were selected
   */
  public record Selection(@NotNull Long companyId, @NotEmpty List<Long> ids, String criteria) {}

  /**
   * A reason.
   *
   * @param reason reason
   */
  public record ReasonBody(@NotBlank @Size(max = 250) String reason) {}
}
