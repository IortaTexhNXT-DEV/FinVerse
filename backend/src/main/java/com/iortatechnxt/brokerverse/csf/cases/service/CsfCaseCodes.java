package com.iortatechnxt.brokerverse.csf.cases.service;

/** Codes of the case management of the Customer Servicing Facility (FR-CSF-050 to 061). */
public final class CsfCaseCodes {

  /** Entity type of a case in the audit trail (the case history) and the documents. */
  public static final String ENTITY = "CsfCase";

  /** Link of a case in the notifications. */
  public static final String LINK = "/csf/cases/";

  /** List of the channels. */
  public static final String LOV_CHANNEL = "CSF_CASE_CHANNEL";

  /** List of the case types. */
  public static final String LOV_TYPE = "CSF_CASE_TYPE";

  /** List of the natures of transaction. */
  public static final String LOV_NATURE = "CSF_CASE_NATURE";

  /** List of the product types. */
  public static final String LOV_PRODUCT = "CSF_CASE_PRODUCT";

  /** List of the market segments. */
  public static final String LOV_SEGMENT = "CSF_CASE_SEGMENT";

  /** List of the fulfilment units. */
  public static final String LOV_UNIT = "CSF_FULFILMENT_UNIT";

  /** List of the priorities. */
  public static final String LOV_PRIORITY = "CSF_CASE_PRIORITY";

  /** List of the escalation reasons. */
  public static final String LOV_ESCALATION = "CSF_ESCALATION_REASON";

  /** List of the return reasons. */
  public static final String LOV_RETURN = "CSF_RETURN_REASON";

  /** List of the closure reasons. */
  public static final String LOV_CLOSURE = "CSF_CLOSURE_REASON";

  /** Document types of the case uploads (the CSF upload list). */
  public static final String LOV_DOCUMENT_TYPE = "CSF_DOCUMENT_TYPE";

  /** Escalation reason of the automatic escalation on breach. */
  public static final String PAST_TAT = "PAST_TAT";

  /** Default priority. */
  public static final String NORMAL = "NORMAL";

  /** Parameter: prefix of the Case ID. */
  public static final String PARAM_PREFIX = "CSF_CASE_PREFIX";

  /** Parameter: refuse a second open case of the same transaction. */
  public static final String PARAM_DUPLICATE = "CSF_CASE_DUPLICATE_CHECK";

  /** Parameter: working days before the due date when a case approaches its breach. */
  public static final String PARAM_NEAR_BREACH = "CSF_CASE_NEAR_BREACH_DAYS";

  /** SLA status: within the turnaround time. */
  public static final String WITHIN_TAT = "WITHIN_TAT";

  /** SLA status: approaching the breach. */
  public static final String APPROACHING = "APPROACHING";

  /** SLA status: past the due date. */
  public static final String BREACHED = "BREACHED";

  private CsfCaseCodes() {}
}
