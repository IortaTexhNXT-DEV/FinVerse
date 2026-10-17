package com.iortatechnxt.brokerverse.eb.setup.api.dto;

import com.iortatechnxt.brokerverse.eb.domain.EbRequiredDocument;
import com.iortatechnxt.brokerverse.eb.domain.EbThresholdRule;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Request and response bodies of the EB Setup API (FR-EB-034, 042). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class SetupDtos {

  private SetupDtos() {}

  /**
   * A threshold rule.
   *
   * @param id id
   * @param benefitLine benefit line, null for every line
   * @param measure TSI or ANNUAL_PREMIUM
   * @param amount amount
   * @param currency currency
   * @param approverPermission approver permission
   * @param approvalLevel level
   * @param effectiveFrom effective from
   * @param effectiveTo effective to
   * @param description description
   * @param recordStatus maker-checker status
   * @param maker maker
   * @param authorizedBy checker
   * @param authorizedAt authorised at
   */
  public record ThresholdRuleResponse(
      Long id,
      String benefitLine,
      String measure,
      BigDecimal amount,
      String currency,
      String approverPermission,
      int approvalLevel,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String description,
      String recordStatus,
      String maker,
      String authorizedBy,
      Instant authorizedAt) {

    /**
     * Maps a rule.
     *
     * @param r rule
     * @return response
     */
    public static ThresholdRuleResponse from(EbThresholdRule r) {
      return new ThresholdRuleResponse(
          r.getId(),
          r.getBenefitLine(),
          r.getMeasure().name(),
          r.getAmount(),
          r.getCurrency(),
          r.getApproverPermission(),
          r.getApprovalLevel(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          r.getDescription(),
          r.getRecordStatus().name(),
          r.getMaker(),
          r.getAuthorizedBy(),
          r.getAuthorizedAt());
    }
  }

  /**
   * A required document.
   *
   * @param id id
   * @param processType process
   * @param benefitLine benefit line, null for every line
   * @param documentType document type
   * @param mandatory mandatory
   * @param recordStatus maker-checker status
   * @param maker maker
   * @param authorizedBy checker
   */
  public record RequiredDocumentResponse(
      Long id,
      String processType,
      String benefitLine,
      String documentType,
      boolean mandatory,
      String recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps a requirement.
     *
     * @param d requirement
     * @return response
     */
    public static RequiredDocumentResponse from(EbRequiredDocument d) {
      return new RequiredDocumentResponse(
          d.getId(),
          d.getProcessType(),
          d.getBenefitLine(),
          d.getDocumentType(),
          d.isMandatory(),
          d.getRecordStatus().name(),
          d.getMaker(),
          d.getAuthorizedBy());
    }
  }
}
