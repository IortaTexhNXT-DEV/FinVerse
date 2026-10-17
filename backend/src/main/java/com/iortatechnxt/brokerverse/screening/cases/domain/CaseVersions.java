package com.iortatechnxt.brokerverse.screening.cases.domain;

/**
 * The configuration versions in force when the case was opened (FR-SS-034 R4).
 *
 * @param matchVersionId MATCH_CRITERIA
 * @param riskVersionId RISK_RULES
 * @param approvalVersionId APPROVAL_MATRIX
 * @param assignmentVersionId ASSIGNMENT_MATRIX
 * @param slaVersionId SLA_MATRIX
 * @param validationVersionId VALIDATION_RULES
 */
public record CaseVersions(
    Long matchVersionId,
    Long riskVersionId,
    Long approvalVersionId,
    Long assignmentVersionId,
    Long slaVersionId,
    Long validationVersionId) {}
