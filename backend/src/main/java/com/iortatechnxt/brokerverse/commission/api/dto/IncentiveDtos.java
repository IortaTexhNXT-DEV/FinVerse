package com.iortatechnxt.brokerverse.commission.api.dto;

import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission;
import com.iortatechnxt.brokerverse.commission.domain.CertificateSubmission.OrLink;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Beneficiary;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Calculation;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.PeriodType;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.RunStatus;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.SchemeType;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRun;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveRunLine;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveScheme;
import com.iortatechnxt.brokerverse.commission.domain.IncentiveTier;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Request and response records of the incentive, BIR certificate and estimated item API
 * (CMRID.003/005/006/010/015, RMTID.037).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class IncentiveDtos {

  private IncentiveDtos() {}

  /**
   * An incentive scheme.
   *
   * @param id id
   * @param code code
   * @param terms terms and tiers
   */
  public record SchemeResponse(Long id, String code, SchemeTerms terms) {

    /**
     * Maps a scheme.
     *
     * @param s scheme
     * @return response
     */
    public static SchemeResponse from(IncentiveScheme s) {
      return new SchemeResponse(
          s.getId(),
          s.getCode(),
          new SchemeTerms(
              s.getName(),
              s.getSchemeType(),
              s.getCalculation(),
              s.getPeriodType(),
              s.getBeneficiary(),
              s.getInsurerCode(),
              s.getSegments(),
              s.getProductLines(),
              s.getEffectiveFrom(),
              s.getEffectiveTo(),
              s.isActive(),
              s.getDescription(),
              s.getTiers()));
    }
  }

  /**
   * Terms of a scheme (request and response).
   *
   * @param name name
   * @param schemeType No Touch, Top Up, Motor Mania or other
   * @param calculation target tiered or fixed per policy
   * @param periodType period
   * @param beneficiary BDOI or branches
   * @param insurerCode insurer, null for all
   * @param segments segments, empty for all
   * @param productLines product lines, empty for all
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param active active
   * @param description description
   * @param tiers tiers
   */
  public record SchemeTerms(
      @NotBlank @Size(max = 120) String name,
      @NotNull SchemeType schemeType,
      @NotNull Calculation calculation,
      @NotNull PeriodType periodType,
      @NotNull Beneficiary beneficiary,
      @Size(max = 30) String insurerCode,
      List<String> segments,
      List<String> productLines,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      boolean active,
      @Size(max = 500) String description,
      List<IncentiveTier> tiers) {

    /**
     * The domain terms.
     *
     * @return terms
     */
    public IncentiveScheme.Terms toTerms() {
      return new IncentiveScheme.Terms(
          name,
          schemeType,
          calculation,
          periodType,
          beneficiary,
          insurerCode == null || insurerCode.isBlank() ? null : insurerCode.strip(),
          segments,
          productLines,
          effectiveFrom,
          effectiveTo,
          active,
          description,
          tiers);
    }
  }

  /**
   * A new scheme.
   *
   * @param companyId company
   * @param code code
   * @param terms terms
   */
  public record SchemeRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 30) String code,
      @NotNull @Valid SchemeTerms terms) {}

  /**
   * An incentive run.
   *
   * @param id id
   * @param runNo run number
   * @param schemeId scheme
   * @param periodFrom first day
   * @param periodTo last day
   * @param status status
   * @param totals totals
   * @param postedAt posted at
   * @param postedBy posted by
   * @param journalRefs journals and disbursement requests
   * @param createdAt computed at
   * @param createdBy computed by
   */
  public record RunResponse(
      Long id,
      String runNo,
      Long schemeId,
      LocalDate periodFrom,
      LocalDate periodTo,
      RunStatus status,
      IncentiveRun.Totals totals,
      Instant postedAt,
      String postedBy,
      String journalRefs,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps a run.
     *
     * @param r run
     * @return response
     */
    public static RunResponse from(IncentiveRun r) {
      return new RunResponse(
          r.getId(),
          r.getRunNo(),
          r.getSchemeId(),
          r.getPeriodFrom(),
          r.getPeriodTo(),
          r.getStatus(),
          new IncentiveRun.Totals(
              r.getEligibleCount(),
              r.getEligibleProduction(),
              r.getExcludedCount(),
              r.getExcludedAmount(),
              r.getTierApplied(),
              r.getIncentiveAmount(),
              r.getPassOnAmount()),
          r.getPostedAt(),
          r.getPostedBy(),
          r.getJournalRefs(),
          r.getCreatedAt(),
          r.getCreatedBy());
    }
  }

  /**
   * A line of a run.
   *
   * @param invoiceNo invoice
   * @param insurerCode insurer
   * @param salesUnit sales unit
   * @param segment segment
   * @param productLine product line
   * @param basicPremium basic premium
   * @param grossPremium gross premium
   * @param exclusionReason exclusion rule, null when eligible
   * @param incentive incentive
   */
  public record RunLineResponse(
      String invoiceNo,
      String insurerCode,
      String salesUnit,
      String segment,
      String productLine,
      BigDecimal basicPremium,
      BigDecimal grossPremium,
      String exclusionReason,
      BigDecimal incentive) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static RunLineResponse from(IncentiveRunLine l) {
      return new RunLineResponse(
          l.getInvoiceNo(),
          l.getInsurerCode(),
          l.getSalesUnit(),
          l.getSegment(),
          l.getProductLine(),
          l.getBasicPremium(),
          l.getGrossPremium(),
          l.getExclusionReason(),
          l.getIncentive());
    }
  }

  /**
   * A run to compute.
   *
   * @param schemeId scheme
   * @param from first booking date
   * @param to last booking date
   */
  public record RunRequest(
      @NotNull Long schemeId, @NotNull LocalDate from, @NotNull LocalDate to) {}

  /**
   * A certificate submission.
   *
   * @param id id
   * @param submissionNo submission number
   * @param insurerCode insurer
   * @param certificate certificate facts and ORs
   * @param stage stage
   * @param rejectReason reason of the last rejection
   * @param submittedCount submissions so far
   * @param decidedAt decided at
   * @param decidedBy decided by
   * @param createdAt submitted at
   * @param createdBy submitted by
   */
  public record CertificateResponse(
      Long id,
      String submissionNo,
      String insurerCode,
      CertificateRequest certificate,
      String stage,
      String rejectReason,
      int submittedCount,
      Instant decidedAt,
      String decidedBy,
      Instant createdAt,
      String createdBy) {

    /**
     * Maps a submission.
     *
     * @param c submission
     * @return response
     */
    public static CertificateResponse from(CertificateSubmission c) {
      return new CertificateResponse(
          c.getId(),
          c.getSubmissionNo(),
          c.getInsurerCode(),
          new CertificateRequest(
              c.getCompanyId(),
              c.getInsurerCode(),
              c.getCertificateForm(),
              c.getCertificateNo(),
              c.getPeriodFrom(),
              c.getPeriodTo(),
              c.getTaxWithheld(),
              c.getReceipts()),
          c.getStage(),
          c.getRejectReason(),
          c.getSubmittedCount(),
          c.getDecidedAt(),
          c.getDecidedBy(),
          c.getCreatedAt(),
          c.getCreatedBy());
    }
  }

  /**
   * A certificate to submit or resubmit.
   *
   * @param companyId company
   * @param insurerCode insurer that issued it
   * @param form BIR form (e.g. 2307)
   * @param number certificate number
   * @param periodFrom period start
   * @param periodTo period end
   * @param taxWithheld tax withheld
   * @param receipts ORs covered
   */
  public record CertificateRequest(
      Long companyId,
      @Size(max = 30) String insurerCode,
      @NotBlank @Size(max = 20) String form,
      @NotBlank @Size(max = 60) String number,
      @NotNull LocalDate periodFrom,
      @NotNull LocalDate periodTo,
      @NotNull BigDecimal taxWithheld,
      List<OrLink> receipts) {

    /**
     * The domain certificate.
     *
     * @return certificate
     */
    public CertificateSubmission.Certificate toCertificate() {
      return new CertificateSubmission.Certificate(
          form.strip(), number.strip(), periodFrom, periodTo, taxWithheld, receipts);
    }
  }

  /**
   * A reason.
   *
   * @param reason reason
   */
  public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {}

  /**
   * An invoice flagged estimated.
   *
   * @param invoiceNo invoice
   * @param arn ARN
   * @param insurerCode insurer
   * @param assuredName assured
   * @param bookingDate booking date
   * @param grossPremium gross premium
   * @param estimated estimated flag
   */
  public record EstimatedResponse(
      String invoiceNo,
      String arn,
      String insurerCode,
      String assuredName,
      LocalDate bookingDate,
      BigDecimal grossPremium,
      boolean estimated) {

    /**
     * Maps an invoice.
     *
     * @param i invoice
     * @return response
     */
    public static EstimatedResponse from(OpsInvoice i) {
      return new EstimatedResponse(
          i.getInvoiceNo(),
          i.getArn(),
          i.getInsurerCode(),
          i.getAssuredName(),
          i.getClassification().bookingDate(),
          i.getGrossPremium(),
          i.isEstimated());
    }
  }

  /**
   * An estimated flag change.
   *
   * @param invoiceNo invoice
   * @param estimated flag
   * @param reason reason
   */
  public record EstimateRequest(
      @NotBlank String invoiceNo, boolean estimated, @NotBlank @Size(max = 200) String reason) {}
}
