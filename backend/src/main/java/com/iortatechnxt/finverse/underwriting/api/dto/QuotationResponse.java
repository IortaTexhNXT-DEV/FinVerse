package com.iortatechnxt.finverse.underwriting.api.dto;

import com.iortatechnxt.finverse.underwriting.domain.Quotation;
import com.iortatechnxt.finverse.underwriting.domain.QuotationStatus;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Quotation view with its iterations (latest last).
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param quotationNo number
 * @param productId product
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
 * @param validityDays validity
 * @param expiryDate last valid day
 * @param periodFrom proposed start
 * @param periodTo proposed end
 * @param currency currency
 * @param sharePct our share %
 * @param commissionRate commission %
 * @param status status
 * @param currentIteration current iteration number
 * @param createdBy maker
 * @param submittedBy submitter
 * @param decidedBy approver / rejecter
 * @param decisionReason rejection reason
 * @param convertedPolicyId policy created on conversion
 * @param iterations iterations
 */
public record QuotationResponse(
    Long id,
    Long companyId,
    Long branchId,
    String quotationNo,
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
    int validityDays,
    LocalDate expiryDate,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BigDecimal sharePct,
    BigDecimal commissionRate,
    QuotationStatus status,
    int currentIteration,
    String createdBy,
    String submittedBy,
    String decidedBy,
    String decisionReason,
    Long convertedPolicyId,
    List<IterationResponse> iterations) {

  /**
   * Maps a quotation (product, parties and iterations loaded).
   *
   * @param q quotation
   * @return response
   */
  public static QuotationResponse from(Quotation q) {
    return new QuotationResponse(
        q.getId(),
        q.getCompanyId(),
        q.getBranchId(),
        q.getQuotationNo(),
        q.getProduct().getId(),
        q.getProduct().getCode(),
        q.getProduct().getName(),
        q.getProduct().getBusinessLine(),
        q.getCustomer().getCode(),
        q.getCustomer().getName(),
        q.getInsuredName(),
        q.getSourceType(),
        q.getIntermediary() == null ? null : q.getIntermediary().getCode(),
        q.getIntermediary() == null ? null : q.getIntermediary().getName(),
        q.getIssueDate(),
        q.getValidityDays(),
        q.expiryDate(),
        q.getPeriodFrom(),
        q.getPeriodTo(),
        q.getCurrency(),
        q.getSharePct(),
        q.getCommissionRate(),
        q.getStatus(),
        q.getCurrentIteration(),
        q.getCreatedBy(),
        q.getSubmittedBy(),
        q.getDecidedBy(),
        q.getDecisionReason(),
        q.getConvertedPolicyId(),
        q.getIterations().stream()
            .map(it -> IterationResponse.from(it, q.getSharePct(), q.getCommissionRate()))
            .toList());
  }
}
