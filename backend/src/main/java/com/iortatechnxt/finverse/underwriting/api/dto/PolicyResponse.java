package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Policy (or marine certificate) view.
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param policyNo number
 * @param productId product id
 * @param productCode product code
 * @param productName product name
 * @param businessLine line of business
 * @param customerCode client code
 * @param customerName client name
 * @param insuredName insured
 * @param sourceType channel
 * @param intermediaryCode agent / broker code
 * @param intermediaryName agent / broker name
 * @param issueDate issue date
 * @param periodFrom period start
 * @param periodTo period end
 * @param currency currency
 * @param uwYear underwriting year
 * @param businessType direct / coinsurance
 * @param sharePct our share %
 * @param coinsurerCode coinsurer code
 * @param coinsuranceLeader company leads the coinsurance
 * @param discountRate discount %
 * @param loadingRate loading %
 * @param openCoverId open cover (certificates)
 * @param openCoverNo open cover number
 * @param quotationId source quotation
 * @param cancelledOn cancellation date
 * @param document workflow and accounting references
 * @param premium premium of the original issue
 * @param risks risks (detail view only)
 */
public record PolicyResponse(
    Long id,
    Long companyId,
    Long branchId,
    String policyNo,
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
    String currency,
    int uwYear,
    BusinessType businessType,
    BigDecimal sharePct,
    String coinsurerCode,
    boolean coinsuranceLeader,
    BigDecimal discountRate,
    BigDecimal loadingRate,
    Long openCoverId,
    String openCoverNo,
    Long quotationId,
    LocalDate cancelledOn,
    DocumentStatusResponse document,
    PremiumResponse premium,
    List<RiskResponse> risks) {

  /**
   * Maps a policy with its risks.
   *
   * @param p policy (risks loaded)
   * @return response
   */
  public static PolicyResponse withRisks(Policy p) {
    return map(p, p.getRisks().stream().map(RiskResponse::from).toList());
  }

  /**
   * Maps a policy without risks (list views).
   *
   * @param p policy
   * @return response
   */
  public static PolicyResponse summary(Policy p) {
    return map(p, List.of());
  }

  private static PolicyResponse map(Policy p, List<RiskResponse> risks) {
    Party intermediary = p.getIntermediary();
    return new PolicyResponse(
        p.getId(),
        p.getCompanyId(),
        p.getBranchId(),
        p.getPolicyNo(),
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
        p.getCurrency(),
        p.getUwYear(),
        p.getBusinessType(),
        p.getSharePct(),
        p.getCoinsurer() == null ? null : p.getCoinsurer().getCode(),
        p.isCoinsuranceLeader(),
        p.getDiscountRate(),
        p.getLoadingRate(),
        p.getOpenCover() == null ? null : p.getOpenCover().getId(),
        p.getOpenCover() == null ? null : p.getOpenCover().getOpenCoverNo(),
        p.getQuotationId(),
        p.getCancelledOn(),
        DocumentStatusResponse.of(p.getWorkflow(), p.getRefs(), p.getCreatedBy(), p.getCreatedAt()),
        PremiumResponse.from(p.getPremium()),
        risks);
  }
}
