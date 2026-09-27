package com.iortatechnxt.brokerverse.renewal.service;

/**
 * Codes shared by the Renewal services (RENEWAL_DESIGN sections 6, 7 and 9): workflow, record type,
 * lists of values, document types, exception codes, notification events, number series and
 * templates. The values are those seeded by V1010.
 */
public final class RenewalCodes {

  /** Workflow of a renewal candidate. */
  public static final String WORKFLOW = "RNW_CASE";

  /** Record type of a candidate in the workflow, audit trail and attachments. */
  public static final String ENTITY = "RenewalCandidate";

  /** Record type of an insurer batch (attachments, audit trail). */
  public static final String ENTITY_BATCH = "RenewalInsurerBatch";

  /** Route of the record page. */
  public static final String LINK = "/renewal/candidates/";

  /** Module of alerts and events. */
  public static final String MODULE = "RENEWAL";

  /** List of dispositions (labels). */
  public static final String LOV_DISPOSITION = "RNW_DISPOSITION";

  /** List of reasons for Not for Renewal. */
  public static final String LOV_NONRENEWAL_REASON = "RNW_NONRENEWAL_REASON";

  /** List of return reasons. */
  public static final String LOV_RETURN_REASON = "RNW_RETURN_REASON";

  /** List of transfer reasons. */
  public static final String LOV_TRANSFER_REASON = "RNW_TRANSFER_REASON";

  /** List of follow-up outcomes. */
  public static final String LOV_FOLLOWUP_OUTCOME = "RNW_FOLLOWUP_OUTCOME";

  /** List of follow-up channels. */
  public static final String LOV_FOLLOWUP_CHANNEL = "RNW_FOLLOWUP_CHANNEL";

  /** List of override reasons. */
  public static final String LOV_OVERRIDE_REASON = "RNW_OVERRIDE_REASON";

  /** Reason: loan fully paid (LAMD paid-off). */
  public static final String REASON_LOAN_PAID = "LOAN_FULLY_PAID";

  /** Reason: RMU (LAMD). */
  public static final String REASON_RMU = "RMU";

  /** Reason: non-renewable accounts (risk code, not in a complete file). */
  public static final String REASON_NON_RENEWABLE = "NON_RENEWABLE_ACCOUNT";

  /** Reason: transfer to another Marketing unit. */
  public static final String REASON_TRANSFER = "TRANSFER_TO_OTHER_UNIT";

  /** Reason: booked to a new invoice. */
  public static final String REASON_BOOKED_NEW_INVOICE = "BOOKED_TO_NEW_INVOICE";

  /** Reason: the insurer declined. */
  public static final String REASON_INSURER_DECLINED = "INSURER_DECLINED";

  /** Return reason counted as Disapproved. */
  public static final String RETURN_DISAPPROVED = "DISAPPROVED";

  /** Document type of a Renewal Advice (shared with Employee Benefits). */
  public static final String DOC_RENEWAL_ADVICE = "RENEWAL_ADVICE";

  /** Document type of the other renewal letters. */
  public static final String DOC_RENEWAL_LETTER = "RENEWAL_LETTER";

  /** Document type of an e-mail acceptance. */
  public static final String DOC_RA_ACCEPTANCE = "RA_ACCEPTANCE";

  /** Document type of a signed Renewal Advice. */
  public static final String DOC_SIGNED_RA = "SIGNED_RA";

  /** Document type of an insurer file. */
  public static final String DOC_INSURER_FILE = "INSURER_RENEWAL_FILE";

  /** Alert: renewal at risk. */
  public static final String ALERT_AT_RISK = "RNW_RENEWAL_AT_RISK";

  /** Alert: extraction failed. */
  public static final String ALERT_EXTRACTION_FAILED = "RNW_EXTRACTION_FAILED";

  /** Alert: insurer batch overdue. */
  public static final String ALERT_INSURER_OVERDUE = "RNW_INSURER_OVERDUE";

  /** Alert: letter not delivered. */
  public static final String ALERT_LETTER_FAILED = "RNW_LETTER_FAILED";

  /** Alert: exception ageing. */
  public static final String ALERT_EXCEPTION_AGEING = "RNW_EXCEPTION_AGEING";

  /** Event: assigned. */
  public static final String EVENT_ASSIGNED = "RNW_ASSIGNED";

  /** Event: transfer requested. */
  public static final String EVENT_TRANSFER_REQUESTED = "RNW_TRANSFER_REQUESTED";

