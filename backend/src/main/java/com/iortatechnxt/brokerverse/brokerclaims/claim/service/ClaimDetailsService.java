package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.brokerclaims.domain.LossDetails;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes to the cover snapshot and loss details of an open claim (BRCLM.004/006/016/036/039;
 * FR-CM-012/014/015/030/033): loss data (BCL_RECORD), reported date correction with a reason
 * (BCL_STATUS_UPDATE), claimant override with a reason (BCL_CLAIMANT_OVERRIDE), refresh of the
 * cover data from the account and switch to the latest cover version. Every change is audited with
 * the old and new values; policy data itself is never edited by Claims.
 */
@Service
@Transactional
public class ClaimDetailsService {

  private final BrokerClaimQueryService claims;
  private final CoverService covers;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param claims claims
   * @param covers covers
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  public ClaimDetailsService(
      BrokerClaimQueryService claims,
      CoverService covers,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.claims = claims;
    this.covers = covers;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Changes the loss data (BCL_RECORD): nature, type, description, place, catastrophe tag and
   * amounts, and the loss date within its rules.
   *
   * @param companyId company
   * @param claimId claim
   * @param loss loss data (its reported date is ignored)
   * @param amounts amounts
   * @return the claim
   */
  public Claim amendLoss(
      Long companyId, Long claimId, LossDetails.Loss loss, LossDetails.Amounts amounts) {
    Claim claim = claims.requireOpen(companyId, claimId);
    LocalDate today = LocalDate.now(clock);
    lovs.requireValid(ClaimCodes.LOV_LOSS_NATURE, loss.lossNature(), today);
    lovs.requireValid(ClaimCodes.LOV_CLAIM_TYPE, loss.claimType(), today);
    lovs.validateOptional(ClaimCodes.LOV_CATASTROPHE, blankToNull(loss.catastropheCode()), today);
    LossDetails details = claim.getLoss();
    String before = summary(details);
    details.amend(loss, amounts, today);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Loss details: " + before + " -> " + summary(details));
    return claim;
  }

  /**
   * Corrects the reported date of an open claim (BRCLM.004, FR-CM-012; BCL_STATUS_UPDATE).
   *
   * @param companyId company
   * @param claimId claim
   * @param date new reported date
   * @param reason reason ({@code BCL_OVERRIDE_REASON})
   * @param remark remark, may be null
   * @return the claim
   */
  public Claim correctReportedDate(
      Long companyId, Long claimId, LocalDate date, String reason, String remark) {
    Claim claim = claims.require(companyId, claimId);
    if (claim.isClosed()) {
      throw new BusinessRuleException(
          "BCL_CLAIM_CLOSED", "The reported date of a closed claim cannot change");
    }
    LocalDate today = LocalDate.now(clock);
    if (reason != null && !reason.isBlank()) {
      lovs.requireValid(ClaimCodes.LOV_OVERRIDE_REASON, reason, today);
    }
    LocalDate previous = claim.getLoss().correctReportedDate(date, reason, today);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Reported date " + previous + " -> " + date + " (" + reason + withRemark(remark) + ")");
    return claim;
  }

  /**
   * Overrides the claimant's name (BRCLM.006, FR-CM-030; BCL_CLAIMANT_OVERRIDE).
   *
   * @param companyId company
   * @param claimId claim
   * @param name claimant
   * @param reason reason ({@code BCL_OVERRIDE_REASON})
   * @return the claim
   */
  public Claim overrideClaimant(Long companyId, Long claimId, String name, String reason) {
    Claim claim = claims.requireOpen(companyId, claimId);
    if (reason != null && !reason.isBlank()) {
      lovs.requireValid(ClaimCodes.LOV_OVERRIDE_REASON, reason, LocalDate.now(clock));
    }
    String previous = claim.getLoss().overrideClaimant(name, reason);
    audit.record(
        ClaimCodes.ENTITY_TYPE,
        claim.getClaimNo(),
        AuditAction.UPDATE,
        "Claimant " + previous + " -> " + claim.getLoss().getClaimantName() + " (" + reason + ")");
    return claim;
  }

  /**
   * Reloads the policy number, sum insured, Marketing unit, AO and branch from the account
   * (BRCLM.016 "Refresh Cover Data"; FR-CM-013 policy number filled once issued).
   *
   * @param companyId company
   * @param claimId claim
   * @return the changes made, empty when the snapshot was current
   */
  public List<String> refreshCover(Long companyId, Long claimId) {
    Claim claim = claims.requireOpen(companyId, claimId);
    CoverSnapshot cover = claim.getCover();
    Account account = covers.account(companyId, cover.getArn());
    List<String> changes =
        cover.refresh(
            covers.policy(account, cover.getPolicyYear()),
            covers.sales(account, cover.getPolicyYear()));
    if (!changes.isEmpty()) {
      audit.record(
          ClaimCodes.ENTITY_TYPE,
          claim.getClaimNo(),
          AuditAction.UPDATE,
          "Cover data refreshed: " + String.join("; ", changes));
    }
    return changes;
  }

  /**
   * Switches the claim to the latest cover version of its policy year (BRCLM.039 "Use Latest
   * Version").
   *
   * @param companyId company
   * @param claimId claim
   * @return the claim
   */
  public Claim useLatestVersion(Long companyId, Long claimId) {
    Claim claim = claims.requireOpen(companyId, claimId);
    CoverSnapshot cover = claim.getCover();
    CoverSnapshot.Version latest = covers.version(cover.getArn(), cover.getPolicyYear(), null);
    if (cover.getCoverVersionNo() != null && latest.no() <= cover.getCoverVersionNo()) {
      throw new BusinessRuleException(
          "BCL_VERSION_CURRENT", "The claim already uses the latest cover version");
    }
    String change = cover.useVersion(latest);
    audit.record(
        ClaimCodes.ENTITY_TYPE, claim.getClaimNo(), AuditAction.UPDATE, "Cover version " + change);
    return claim;
  }

  private static String summary(LossDetails d) {
    return d.getLossDate()
        + " "
        + d.getLossNature()
        + "/"
        + d.getClaimType()
        + " CAT "
        + d.getCatastropheCode()
        + " amount "
        + d.getClaimAmount()
        + " deductible "
        + d.getDeductible()
        + " reserve "
        + d.getInitialReserve();
  }

  private static String withRemark(String remark) {
    return remark == null || remark.isBlank() ? "" : ": " + remark.strip();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
