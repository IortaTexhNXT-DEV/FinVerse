package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable view of a policy header for other modules (claims, reinsurance, receivables). The
 * period is the current one (after any approved renewal).
 *
 * @param id policy id
 * @param policyNo policy (or certificate) number
 * @param companyId company
 * @param branchId branch
 * @param productId product id
 * @param productCode product code
 * @param productName product name
 * @param businessLine line of business
 * @param customerCode client party code
 * @param customerName client name
 * @param insuredName insured name
 * @param sourceType channel
 * @param intermediaryCode agent / broker code, null for direct
 * @param intermediaryName agent / broker name, null for direct
 * @param issueDate issue date
 * @param periodFrom current period start
 * @param periodTo current period end
 * @param uwYear underwriting year
 * @param currency currency
 * @param businessType direct / with coinsurance
 * @param sharePct company share %
 * @param coinsurerCode coinsurer code, null when not coinsured
 * @param status status
 * @param approvalDate approval (accounting) date of the original issue
 * @param cancelledOn cancellation effective date, null when not cancelled
 * @param openCoverNo open cover of a marine certificate, else null
 */
public record PolicySnapshot(
    Long id,
    String policyNo,
    Long companyId,
    Long branchId,
    Long productId,
    String productCode,
    String productName,
    String businessLine,
    String customerCode,
    String customerName,
    String insuredName,
    SourceType sourceType,
    String intermediaryCode,
    String intermediaryName,
    LocalDate issueDate,
    LocalDate periodFrom,
    LocalDate periodTo,
    int uwYear,
    String currency,
    BusinessType businessType,
    BigDecimal sharePct,
    String coinsurerCode,
    PolicyStatus status,
    LocalDate approvalDate,
    LocalDate cancelledOn,
    String openCoverNo) {

  /**
   * Maps a policy (product, parties and open cover must be loaded).
   *
   * @param p policy
   * @return snapshot
   */
  public static PolicySnapshot of(Policy p) {
    Party intermediary = p.getIntermediary();
    return new PolicySnapshot(
        p.getId(),
        p.getPolicyNo(),
        p.getCompanyId(),
        p.getBranchId(),
        p.getProduct().getId(),
        p.getProduct().getCode(),
        p.getProduct().getName(),
        p.getProduct().getBusinessLine(),
        p.getCustomer().getCode(),
        p.getCustomer().getName(),
        p.getInsuredName(),
        p.getSourceType(),
        intermediary == null ? null : intermediary.getCode(),
        intermediary == null ? null : intermediary.getName(),
        p.getIssueDate(),
        p.getPeriodFrom(),
        p.getPeriodTo(),
        p.getUwYear(),
        p.getCurrency(),
        p.getBusinessType(),
        p.getSharePct(),
        p.getCoinsurer() == null ? null : p.getCoinsurer().getCode(),
        p.getStatus(),
        p.getWorkflow().getApprovalDate(),
        p.getCancelledOn(),
        p.getOpenCover() == null ? null : p.getOpenCover().getOpenCoverNo());
  }

  /**
   * Whether the policy covers a date (approved and not cancelled before it).
   *
   * @param date date
   * @return true when in force
   */
  public boolean isInForce(LocalDate date) {
    boolean live =
        status == PolicyStatus.APPROVED || cancelledOn != null && date.isBefore(cancelledOn);
    return live && !date.isBefore(periodFrom) && !date.isAfter(periodTo);
  }
}