  /** Event: transfer decided. */
  public static final String EVENT_TRANSFER_DECIDED = "RNW_TRANSFER_DECIDED";

  /** Event: returned. */
  public static final String EVENT_RETURNED = "RNW_RETURNED";

  /** Event: posted. */
  public static final String EVENT_POSTED = "RNW_POSTED";

  /** Event: insurer responded. */
  public static final String EVENT_INSURER_RESPONDED = "RNW_INSURER_RESPONDED";

  /** Event: accepted. */
  public static final String EVENT_ACCEPTED = "RNW_ACCEPTED";

  /** Event: renewed. */
  public static final String EVENT_RENEWED = "RNW_RENEWED";

  /** Event: go-live take-over completed. */
  public static final String EVENT_GOLIVE = "RNW_GOLIVE_COMPLETED";

  /** Event: package choice decided. */
  public static final String EVENT_PACKAGE_DECIDED = "RNW_PACKAGE_DECIDED";

  /** Outbox purpose of renewal letters. */
  public static final String PURPOSE_LETTER = "RENEWAL_LETTER";

  /** Outbox purpose of insurer batches. */
  public static final String PURPOSE_INSURER = "RENEWAL_INSURER";

  /** Template: first-notice Renewal Advice. */
  public static final String TEMPLATE_RA_FIRST = "RNW_RA_FIRST";

  /** Template: second-notice Renewal Advice. */
  public static final String TEMPLATE_RA_SECOND = "RNW_RA_SECOND";

  /** Template: No Advice Letter. */
  public static final String TEMPLATE_NAL = "RNW_NAL";

  /** Template: Not for Renewal Letter. */
  public static final String TEMPLATE_NFR = "RNW_NFR";

  /** Template: NRNS reminder. */
  public static final String TEMPLATE_NRNS = "RNW_NRNS_REMINDER";

  /** Template: non-acceptance letter. */
  public static final String TEMPLATE_NON_ACCEPTANCE = "RNW_NON_ACCEPTANCE";

  /** Template: insurer cover e-mail. */
  public static final String TEMPLATE_INSURER_COVER = "RNW_INSURER_COVER";

  /** Permission: view. */
  public static final String VIEW = "RNW_VIEW";

  /** Permission: extract and initiate. */
  public static final String EXTRACT = "RNW_EXTRACT";

  /** Permission: assign and transfer. */
  public static final String ASSIGN = "RNW_ASSIGN";

  /** Permission: disposition. */
  public static final String DISPOSE = "RNW_DISPOSE";

  /** Permission: Team Leader review. */
  public static final String REVIEW = "RNW_REVIEW";

  /** Permission: overrides. */
  public static final String OVERRIDE = "RNW_OVERRIDE";

  /** Permission: assign Processing Officers. */
  public static final String PROCESS_ASSIGN = "RNW_PROCESS_ASSIGN";

  /** Permission: processing. */
  public static final String PROCESS = "RNW_PROCESS";

  /** Permission: LAMD upload. */
  public static final String LAMD_UPLOAD = "RNW_LAMD_UPLOAD";

  /** Permission: Contact Center follow-up. */
  public static final String FOLLOWUP = "RNW_FOLLOWUP";

  /** Permission: set-up. */
  public static final String SETUP = "RNW_SETUP";

  /** Permission: package choice of migrated policies. */
  public static final String PACKAGE_REMAP = "RNW_PACKAGE_REMAP";

  /** Permission: upload dispositioned files and insurer responses. */
  public static final String UPLOAD = "RNW_UPLOAD";

  /** Permission: insurer batches and responses. */
  public static final String INSURER = "RNW_INSURER";

  /** Permission: generate Renewal Advices and letters. */
  public static final String RA_GENERATE = "RNW_RA_GENERATE";

  /** Permission: send Renewal Advices and letters. */
  public static final String RA_SEND = "RNW_RA_SEND";

  /** Permission: record acceptances. */
  public static final String ACCEPT = "RNW_ACCEPT";

  /** Permission: validate uploads. */
  public static final String VALIDATE = "RNW_VALIDATE";

  /** Permission: renewal reports. */
  public static final String REPORT_VIEW = "RNW_REPORT_VIEW";

  /** Permission: exports. */
  public static final String EXPORT = "RNW_EXPORT";

  /** Permission: letter templates. */
  public static final String TEMPLATE_MAINTAIN = "RNW_TEMPLATE_MAINTAIN";

  /** Permission of a checker of master data. */
  public static final String MASTER_AUTHORIZE = "MASTER_AUTHORIZE";

  private RenewalCodes() {}
}
