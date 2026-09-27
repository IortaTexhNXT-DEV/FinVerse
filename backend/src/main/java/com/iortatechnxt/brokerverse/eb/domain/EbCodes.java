package com.iortatechnxt.brokerverse.eb.domain;

/**
 * Workflow codes, entity types (attachments, work cases, audit) and document number series of
 * Employee Benefits (EMPLOYEE_BENEFITS_DESIGN sections 7 and 8.4; workflows seeded by V1030).
 * Shared by the build waves; changed only by additions.
 */
public final class EbCodes {

  /** Workflow of a cycle (design 7.1). */
  public static final String WORKFLOW_CYCLE = "EB_CYCLE";

  /** Workflow of a franchise request (BRID-026, 027, 029). */
  public static final String WORKFLOW_FRANCHISE = "EB_FRANCHISE";

  /** Workflow of a member change (BRID-013, 025). */
  public static final String WORKFLOW_MEMBER_CHANGE = "EB_MEMBER_CHANGE";

  /** Workflow of an insurer SOA (BRID-021). */
  public static final String WORKFLOW_SOA = "EB_SOA";

  /** Entity type of a programme. */
  public static final String ENTITY_PROGRAMME = "EbProgramme";

  /** Entity type of a cycle (work case of EB_CYCLE). */
  public static final String ENTITY_CYCLE = "EbCycle";

  /** Entity type of a franchise request (work case of EB_FRANCHISE). */
  public static final String ENTITY_FRANCHISE = "EbFranchise";

  /** Entity type of a member change (work case of EB_MEMBER_CHANGE). */
  public static final String ENTITY_MEMBER_CHANGE = "EbMemberChange";

  /** Entity type of an insurer SOA (work case of EB_SOA). */
  public static final String ENTITY_SOA = "EbSoa";

  /** Programme numbers {@code EBP-<yyyy>-nnnnnn}. */
  public static final String PREFIX_PROGRAMME = "EBP";

  /** Cycle numbers {@code EBC-<yyyy>-nnnnnn}. */
  public static final String PREFIX_CYCLE = "EBC";

  /** Franchise request numbers {@code EBF-<yyyy>-nnnnnn}. */
  public static final String PREFIX_FRANCHISE = "EBF";

  /** Insurer request numbers {@code EBR-<yyyy>-nnnnnn}. */
  public static final String PREFIX_REQUEST = "EBR";

  /** Proposal numbers {@code EBPR-<yyyy>-nnnnnn}. */
  public static final String PREFIX_PROPOSAL = "EBPR";

  /** Comparative numbers {@code EBCA-<yyyy>-nnnnnn}. */
  public static final String PREFIX_COMPARATIVE = "EBCA";

  /** Member change numbers {@code EBM-<yyyy>-nnnnnn}. */
  public static final String PREFIX_MEMBER_CHANGE = "EBM";

  /** SOA intake numbers {@code EBS-<yyyy>-nnnnnn}. */
  public static final String PREFIX_SOA = "EBS";

  /** List of values of the benefit lines. */
  public static final String LOV_BENEFIT_LINE = "EB_BENEFIT_LINE";

  /** List of values of the teams. */
  public static final String LOV_TEAM = "EB_TEAM";

  /** Entity type of a tracked item (audit). */
  public static final String ENTITY_TRACKED_ITEM = "EbTrackedItem";

  /** List of values of the tracked item types. */
  public static final String LOV_TRACKED_ITEM_TYPE = "EB_TRACKED_ITEM_TYPE";

  /** List of values of the lost / not renewed reasons. */
  public static final String LOV_LOST_REASON = "EB_LOST_REASON";

  /** Template of the renewal advice (V1030). */
  public static final String TEMPLATE_RENEWAL_ADVICE = "EB_RENEWAL_ADVICE";

  /** Template of the renewal advice reminder (V1030). */
  public static final String TEMPLATE_RA_REMINDER = "EB_RA_REMINDER";

  /** Template of the tracked item follow-up (V1030). */
  public static final String TEMPLATE_ITEM_FOLLOWUP = "EB_ITEM_FOLLOWUP";

  /** Alert: renewal advice not sent inside the lead time (V1030). */
  public static final String ALERT_RA_NOT_SENT = "EB_RA_NOT_SENT";

  /** Alert: tracked item past due after the follow-ups (V1030). */
  public static final String ALERT_ITEM_OVERDUE = "EB_ITEM_OVERDUE";

  /** Notification event: client feedback received (V1030). */
  public static final String EVENT_FEEDBACK_RECEIVED = "EB_FEEDBACK_RECEIVED";

