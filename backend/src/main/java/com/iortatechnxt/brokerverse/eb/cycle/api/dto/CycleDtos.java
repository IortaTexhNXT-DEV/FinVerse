package com.iortatechnxt.brokerverse.eb.cycle.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.eb.domain.EbBor;
import com.iortatechnxt.brokerverse.eb.domain.EbFeedback;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Request and response bodies of the cycle API (FR-EB-021, 023, 031, 046). */
public final class CycleDtos {

  private CycleDtos() {}

  /**
   * A cycle to open.
   *
   * @param businessType NEW_BUSINESS or RENEWAL (required)
   * @param policyYear policy year, the target inception's year when empty
   * @param targetInception target inception
   */
  public record OpenCycleRequest(
      BusinessType businessType, Integer policyYear, LocalDate targetInception) {}

  /**
   * The validator's BOR checklist.
   *
   * @param signedBySignatory signed by an authorised signatory
   * @param notBlank not blank
   * @param clientNameMatches client name matches
   * @param validFrom valid from
   * @param validTo valid to
   */
  public record BorChecklistRequest(
      boolean signedBySignatory,
      boolean notBlank,
      boolean clientNameMatches,
      LocalDate validFrom,
      LocalDate validTo) {

    /**
     * The domain checklist.
     *
     * @return checklist
     */
    public EbBor.Checklist toChecklist() {
      return new EbBor.Checklist(
          signedBySignatory, notBlank, clientNameMatches, validFrom, validTo);
    }
  }

  /**
   * A rejection reason.
   *
   * @param reason why
   */
  public record RejectRequest(String reason) {}

  /**
   * A BOR version.
   *
   * @param id version
   * @param cycleId cycle
   * @param versionNo version
   * @param attachmentId stored file
   * @param status UPLOADED, VALIDATED, REJECTED or SUPERSEDED
   * @param signedBySignatory checklist
   * @param notBlank checklist
   * @param clientNameMatches checklist
   * @param validFrom valid from
   * @param validTo valid to
   * @param uploadedBy uploader
   * @param uploadedAt uploaded
   * @param decidedBy validator
   * @param decidedAt decided
   * @param rejectReason reason of a rejection
   */
  public record BorResponse(
      Long id,
      Long cycleId,
      int versionNo,
      Long attachmentId,
      String status,
      Boolean signedBySignatory,
      Boolean notBlank,
      Boolean clientNameMatches,
      LocalDate validFrom,
      LocalDate validTo,
      String uploadedBy,
      Instant uploadedAt,
      String decidedBy,
      Instant decidedAt,
      String rejectReason) {

    /**
     * Maps a version.
     *
     * @param b version
     * @return response
     */
    public static BorResponse from(EbBor b) {
      return new BorResponse(
          b.getId(),
          b.getCycleId(),
          b.getVersionNo(),
          b.getAttachmentId(),
          b.getStatus().name(),
          b.getSignedBySignatory(),
          b.getNotBlank(),
          b.getClientNameMatches(),
          b.getValidFrom(),
          b.getValidTo(),
          b.getCreatedBy(),
          b.getCreatedAt(),
          b.getDecidedBy(),
          b.getDecidedAt(),
          b.getRejectReason());
    }
  }

  /**
   * Client feedback.
   *
   * @param id feedback
   * @param cycleId cycle
   * @param channel channel
   * @param receivedOn date received
   * @param text text
   * @param fileCount files attached
   * @param recordedBy user
   * @param recordedAt time
   */
  public record FeedbackResponse(
      Long id,
      Long cycleId,
      String channel,
      LocalDate receivedOn,
      String text,
      int fileCount,
      String recordedBy,
      Instant recordedAt) {

    /**
     * Maps feedback.
     *
     * @param f feedback
     * @return response
     */
    public static FeedbackResponse from(EbFeedback f) {
      return new FeedbackResponse(
          f.getId(),
          f.getCycleId(),
          f.getChannel().name(),
          f.getReceivedOn(),
          f.getText(),
          f.getFileCount(),
          f.getCreatedBy(),
          f.getCreatedAt());
    }
  }

  /**
   * An account created for a cycle (BT0 business type and renewal link).
   *
   * @param id account
   * @param arn ARN
   * @param cycleId cycle
   * @param cycleNo cycle number
   * @param productCode product
   * @param insurerCode insurer
   * @param periodFrom period start
   * @param periodTo period end
   * @param businessType NEW_BUSINESS or RENEWAL
   * @param renewalOfRef what it renews
   * @param status account status
   * @param grossPremium gross premium
   * @param currency currency
   */
  public record AccountResponse(
      Long id,
      String arn,
      Long cycleId,
      String cycleNo,
      String productCode,
      String insurerCode,
      LocalDate periodFrom,
      LocalDate periodTo,
      String businessType,
      String renewalOfRef,
      String status,
      BigDecimal grossPremium,
      String currency) {

    /**
     * Maps an account.
     *
     * @param a account
     * @param cycleId cycle
     * @param cycleNo cycle number
     * @return response
     */
    public static AccountResponse from(Account a, Long cycleId, String cycleNo) {
      return new AccountResponse(
          a.getId(),
          a.getArn(),
          cycleId,
          cycleNo,
          a.getProductCode(),
          a.getInsurerCode(),
          a.getPeriodFrom(),
          a.getPeriodTo(),
          a.getBusinessType().name(),
          a.getClassification().renewalOfRef(),
          a.getStatus().name(),
          a.getPremium() == null ? null : a.getPremium().grossPremium(),
          a.getCurrency());
    }
  }
}
