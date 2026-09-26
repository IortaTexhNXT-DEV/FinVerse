package com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimViewService.ClaimView;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.PremiumDto;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPremiumStatus;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimProgress;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The claim record (design 11): identity, cover snapshot, premium check and authorization, loss
 * and claimant, progress (read-only here, owned by the status engine), flags and totals.
 *
 * @param id claim id
 * @param claimNo claim number
 * @param source source
 * @param handler handler
 * @param unitCode handling unit
 * @param unitLabel unit label
 * @param branchId handling branch
 * @param cover cover snapshot
 * @param premium premium check and authorization code
 * @param loss loss and claimant
 * @param progress status, phase, settlement and follow-up
 * @param flags flags of the summary card
 * @param locationCount linked locations
 * @param insurerCount distinct insurers
 * @param totalReserve sum of the insurer reserves
 * @param createdBy recorded by
 * @param createdAt recorded at
 */
public record ClaimResponse(
    Long id,
    String claimNo,
    String source,
    String handler,
    String unitCode,
    String unitLabel,
    Long branchId,
    Cover cover,
    Premium premium,
    Loss loss,
    Progress progress,
    Flags flags,
    int locationCount,
    int insurerCount,
    BigDecimal totalReserve,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a claim view.
   *
   * @param v view
   * @return response
   */
  public static ClaimResponse from(ClaimView v) {
    Claim c = v.claim();
    Map<String, String> labels = v.labels();
    return new ClaimResponse(
        c.getId(),
        c.getClaimNo(),
        c.getSource().name(),
        c.getHandler(),
        c.getUnitCode(),
        labels.get(ClaimCodes.LOV_UNIT),
        c.getBranchId(),
        Cover.from(c.getCover(), v.latestVersionNo()),
        Premium.from(c.getCover(), v),
        Loss.from(c.getLoss(), labels),
        Progress.from(c.getProgress(), labels),
        new Flags(
            v.premium().blocking(),
            v.awaitingRemittance(),
            isNewer(c.getCover(), v.latestVersionNo()),
            v.locationCount() > 1,
            v.insurerCount() > 1,
            c.getLoss().isCatastrophe(),
            c.getLoss().isClaimantOverridden()),
        v.locationCount(),
        v.insurerCount(),
        v.totalReserve(),
        c.getCreatedBy(),
        c.getCreatedAt());
  }

  private static boolean isNewer(CoverSnapshot cover, int latest) {
    return cover.getCoverVersionNo() != null && latest > cover.getCoverVersionNo();
  }

  /**
   * Cover snapshot of the claim.
   *
   * @param arn account reference number
   * @param accountId account id
   * @param policyYear policy year
   * @param policyNo policy number, null while pending
   * @param versionNo cover version used
   * @param versionLabel version label
   * @param versionAt effective date of the version
   * @param latestVersionNo latest version of the policy year
   * @param productCode product
   * @param lineCode line
   * @param clientCode client
   * @param assuredName assured
   * @param leadInsurerCode lead insurer
   * @param periodFrom start
   * @param periodTo end
   * @param sumInsured sum insured
   * @param salesTeam Marketing team
   * @param salesDepartment department
   * @param accountOfficer account officer
   * @param invoicingBranchId invoicing branch
   * @param currency currency
   */
  public record Cover(
      String arn,
      Long accountId,
      int policyYear,
      String policyNo,
      Integer versionNo,
      String versionLabel,
      LocalDate versionAt,
      int latestVersionNo,
      String productCode,
      String lineCode,
      String clientCode,
      String assuredName,
      String leadInsurerCode,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal sumInsured,
      String salesTeam,
      String salesDepartment,
      String accountOfficer,
      Long invoicingBranchId,
      String currency) {

    static Cover from(CoverSnapshot s, int latest) {
      return new Cover(
          s.getArn(),
          s.getAccountId(),
          s.getPolicyYear(),
          s.getPolicyNo(),
          s.getCoverVersionNo(),
          s.versionLabel(),
          s.getCoverVersionAt(),
          latest,
          s.getProductCode(),
          s.getLineCode(),
          s.getClientCode(),
          s.getAssuredName(),
          s.getLeadInsurerCode(),
          s.getPeriodFrom(),
          s.getPeriodTo(),
          s.getSumInsured(),
          s.getSalesTeam(),
          s.getSalesDepartment(),
          s.getAccountOfficer(),
          s.getInvoicingBranchId(),
          s.getCurrency());
    }
  }

  /**
   * Premium check and claims authorization code (BRCLM.001).
   *
   * @param recordedStatus status recorded at the last check
   * @param checkedAt time of the last recorded check
   * @param live check now, with the invoices not fully paid
   * @param authorizationCode code, null until issued
   * @param authorizedBy user
   * @param authorizedAt time
   * @param evidenceAttachmentId insurer payment evidence (direct payment)
   * @param dpPolicy parameter BCL_AUTH_DP_POLICY
   * @param canAuthorize the code can be generated now (PAID, or direct payment not blocked)
   * @param unremittedInvoices invoices not fully remitted (special remittance, OQ46)
   */
  public record Premium(
      ClaimPremiumStatus recordedStatus,
      Instant checkedAt,
      PremiumDto live,
      String authorizationCode,
      String authorizedBy,
      Instant authorizedAt,
      Long evidenceAttachmentId,
      String dpPolicy,
      boolean canAuthorize,
      List<String> unremittedInvoices) {

    static Premium from(CoverSnapshot s, ClaimView v) {
      ClaimPremiumStatus now = v.premium().status();
      boolean directAllowed =
          now == ClaimPremiumStatus.DIRECT_PAYMENT && !"BLOCK".equalsIgnoreCase(v.dpPolicy());
      boolean allowed = now == ClaimPremiumStatus.PAID || directAllowed;
      return new Premium(
          s.getPremiumStatus(),
          s.getPremiumCheckedAt(),
          PremiumDto.from(v.premium()),
          s.getAuthorizationCode(),
          s.getAuthorizedBy(),
          s.getAuthorizedAt(),
          s.getDpEvidenceAttachmentId(),
          v.dpPolicy(),
          allowed && !s.isAuthorized(),
          v.unremittedInvoices());
    }
  }

  /**
   * Loss data and claimant.
   *
   * @param lossDate loss date
   * @param reportedDate reported date
   * @param lossNature nature
   * @param lossNatureLabel nature label
   * @param claimType type
   * @param claimTypeLabel type label
   * @param lossDescription description
   * @param lossPlace place
   * @param catastropheCode catastrophe
   * @param catastropheLabel catastrophe label
   * @param catastropheEvent event name
   * @param claimAmount claim amount
   * @param deductible deductible
   * @param initialReserve initial loss reserve
   * @param claimantName claimant
   * @param claimantOverridden overridden
   * @param claimantReason override reason
   */
  public record Loss(
      LocalDate lossDate,
      LocalDate reportedDate,
      String lossNature,
      String lossNatureLabel,
      String claimType,
      String claimTypeLabel,
      String lossDescription,
      String lossPlace,
      String catastropheCode,
      String catastropheLabel,
      String catastropheEvent,
      BigDecimal claimAmount,
      BigDecimal deductible,
      BigDecimal initialReserve,
      String claimantName,
      boolean claimantOverridden,
      String claimantReason) {

    static Loss from(LossDetails d, Map<String, String> labels) {
      return new Loss(
          d.getLossDate(),
          d.getReportedDate(),
          d.getLossNature(),
          labels.get(ClaimCodes.LOV_LOSS_NATURE),
          d.getClaimType(),
          labels.get(ClaimCodes.LOV_CLAIM_TYPE),
          d.getLossDescription(),
          d.getLossPlace(),
          d.getCatastropheCode(),
          labels.get(ClaimCodes.LOV_CATASTROPHE),
          d.getCatastropheEvent(),
          d.getClaimAmount(),
          d.getDeductible(),
          d.getInitialReserve(),
          d.getClaimantName(),
          d.isClaimantOverridden(),
          d.getClaimantReason());
    }
  }

  /**
   * Where the claim stands (wave CL1-B maintains it).
   *
   * @param statusCode status
   * @param statusLabel status label
   * @param statusSince status set at
   * @param phase phase
   * @param closureKind closure
   * @param settlementTypeCode settlement type
   * @param settlementTypeLabel settlement type label
   * @param settlementAmount settlement amount
   * @param dateSettled date settled
   * @param closedOn closed on
   * @param adjusterCode adjuster of the claim
   * @param adjusterLabel adjuster label
   * @param nextFollowUpDate next follow-up
   * @param followUpOverridden follow-up overridden
   * @param nextActionPlan action plan
   */
  public record Progress(
      String statusCode,
      String statusLabel,
      Instant statusSince,
      String phase,
      String closureKind,
      String settlementTypeCode,
      String settlementTypeLabel,
      BigDecimal settlementAmount,
      LocalDate dateSettled,
      LocalDate closedOn,
      String adjusterCode,
      String adjusterLabel,
      LocalDate nextFollowUpDate,
      boolean followUpOverridden,
      String nextActionPlan) {

    static Progress from(ClaimProgress p, Map<String, String> labels) {
      return new Progress(
          p.getStatusCode(),
          labels.get(ClaimCodes.LOV_STATUS),
          p.getStatusSince(),
          p.getPhase().name(),
          p.getClosureKind() == null ? null : p.getClosureKind().name(),
          p.getSettlementTypeCode(),
          labels.get(ClaimCodes.LOV_SETTLEMENT_TYPE),
          p.getSettlementAmount(),
          p.getDateSettled(),
          p.getClosedOn(),
          p.getAdjusterCode(),
          labels.get(ClaimCodes.LOV_ADJUSTER),
          p.getNextFollowUpDate(),
          p.isFollowUpOverridden(),
          p.getNextActionPlan());
    }
  }

  /**
   * Flags of the summary card, apart from the status pill.
   *
   * @param unpaidPremium premium unpaid or partly paid
   * @param awaitingPremiumRemittance status waits for the premium remittance
   * @param newerCoverVersion a later endorsement exists
   * @param multiLocation two or more locations
   * @param multiInsurer two or more insurers
   * @param catastrophe catastrophe code set
   * @param claimantOverridden claimant overridden
   */
  public record Flags(
      boolean unpaidPremium,
      boolean awaitingPremiumRemittance,
      boolean newerCoverVersion,
      boolean multiLocation,
      boolean multiInsurer,
      boolean catastrophe,
      boolean claimantOverridden) {}
}