  /** Notification event: accounts of a cycle created for placement (V1030). */
  public static final String EVENT_PLACEMENT_TRIGGERED = "EB_PLACEMENT_TRIGGERED";

  /** Notification event: tracked item escalated (V1030). */
  public static final String EVENT_ITEM_ESCALATED = "EB_ITEM_ESCALATED";

  /** Notification event: Broker on Record validated or rejected (V1036). */
  public static final String EVENT_BOR_DECIDED = "EB_BOR_DECIDED";

  /** Retention record type of a programme (V1036). */
  public static final String RETENTION_PROGRAMME = "EB_PROGRAMME";

  /** Outbox purpose of the renewal advice and its reminders. */
  public static final String PURPOSE_RENEWAL_ADVICE = "EB_RENEWAL_ADVICE";

  /** Outbox purpose of the tracked item follow-ups. */
  public static final String PURPOSE_ITEM_FOLLOWUP = "EB_ITEM_FOLLOWUP";

  /** Front-end route of a programme page (link of notices, cases and alerts). */
  public static final String PROGRAMME_LINK = "/eb/programmes/";

  /** Entity type of a proposal (audit, attachments). */
  public static final String ENTITY_PROPOSAL = "EbProposal";

  /** Entity type of a comparative (audit, attachments, approvals). */
  public static final String ENTITY_COMPARATIVE = "EbComparative";

  /** Entity type of an insurer request (audit). */
  public static final String ENTITY_REQUEST = "EbInsurerRequest";

  /** Entity type of a TOR version (audit). */
  public static final String ENTITY_TOR = "EbTor";

  /** Entity type of a submission (audit). */
  public static final String ENTITY_SUBMISSION = "EbSubmission";

  /** Entity type of a roster version (audit). */
  public static final String ENTITY_ROSTER = "EbRoster";

  /** Entity type of a threshold rule (audit, approvals). */
  public static final String ENTITY_THRESHOLD_RULE = "EbThresholdRule";

  /** Entity type of a required document (audit, approvals). */
  public static final String ENTITY_REQUIRED_DOCUMENT = "EbRequiredDocument";

  /** List of values of the capability factors. */
  public static final String LOV_CAPABILITY_FACTOR = "EB_CAPABILITY_FACTOR";

  /** List of values of the franchise rejection reasons. */
  public static final String LOV_FRANCHISE_REJECT_REASON = "EB_FRANCHISE_REJECT_REASON";

  /** List of values of the SOA rejection reasons. */
  public static final String LOV_SOA_REJECT_REASON = "EB_SOA_REJECT_REASON";

  /** List of values of the civil status (members). */
  public static final String LOV_CIVIL_STATUS = "CIVIL_STATUS";

  /** Template of the Terms of Reference (V1030). */
  public static final String TEMPLATE_TOR = "EB_TOR";

  /** Template of the request for proposal e-mail (V1030). */
  public static final String TEMPLATE_RFP_COVER = "EB_RFP_COVER";

  /** Template of the franchise request e-mail (V1030). */
  public static final String TEMPLATE_FRANCHISE_REQUEST = "EB_FRANCHISE_REQUEST";

  /** Template of the franchise outcome advice (V1030). */
  public static final String TEMPLATE_FRANCHISE_ADVICE = "EB_FRANCHISE_ADVICE";

  /** Template of the comparative sent to the client (V1030). */
  public static final String TEMPLATE_COMPARATIVE = "EB_COMPARATIVE";

  /** Template of the revision relayed to an insurer (V1030). */
  public static final String TEMPLATE_REVISION_RELAY = "EB_REVISION_RELAY";

  /** Template of the member change relayed to the insurer (V1039). */
  public static final String TEMPLATE_MEMBER_CHANGE_RELAY = "EB_MEMBER_CHANGE_RELAY";

  /** Template of a submission of documents to an insurer (V1039). */
  public static final String TEMPLATE_SUBMISSION_COVER = "EB_SUBMISSION_COVER";

  /** Template of the SOA released to the client (V1039). */
  public static final String TEMPLATE_SOA_RELEASE = "EB_SOA_RELEASE";

  /** Alert: franchise decision overdue (V1030). */
  public static final String ALERT_FRANCHISE_OVERDUE = "EB_FRANCHISE_OVERDUE";

  /** Alert: franchise outcome not advised to the client in time (V1039). */
  public static final String ALERT_FRANCHISE_ADVICE_LATE = "EB_FRANCHISE_ADVICE_LATE";

