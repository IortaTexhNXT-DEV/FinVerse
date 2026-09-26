package com.iortatechnxt.brokerverse.remittance.api.dto;

import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** Requests and responses of the hold and special remittance endpoints (MKTID.002-009). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RequestDtos {

  private RequestDtos() {}

  /**
   * A hold request.
   *
   * @param id id
   * @param requestNo number
   * @param invoiceNo invoice
   * @param arn ARN
   * @param insurerCode insurer
   * @param clientCode client
   * @param assuredName assured
   * @param reasonCode reason
   * @param remarks remarks
   * @param holdUntil hold until
   * @param requestedUntil extension requested
   * @param extensionCount extensions granted
   * @param stage stage
   * @param source screen or feed
   * @param requestedBy requestor
   * @param approvedBy approver
   * @param assignedProcessor processor
   * @param releasedAt release time
   * @param releaseNote release note
   * @param createdAt created
   */
  public record HoldResponse(
      Long id,
      String requestNo,
      String invoiceNo,
      String arn,
      String insurerCode,
      String clientCode,
      String assuredName,
      String reasonCode,
      String remarks,
      LocalDate holdUntil,
      LocalDate requestedUntil,
      int extensionCount,
      HoldStage stage,
      RequestSource source,
      String requestedBy,
      String approvedBy,
      String assignedProcessor,
      Instant releasedAt,
      String releaseNote,
      Instant createdAt) {

    /**
     * Maps a request.
     *
     * @param h request
     * @return DTO
     */
    public static HoldResponse from(HoldRequest h) {
      return new HoldResponse(
          h.getId(),
          h.getRequestNo(),
          h.getInvoiceNo(),
          h.getArn(),
          h.getInsurerCode(),
          h.getClientCode(),
          h.getAssuredName(),
          h.getReasonCode(),
          h.getRemarks(),
          h.getHoldUntil(),
          h.getRequestedUntil(),
          h.getExtensionCount(),
          h.getStage(),
          h.getSource(),
          h.getRequestedBy(),
          h.getApprovedBy(),
          h.getAssignedProcessor(),
          h.getReleasedAt(),
          h.getReleaseNote(),
          h.getCreatedAt());
    }
  }

  /**
   * A new hold request (MKTID.003).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param reasonCode reason (LOV HOLD_REASON)
   * @param remarks remarks
   * @param holdUntil hold until
   * @param submit submit for approval now
   */
  public record HoldCreateRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String invoiceNo,
      @NotBlank String reasonCode,
      @Size(max = 500) String remarks,
      @NotNull LocalDate holdUntil,
      boolean submit) {

    /**
     * The terms.
     *
     * @return terms
     */
    public HoldRequest.Terms terms() {
      return new HoldRequest.Terms(reasonCode, remarks, holdUntil);
    }
  }

  /**
   * Terms of a draft.
   *
   * @param reasonCode reason
   * @param remarks remarks
   * @param holdUntil hold until
   */
  public record HoldTermsRequest(
      @NotBlank String reasonCode, @Size(max = 500) String remarks, @NotNull LocalDate holdUntil) {

    /**
     * The terms.
     *
     * @return terms
     */
    public HoldRequest.Terms terms() {
      return new HoldRequest.Terms(reasonCode, remarks, holdUntil);
    }
  }

  /**
   * An extension (MKTID.005).
   *
   * @param holdUntil new hold-until date
   * @param comment comment
   */
  public record ExtendRequest(@NotNull LocalDate holdUntil, @Size(max = 1000) String comment) {}

  /**
   * An approval decision.
   *
   * @param approve approve or reject
   * @param comment comment
   */
  public record DecisionRequest(boolean approve, @Size(max = 1000) String comment) {}

  /**
   * A special remittance request.
   *
   * @param id id
   * @param requestNo number
   * @param invoiceNo invoice
   * @param arn ARN
   * @param insurerCode insurer
   * @param clientCode client
   * @param assuredName assured
   * @param policyNo policy
   * @param segment segment
   * @param conditionCode condition
   * @param remarks remarks
   * @param stage stage
   * @param source screen or feed
   * @param requestedBy requestor
   * @param validationNote outcome of the checks
   * @param approvedBy approver
   * @param rejectedReason rejection
   * @param batchNo special batch
   * @param pushedAt push to Disbursement
   * @param createdAt created
   */
  public record SpecialResponse(
      Long id,
      String requestNo,
      String invoiceNo,
      String arn,
      String insurerCode,
      String clientCode,
      String assuredName,
      String policyNo,
      String segment,
      String conditionCode,
      String remarks,
      SpecialStage stage,
      RequestSource source,
      String requestedBy,
      String validationNote,
      String approvedBy,
      String rejectedReason,
      String batchNo,
      Instant pushedAt,
      Instant createdAt) {

    /**
     * Maps a request.
     *
     * @param s request
     * @return DTO
     */
    public static SpecialResponse from(SpecialRemittance s) {
      return new SpecialResponse(
          s.getId(),
          s.getRequestNo(),
          s.getInvoiceNo(),
          s.getArn(),
          s.getInsurerCode(),
          s.getClientCode(),
          s.getAssuredName(),
          s.getPolicyNo(),
          s.getSegment(),
          s.getConditionCode(),
          s.getRemarks(),
          s.getStage(),
          s.getSource(),
          s.getRequestedBy(),
          s.getValidationNote(),
          s.getApprovedBy(),
          s.getRejectedReason(),
          s.getBatchNo(),
          s.getPushedAt(),
          s.getCreatedAt());
    }
  }

  /**
   * A new special remittance request (MKTID.009).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param conditionCode condition (LOV SPECIAL_REMIT_CONDITION)
   * @param remarks remarks
   */
  public record SpecialCreateRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 40) String invoiceNo,
      @NotBlank String conditionCode,
      @Size(max = 500) String remarks) {}
}
