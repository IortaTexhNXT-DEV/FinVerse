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
