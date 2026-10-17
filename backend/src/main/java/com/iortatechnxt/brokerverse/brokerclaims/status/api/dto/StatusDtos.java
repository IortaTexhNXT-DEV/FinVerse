package com.iortatechnxt.brokerverse.brokerclaims.status.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimClosureKind;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimProgress;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimClosureService;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Requests and responses of the claim status actions (FR-CM-042/044/045/050/051). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class StatusDtos {

  private StatusDtos() {}

  /**
   * Change Status.
   *
   * @param statusCode new status ({@code BCL_CLAIM_STATUS})
   * @param remark remark, up to 500 characters
   */
  public record StatusRequest(String statusCode, String remark) {}

  /**
   * Set Settlement.
   *
   * @param typeCode requested type of settlement
   * @param amount settlement amount
   * @param dateSettled date settled
   * @param remark remark
   */
  public record SettlementRequest(
      String typeCode, BigDecimal amount, LocalDate dateSettled, String remark) {

    /**
     * The service command.
     *
     * @return settlement
     */
    public ClaimClosureService.Settlement toSettlement() {
      return new ClaimClosureService.Settlement(typeCode, amount, dateSettled, remark);
    }
  }

  /**
   * Reopen Claim.
   *
   * @param reasonCode reason ({@code BCL_REOPEN_REASON})
   * @param remark remark
   */
  public record ReopenRequest(String reasonCode, String remark) {}

  /**
   * Override Follow-up Date.
   *
   * @param date next follow-up date
   * @param reasonCode reason ({@code BCL_OVERRIDE_REASON})
   */
  public record FollowUpRequest(LocalDate date, String reasonCode) {}

  /**
   * Next action plan summary.
   *
   * @param text summary, up to 2,000 characters
   */
  public record ActionPlanRequest(String text) {}

  /**
   * Adjuster / appraiser of the claim.
   *
   * @param adjusterCode adjuster ({@code BCL_ADJUSTER}), blank clears it
   * @param remark remark
   */
  public record AdjusterRequest(String adjusterCode, String remark) {}

  /**
   * The progress of a claim after an action.
   *
   * @param claimId claim
   * @param claimNo claim number
   * @param statusCode status
   * @param phase phase
   * @param closureKind closure kind
   * @param settlementTypeCode settlement type
   * @param nextFollowUpDate next follow-up date
   * @param followUpOverridden whether the date was overridden
   */
  public record ClaimProgressResponse(
      Long claimId,
      String claimNo,
      String statusCode,
      ClaimPhase phase,
      ClaimClosureKind closureKind,
      String settlementTypeCode,
      LocalDate nextFollowUpDate,
      boolean followUpOverridden) {

    /**
     * Maps a claim.
     *
     * @param claim claim
     * @return response
     */
    public static ClaimProgressResponse from(Claim claim) {
      ClaimProgress p = claim.getProgress();
      return new ClaimProgressResponse(
          claim.getId(),
          claim.getClaimNo(),
          p.getStatusCode(),
          p.getPhase(),
          p.getClosureKind(),
          p.getSettlementTypeCode(),
          p.getNextFollowUpDate(),
          p.isFollowUpOverridden());
    }
  }
}
