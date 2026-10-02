package com.iortatechnxt.brokerverse.screening.cases.domain;

/**
 * The events of the insert-only case timeline (SNSRP-401, 404, 405, 601, 701-706, 903; design 4.4).
 * The design's list is completed with the match, review, information request, committee decision,
 * STR and document reminder events of the case wave.
 */
public enum CaseEventType {
  /** The case was opened. */
  CREATED,
  /** Assigned by the assignment matrix or on stage entry. */
  ASSIGNED,
  /** Re-assigned with a reason (SNSRP-404). */
  REASSIGNED,
  /** A match joined the open case (FR-SS-034 alternate flow). */
  MATCH_ADDED,
  /** A match of the case was confirmed or cleared. */
  MATCH_DECIDED,
  /** The review was saved as a draft. */
  REVIEW_SAVED,
  /** The investigator asked for more information (disposition NEED_MORE_INFO). */
  INFO_REQUESTED,
  /** Submitted for approval. */
  SUBMITTED,
  /** Validation passed (SNSRP-701). */
  VALIDATED,
  /** Validation failed (SNSRP-701). */
  VALIDATION_FAILED,
  /** Approved by the unit head. */
  APPROVED,
  /** Disapproved by the unit head (returned). */
  DISAPPROVED,
  /** Returned for rework by Compliance. */
  RETURNED,
  /** Resubmitted after a return. */
  RESUBMITTED,
  /** Escalated (to the AML Committee or STR preparation). */
  ESCALATED,
  /** A committee member voted. */
  COMMITTEE_VOTE,
  /** The committee rule finalised the decision. */
  COMMITTEE_DECISION,
  /** The STR was prepared from the case. */
  STR_PREPARED,
  /** The STR was marked ready. */
  STR_READY,
  /** The STR was extracted. */
  STR_EXTRACTED,
  /** The AMLC filing was recorded. */
  FILED,
  /** The case was closed. */
  CLOSED,
  /** The case was re-opened. */
  REOPENED,
  /** SLA reminder sent. */
  REMINDER,
  /** Missing-document reminder sent (SNSRP-802). */
  DOCUMENT_REMINDER,
  /** SLA breached and escalated. */
  BREACH,
  /** A document was uploaded. */
  DOCUMENT_ADDED,
  /** The client's risk tag was changed by hand (SNSRP-304). */
  RISK_TAG_CHANGED
}
