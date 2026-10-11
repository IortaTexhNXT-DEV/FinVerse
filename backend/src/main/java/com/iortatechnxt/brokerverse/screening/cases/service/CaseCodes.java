package com.iortatechnxt.brokerverse.screening.cases.service;

/**
 * Names shared by the case services (SNSRP-303, 401-405, 501-502, 601, 701-706, 801-802): the
 * workflow and entity type, the stage permissions, the lists of values and the notification events
 * of V1050.
 */
public final class CaseCodes {

  /** Workflow of screening cases (V1050). */
  public static final String WORKFLOW = "SCR_CASE";

  /** Entity type of cases in the workflow, the attachments and the audit trail. */
  public static final String ENTITY = "ScreeningCase";

  /** Frontend route of a case. */
  public static final String LINK = "/screening/cases/";

  /** View cases. */
  public static final String VIEW = "SCR_VIEW";

  /** Investigation stage. */
  public static final String INVESTIGATE = "SCR_INVESTIGATE";

  /** Manual risk tag (SNSRP-304). */
  public static final String RISK_TAG = "SCR_RISK_TAG";

  /** Re-assignment (SNSRP-404). */
  public static final String CASE_ASSIGN = "SCR_CASE_ASSIGN";

  /** Unit head approval (SNSRP-702). */
  public static final String CASE_APPROVE = "SCR_CASE_APPROVE";

  /** Compliance review and STR preparation (SNSRP-703, 705). */
  public static final String COMPLIANCE_REVIEW = "SCR_COMPLIANCE_REVIEW";

  /** AML Committee (SNSRP-704). */
  public static final String COMMITTEE = "SCR_COMMITTEE";

  /** STR extraction and filing (SNSRP-706). */
  public static final String STR_EXTRACT = "SCR_STR_EXTRACT";

  /** Compliance reports. */
  public static final String REPORT_VIEW = "SCR_REPORT_VIEW";

  /** Screening audit log. */
  public static final String AUDIT_VIEW = "SCR_AUDIT_VIEW";

  /** Dispositions per stage (parent = stage, SNSRP-107). */
  public static final String DISPOSITION_LOV = "SCR_DISPOSITION";

  /** Re-assignment reasons (SNSRP-404). */
  public static final String REASSIGN_LOV = "SCR_REASSIGN_REASON";

  /** Form types of documents (SNSRP-601). */
  public static final String FORM_TYPE_LOV = "SCR_FORM_TYPE";

  /** Screening document types (parent = DOCUMENT_TYPE, SNSRP-601). */
  public static final String DOCUMENT_TYPE_LOV = "SCR_DOCUMENT_TYPE";

  /** Unit Compliance Coordinator role (SNSRP-303, 802 notices). */
  public static final String UCC_ROLE = "UNIT_COMPLIANCE_COORD";

  /** Investigator role (SNSRP-303 notice). */
  public static final String INVESTIGATOR_ROLE = "SCR_INVESTIGATOR";

  /** Notice: a case waits for approval (SNSRP-702, 703). */
  public static final String EVENT_FOR_APPROVAL = "SCR_CASE_FOR_APPROVAL";

  /** Notice: a case was returned (SNSRP-702, 703). */
  public static final String EVENT_RETURNED = "SCR_CASE_RETURNED";

  /** Notice: the committee is asked to vote (SNSRP-704). */
  public static final String EVENT_COMMITTEE = "SCR_COMMITTEE_REVIEW";

  /** Notice: SLA reminder (SNSRP-405). */
  public static final String EVENT_SLA_REMINDER = "SCR_SLA_REMINDER";

  /** Notice: SLA escalation (SNSRP-108, 405). */
  public static final String EVENT_SLA_ESCALATION = "SCR_SLA_ESCALATION";

  /** Notice: missing documents (SNSRP-802). */
  public static final String EVENT_DOCUMENT_REMINDER = "SCR_DOCUMENT_REMINDER";

  /** Notice: high-risk or PEP client without an active policy (SNSRP-303). */
  public static final String EVENT_NO_POLICY = "SCR_NO_POLICY_HIT";

  /** Alert: SLA breached (SNSRP-405). */
  public static final String ALERT_SLA_BREACH = "SCR_SLA_BREACH";

  private CaseCodes() {}

  /**
   * The frontend route of a case.
   *
   * @param caseId case
   * @return route
   */
  public static String link(Long caseId) {
    return LINK + caseId;
  }
}