  /** Alert: insurer request past due (V1030). */
  public static final String ALERT_PROPOSAL_OVERDUE = "EB_PROPOSAL_OVERDUE";

  /** Alert: comparative late after the last proposal (V1030). */
  public static final String ALERT_COMPARATIVE_LATE = "EB_COMPARATIVE_LATE";

  /** Alert: SOA not validated in time (V1030). */
  public static final String ALERT_SOA_VALIDATION_LATE = "EB_SOA_VALIDATION_LATE";

  /** Notification event: franchise decided (V1030). */
  public static final String EVENT_FRANCHISE_DECIDED = "EB_FRANCHISE_DECIDED";

  /** Notification event: proposal to validate (V1030). */
  public static final String EVENT_PROPOSAL_RECEIVED = "EB_PROPOSAL_RECEIVED";

  /** Notification event: comparative to sign off (V1030). */
  public static final String EVENT_COMPARATIVE_SIGNOFF = "EB_COMPARATIVE_SIGNOFF";

  /** Notification event: comparative signed off, approved or returned (V1039). */
  public static final String EVENT_COMPARATIVE_DECIDED = "EB_COMPARATIVE_DECIDED";

  /** Notification event: threshold approval waiting (V1030). */
  public static final String EVENT_THRESHOLD_APPROVAL = "EB_THRESHOLD_APPROVAL";

  /** Notification event: client confirmation recorded (V1030). */
  public static final String EVENT_CLIENT_CONFIRMED = "EB_CLIENT_CONFIRMED";

  /** Notification event: member change billed (V1030). */
  public static final String EVENT_MEMBER_CHANGE_BILLED = "EB_MEMBER_CHANGE_BILLED";

  /** Notification event: SOA released (V1030). */
  public static final String EVENT_SOA_RELEASED = "EB_SOA_RELEASED";

  /** Notification event: invoice of an SOA paid (V1030). */
  public static final String EVENT_INVOICE_PAID = "EB_INVOICE_PAID";

  /** Notification event: master list staged for review (V1039). */
  public static final String EVENT_ROSTER_STAGED = "EB_ROSTER_STAGED";

  /** Notification event: documents submitted to an insurer (V1039). */
  public static final String EVENT_SUBMISSION_SENT = "EB_SUBMISSION_SENT";

  /** Outbox purpose of the franchise requests and advices. */
  public static final String PURPOSE_FRANCHISE = "EB_FRANCHISE";

  /** Outbox purpose of the requests for proposal and revisions. */
  public static final String PURPOSE_RFP = "EB_RFP";

  /** Outbox purpose of the comparative sent to the client. */
  public static final String PURPOSE_COMPARATIVE = "EB_COMPARATIVE";

  /** Outbox purpose of the submissions to insurers. */
  public static final String PURPOSE_SUBMISSION = "EB_SUBMISSION";

  /** Outbox purpose of the member changes relayed. */
  public static final String PURPOSE_MEMBER_CHANGE = "EB_MEMBER_CHANGE";

  /** Outbox purpose of the SOA released to the client. */
  public static final String PURPOSE_SOA = "EB_SOA";

  /** Front-end route of a comparative page. */
  public static final String COMPARATIVE_LINK = "/eb/comparatives/";

  /** Front-end route of a member change. */
  public static final String MEMBER_CHANGE_LINK = "/eb/member-changes?change=";

  /** Front-end route of an SOA. */
  public static final String SOA_LINK = "/eb/soa?soa=";

  /** Permission of the EB Marketing users. */
  public static final String PERMISSION_MARKET = "EB_MARKET";

  /** Permission of the EB Processing users. */
  public static final String PERMISSION_PROCESS = "EB_PROCESS";

  /** Permission of the EB Collection users. */
  public static final String PERMISSION_COLLECT = "EB_COLLECT";

  /** Permission of the comparative signatories. */
  public static final String PERMISSION_COMPARATIVE_APPROVE = "EB_COMPARATIVE_APPROVE";

  /** Default permission of the threshold approvers. */
  public static final String PERMISSION_THRESHOLD_APPROVE = "EB_THRESHOLD_APPROVE";

  /** Permission of the EB set-up. */
  public static final String PERMISSION_SETUP = "EB_SETUP";

  private EbCodes() {}

  /**
   * The number series of a prefix and year, for {@code DocumentNumberService.next}.
   *
   * @param prefix one of the {@code PREFIX_*} constants
   * @param year year
   * @return series, e.g. {@code EBP-2026}
   */
  public static String series(String prefix, int year) {
    return prefix + "-" + year;
  }
}
