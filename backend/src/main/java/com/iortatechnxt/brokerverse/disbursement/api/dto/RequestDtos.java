package com.iortatechnxt.brokerverse.disbursement.api.dto;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementQueryService.Summary;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Requests and responses of the payment request endpoints (DIS 2.4-2.6, 3.25). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RequestDtos {

  private RequestDtos() {}

  /**
   * A payment request.
   *
   * @param id id
   * @param requestNo request number
   * @param source how it arrived
   * @param sourceModule source module
   * @param sourceRef source reference
   * @param rfpNo RFP number
   * @param disbursementType disbursement type
   * @param payeeClass payee class
   * @param payeeCode payee code
   * @param payeeName payee name
   * @param payeeId matched payee
   * @param currency currency
   * @param amount amount
   * @param purpose purpose
   * @param receivedAt received
   * @param rootInvoiceNo root invoice
   * @param attachmentRefs supporting documents
   * @param status status
   * @param statusReason reason of the status
   * @param voucherId voucher
   * @param uploadJobNo upload
   * @param createdBy requestor
   */
  public record RequestResponse(
      Long id,
      String requestNo,
      RequestSource source,
      String sourceModule,
      String sourceRef,
      String rfpNo,
      String disbursementType,
      String payeeClass,
      String payeeCode,
      String payeeName,
      Long payeeId,
      String currency,
      BigDecimal amount,
      String purpose,
      Instant receivedAt,
      String rootInvoiceNo,
      List<String> attachmentRefs,
      RequestStatus status,
      String statusReason,
      Long voucherId,
      String uploadJobNo,
      String createdBy) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return DTO
     */
    public static RequestResponse from(IntakeRequest r) {
      return new RequestResponse(
          r.getId(),
          r.getRequestNo(),
          r.getSource(),
          r.getSourceModule(),
          r.getSourceRef(),
          r.getRfpNo(),
          r.getDisbursementType(),
          r.getPayeeClass(),
          r.getPayeeCode(),
          r.getPayeeName(),
          r.getPayeeId(),
          r.getCurrency(),
          r.getAmount(),
          r.getPurpose(),
          r.getReceivedAt(),
          r.getRootInvoiceNo(),
          r.getAttachmentRefs(),
          r.getStatus(),
          r.getStatusReason(),
          r.getVoucherId(),
          r.getUploadJobNo(),
          r.getCreatedBy());
    }
  }

  /**
   * A request encoded from an e-mail (DIS 2.6.1).
   *
   * @param companyId company
   * @param disbursementType type (LOV {@code DISBURSEMENT_TYPE})
   * @param payeeCode payee party code
   * @param payeeName payee name
   * @param currency currency
   * @param amount gross amount
   * @param purpose purpose
   * @param rfpNo RFP number of the requesting unit
   * @param rootInvoiceNo root invoice
   * @param expenseAccount expense account of an OTHER payment
   * @param costCenter cost centre
   */
  public record EncodeRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 30) String disbursementType,
      @NotBlank @Size(max = 30) String payeeCode,
      @Size(max = 250) String payeeName,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @NotBlank @Size(max = 500) String purpose,
      @Size(max = 40) String rfpNo,
      @Size(max = 40) String rootInvoiceNo,
      @Size(max = 30) String expenseAccount,
      @Size(max = 20) String costCenter) {}

  /**
   * A reason with a comment.
   *
   * @param reasonCode reason code
   * @param comment comment
   */
  public record ReasonComment(
      @NotBlank @Size(max = 40) String reasonCode, @Size(max = 500) String comment) {}

  /**
   * Counts of the workbench.
   *
   * @param requests requests without voucher
   * @param noPayee requests waiting for their payee
   * @param inProcess vouchers in process
   * @param forReview vouchers for review
   * @param forApproval vouchers for approval
   * @param approved approved vouchers
   * @param closed cancelled and rejected vouchers
   * @param unregularized vouchers whose posting failed
   * @param payeesPending payees waiting for authorisation
   * @param payeeRequests open payee requests
   * @param fundingPending funding requests waiting
   */
  public record SummaryResponse(
      long requests,
      long noPayee,
      long inProcess,
      long forReview,
      long forApproval,
      long approved,
      long closed,
      long unregularized,
      long payeesPending,
      long payeeRequests,
      long fundingPending) {

    /**
     * Maps the counts.
     *
     * @param s counts
     * @return DTO
     */
    public static SummaryResponse from(Summary s) {
      return new SummaryResponse(
          s.requests(),
          s.noPayee(),
          s.inProcess(),
          s.forReview(),
          s.forApproval(),
          s.approved(),
          s.closed(),
          s.unregularized(),
          s.payeesPending(),
          s.payeeRequests(),
          s.fundingPending());
    }
  }
}
