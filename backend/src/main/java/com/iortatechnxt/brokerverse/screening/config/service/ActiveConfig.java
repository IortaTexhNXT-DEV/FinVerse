package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.ConfigType;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Read port of the versioned screening configuration (SNSRP-101-108; design section 2, "resolvers
 * {@code ActiveConfig} as of a date"). Matching, risk, cases and STR read their rules only through
 * this interface.
 *
 * <p>Two ways to read:
 *
 * <ul>
 *   <li><b>As of a date</b> ({@code matchCriteria(companyId, asOf)} ...): the version ACTIVE on
 *       that date (the ACTIVE or SUPERSEDED version of the type with the latest effective date on
 *       or before {@code asOf}). Empty when none is in force: the caller raises {@code
 *       SCR_NO_ACTIVE_CONFIG} (FR-SS-019 R3).
 *   <li><b>By version id</b> ({@code matchCriteria(versionId)} ...): a version a run, case, review
 *       or STR recorded earlier, so that later versions do not change it (FR-SS-010 R4). These
 *       throw {@code ResourceNotFoundException} when no version of that type has the id.
 * </ul>
 *
 * <p>The returned records are immutable snapshots, safe to cache and to keep for a whole run.
 */
public interface ActiveConfig {

  /**
   * The version of a type in force on a date.
   *
   * @param companyId the company
   * @param type the configuration type
   * @param scope the template type name for {@link ConfigType#TEMPLATE}, otherwise {@code null}
   * @param asOf the date
   * @return the version, empty when none is in force
   */
  Optional<ConfigVersionRef> activeVersion(
      Long companyId, ConfigType type, String scope, LocalDate asOf);

  /**
   * Matching criteria of a version.
   *
   * @param versionId a MATCH_CRITERIA version id
   * @return the criteria
   */
  MatchCriteria matchCriteria(Long versionId);

  /**
   * Risk categories and rules of a version.
   *
   * @param versionId a RISK_RULES version id
   * @return the categories and rules
   */
  RiskRules riskRules(Long versionId);

  /**
   * Approval and escalation routes of a version.
   *
   * @param versionId an APPROVAL_MATRIX version id
   * @return the matrix
   */
  ApprovalMatrix approvalMatrix(Long versionId);

  /**
   * Assignment scenarios of a version.
   *
   * @param versionId an ASSIGNMENT_MATRIX version id
   * @return the matrix
   */
  AssignmentMatrix assignmentMatrix(Long versionId);

  /**
   * SLA rows of a version.
   *
   * @param versionId an SLA_MATRIX version id
   * @return the matrix
   */
  SlaMatrix slaMatrix(Long versionId);

  /**
   * Validation rules of a version.
   *
   * @param versionId a VALIDATION_RULES version id
   * @return the rules
   */
  ValidationRules validationRules(Long versionId);

  /**
   * A review or STR template version.
   *
   * @param versionId a TEMPLATE version id
   * @return the template with its fields
   */
  ReviewTemplate template(Long versionId);

  /**
   * An STR extraction layout version.
   *
   * @param versionId an STR_LAYOUT version id
   * @return the layout with its columns
   */
  StrLayout strLayout(Long versionId);

  /**
   * Matching criteria in force on a date.
   *
   * @param companyId the company
   * @param asOf the date
   * @return the criteria, empty when no version is in force
   */
  default Optional<MatchCriteria> matchCriteria(Long companyId, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.MATCH_CRITERIA, null, asOf)
        .map(v -> matchCriteria(v.id()));
  }

  /**
   * Risk rules in force on a date.
   *
   * @param companyId the company
   * @param asOf the date
   * @return the rules, empty when no version is in force
   */
  default Optional<RiskRules> riskRules(Long companyId, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.RISK_RULES, null, asOf).map(v -> riskRules(v.id()));
  }

  /**
   * Approval matrix in force on a date.
   *
   * @param companyId the company
   * @param asOf the date
   * @return the matrix, empty when no version is in force
   */
  default Optional<ApprovalMatrix> approvalMatrix(Long companyId, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.APPROVAL_MATRIX, null, asOf)
        .map(v -> approvalMatrix(v.id()));
  }

  /**
   * Assignment matrix in force on a date.
   *
   * @param companyId the company
   * @param asOf the date
   * @return the matrix, empty when no version is in force
   */
  default Optional<AssignmentMatrix> assignmentMatrix(Long companyId, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.ASSIGNMENT_MATRIX, null, asOf)
        .map(v -> assignmentMatrix(v.id()));
  }

  /**
   * SLA matrix in force on a date.
   *
   * @param companyId the company
   * @param asOf the date
   * @return the matrix, empty when no version is in force
   */
  default Optional<SlaMatrix> slaMatrix(Long companyId, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.SLA_MATRIX, null, asOf).map(v -> slaMatrix(v.id()));
  }

  /**
   * Validation rules in force on a date.
   *
   * @param companyId the company
   * @param asOf the date
   * @return the rules, empty when no version is in force
   */
  default Optional<ValidationRules> validationRules(Long companyId, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.VALIDATION_RULES, null, asOf)
        .map(v -> validationRules(v.id()));
  }

  /**
   * The template of a type in force on a date.
   *
   * @param companyId the company
   * @param templateType the template type
   * @param asOf the date
   * @return the template, empty when no version is in force
   */
  default Optional<ReviewTemplate> template(
      Long companyId, TemplateType templateType, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.TEMPLATE, templateType.name(), asOf)
        .map(v -> template(v.id()));
  }

  /**
   * STR layout in force on a date.
   *
   * @param companyId the company
   * @param asOf the date
   * @return the layout, empty when no version is in force
   */
  default Optional<StrLayout> strLayout(Long companyId, LocalDate asOf) {
    return activeVersion(companyId, ConfigType.STR_LAYOUT, null, asOf).map(v -> strLayout(v.id()));
  }
}
