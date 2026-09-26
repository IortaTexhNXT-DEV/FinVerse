package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * The stages of workflow {@code SCR_CASE} (SNSRP-401; design section 7), mirrored on the case from
 * {@code WorkCaseTransitioned}.
 */
public enum CaseStage {
  /** Created, not yet routed. */
  NEW,
  /** Worked by the investigator (SNSRP-501, 502, 601). */
  INVESTIGATION,
  /** Returned to the investigator by the unit head or Compliance (SNSRP-702, 703). */
  RETURNED,
  /** Unit head approval (SNSRP-702). */
  UNIT_HEAD_APPROVAL,
  /** Compliance review of the BU escalation (SNSRP-703). */
  COMPLIANCE_REVIEW,
  /** AML Committee decision (SNSRP-704). */
  AML_COMMITTEE,
  /** STR preparation (SNSRP-705). */
  STR_PREPARATION,
  /** STR extraction and filing (SNSRP-706). */
  STR_EXTRACTION,
  /** Closed (terminal; re-opened with a reason). */
  CLOSED;

  private static final Set<CaseStage> REASSIGNABLE =
      EnumSet.of(INVESTIGATION, RETURNED, UNIT_HEAD_APPROVAL, COMPLIANCE_REVIEW);

  /**
   * Whether the investigator works the case in this stage (review, documents, submit).
   *
   * @return true for INVESTIGATION and RETURNED
   */
  public boolean isInvestigation() {
    return this == INVESTIGATION || this == RETURNED;
  }

  /**
   * Whether a case in this stage can be re-assigned (FR-SS-043: review and approval stages).
   *
   * @return true when re-assignable
   */
  public boolean isReassignable() {
    return REASSIGNABLE.contains(this);
  }
}
