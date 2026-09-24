package com.iortatechnxt.brokerverse.claims.api.dto;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimPolicy;
import com.iortatechnxt.brokerverse.claims.domain.ClaimStatus;
import com.iortatechnxt.brokerverse.claims.domain.LossDetails;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * A claim with its policy facts, loss details, totals and (on the detail view) involved parties.
 *
 * @param id id
 * @param claimNo claim number
 * @param companyId company
 * @param branchId branch
 * @param status status
 * @param statusReason reason of the last status decision
 * @param closedOn date closed, repudiated or withdrawn
 * @param policyId policy id
 * @param policyNo policy number
 * @param productCode product code
 * @param productName product name
 * @param businessLine class
 * @param customerCode policyholder code
 * @param customerName policyholder name
 * @param insuredName insured
 * @param sharePct company share %
 * @param coinsuranceLeader whether the company leads the coinsurance
 * @param coinsurerCode coinsurer
 * @param riskDescription insured risk
 * @param sumInsured sum insured at 100 %
 * @param lossDate date of loss
 * @param reportedDate date reported
 * @param natureOfLoss nature of loss
 * @param causeOfLoss cause of loss
 * @param lossLocation place of loss
 * @param description narrative
 * @param currency currency
 * @param claimantCode claimant code
 * @param claimantName claimant name
 * @param totals estimate / paid totals
 * @param parties involved parties (empty on list views)
 * @param createdBy user who registered the claim
 * @param createdAt registration time
 */
public record ClaimResponse(
    Long id,
    String claimNo,
    Long companyId,
    Long branchId,
    ClaimStatus status,
    String statusReason,
    LocalDate closedOn,
    Long policyId,
    String policyNo,
    String productCode,
    String productName,
    String businessLine,
    String customerCode,
    String customerName,
    String insuredName,
    BigDecimal sharePct,
    boolean coinsuranceLeader,
    String coinsurerCode,
    String riskDescription,
    BigDecimal sumInsured,
    LocalDate lossDate,
    LocalDate reportedDate,
    String natureOfLoss,
    String causeOfLoss,
    String lossLocation,
    String description,
    String currency,
    String claimantCode,
    String claimantName,
    ClaimTotalsResponse totals,
    List<ClaimPartyResponse> parties,
    String createdBy,
    Instant createdAt) {

  /**
   * List view (parties omitted; claimant must be loaded).
   *
   * @param c claim
   * @return response
   */
  public static ClaimResponse summary(Claim c) {
    return of(c, List.of());
  }

  /**
   * Detail view (parties must be loaded).
   *
   * @param c claim
   * @return response
   */
  public static ClaimResponse detail(Claim c) {
    return of(c, c.getParties().stream().map(ClaimPartyResponse::from).toList());
  }

  private static ClaimResponse of(Claim c, List<ClaimPartyResponse> parties) {
    ClaimPolicy p = c.getPolicy();
    LossDetails l = c.getLoss();
    return new ClaimResponse(
        c.getId(),
        c.getClaimNo(),
        c.getCompanyId(),
        c.getBranchId(),
        c.getStatus(),
        c.getStatusReason(),
        c.getClosedOn(),
        p.getPolicyId(),
        p.getPolicyNo(),
        p.getProductCode(),
        p.getProductName(),
        p.getBusinessLine(),
        p.getCustomerCode(),
        p.getCustomerName(),
        p.getInsuredName(),
        p.getSharePct(),
        p.isCoinsuranceLeader(),
        p.getCoinsurerCode(),
        p.getRiskDescription(),
        p.getSumInsured(),
        l.getLossDate(),
        l.getReportedDate(),
        l.getNatureOfLoss(),
        l.getCauseOfLoss(),
        l.getLossLocation(),
        l.getDescription(),
        c.getCurrency(),
        c.getClaimant().getCode(),
        c.getClaimant().getName(),
        ClaimTotalsResponse.from(c),
        parties,
        c.getCreatedBy(),
        c.getCreatedAt());
  }
}
