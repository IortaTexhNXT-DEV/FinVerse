package com.iortatechnxt.brokerverse.submitted.service;

import java.util.Set;

/**
 * Codes shared by the Submitted Policies services (SUBMITTED_POLICIES_DESIGN sections 6-8): the
 * work case, the record links, the segment and source codes the code branches on, the lists of
 * values and the notification events. Business values (buckets, reasons, lead days) are data.
 */
public final class SubmittedCodes {

  /** Module code (alerts, hand-offs). */
  public static final String MODULE = "SUBMITTED";

  /** Workflow of a masterlist record. */
  public static final String WORKFLOW = "SBM_POLICY";

  /** Entity type of a masterlist record (work case, attachments, audit). */
  public static final String ENTITY = "SubmittedPolicy";

  /** Frontend route of a masterlist record (followed by its id). */
  public static final String LINK = "/submitted/policies/";

  /** CBG Motor. */
  public static final String CBG_MOTOR = "CBG_MOTOR";

  /** CBG Fire. */
  public static final String CBG_FIRE = "CBG_FIRE";

  /** Non-CBG Corporate and Branches. */
  public static final String NONCBG_CORPORATE = "NONCBG_CORPORATE";

  /** Non-CBG Retail. */
  public static final String NONCBG_RETAIL = "NONCBG_RETAIL";

  /** Segments whose records with documents enter the policy review (BRIDSP-05). */
  public static final Set<String> REVIEW_SEGMENTS = Set.of(NONCBG_CORPORATE, CBG_FIRE);

  /** Segments whose records need a PN (BRIDSP-13). */
  public static final Set<String> PN_SEGMENTS = Set.of(CBG_MOTOR, CBG_FIRE);

  /** Source of manual entries. */
  public static final String SOURCE_MANUAL = "MANUAL";

  /** Source of migrated records. */
  public static final String SOURCE_MIGRATION = "MIGRATION";

  /** Source of the policy documents of IBG / Leasing (extractions). */
  public static final String SOURCE_DOCUMENT = "IBG_LEASING_DOC";

  /** LOV of the segments. */
  public static final String LOV_SEGMENT = "SBM_SEGMENT";

  /** LOV of the buckets (parent code = renewal action). */
  public static final String LOV_BUCKET = "SBM_BUCKET";

  /** LOV of the reasons. */
  public static final String LOV_REASON = "SBM_REASON";

  /** LOV of the non-renewal reasons. */
  public static final String LOV_NON_RENEWAL = "SBM_NON_RENEWAL_REASON";

  /** LOV of the conversion statuses. */
  public static final String LOV_CONVERSION = "SBM_CONVERSION_STATUS";

  /** LOV of the loan statuses. */
  public static final String LOV_LOAN_STATUS = "SBM_LOAN_STATUS";

  /** List of the loan reports loaded as loan files, in their order of precedence. */
  public static final String LOV_LOAN_REPORT = "SBM_LOAN_REPORT";

  /** LOV of the re-assignment reasons. */
  public static final String LOV_DECLINE = "SBM_DECLINE_REASON";

  /** LOV of the policy review findings. */
  public static final String LOV_FINDING = "SBM_IAAF_FINDING";

  /** Renewable tag. */
  public static final String RENEWABLE = "RENEWABLE";

  /** Non-renewable tag. */
  public static final String NON_RENEWABLE = "NON_RENEWABLE";

  /** Document type of a submitted policy document. */
  public static final String DOC_POLICY = "SUBMITTED_POLICY";

  /** Notification: new submissions. */
  public static final String EVT_NEW_SUBMISSION = "SBM_NEW_SUBMISSION";

  /** Notification: a document waits for confirmation. */
  public static final String EVT_MANUAL_VALIDATION = "SBM_MANUAL_VALIDATION";

  /** Notification: bucket changed. */
  public static final String EVT_BUCKET_CHANGED = "SBM_BUCKET_CHANGED";

  /** Notification: fallout. */
  public static final String EVT_FALLOUT = "SBM_FALLOUT";

  /** Notification: records assigned to a handler. */
  public static final String EVT_HANDLER_ASSIGNED = "SBM_HANDLER_ASSIGNED";

  private SubmittedCodes() {}

  /**
   * The link of a masterlist record.
   *
   * @param id record id
   * @return frontend route
   */
  public static String link(Long id) {
    return LINK + id;
  }
}
