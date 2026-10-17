package com.iortatechnxt.brokerverse.payrequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * Who moved a request and when (MKT 1.14.0-1.16.2, 2.26.0), used for the four-eyes checks; and the
 * last return reason (MKT 1.8.0).
 *
 * @param submittedBy submitter
 * @param submittedAt submitted at
 * @param reviewedBy reviewer who endorsed it
 * @param reviewedAt endorsed at
 * @param approvedBy Marketing approver
 * @param approvedAt approved at
 * @param hrApprovedBy HR approver (cash advances)
 * @param hrApprovedAt HR approved at
 * @param returnReason last return reason
 * @param returnComment last return comment
 */
@Embeddable
public record RequestTrail(
    @Column(name = "submitted_by", length = 50) String submittedBy,
    @Column(name = "submitted_at") Instant submittedAt,
    @Column(name = "reviewed_by", length = 50) String reviewedBy,
    @Column(name = "reviewed_at") Instant reviewedAt,
    @Column(name = "approved_by", length = 50) String approvedBy,
    @Column(name = "approved_at") Instant approvedAt,
    @Column(name = "hr_approved_by", length = 50) String hrApprovedBy,
    @Column(name = "hr_approved_at") Instant hrApprovedAt,
    @Column(name = "return_reason", length = 40) String returnReason,
    @Column(name = "return_comment", length = 500) String returnComment) {

  /** Nothing done yet. */
  public static final RequestTrail NONE =
      new RequestTrail(null, null, null, null, null, null, null, null, null, null);

  /**
   * After a submission.
   *
   * @param user submitter
   * @param at time
   * @return new trail
   */
  public RequestTrail submitted(String user, Instant at) {
    return new RequestTrail(
        user,
        at,
        reviewedBy,
        reviewedAt,
        approvedBy,
        approvedAt,
        hrApprovedBy,
        hrApprovedAt,
        returnReason,
        returnComment);
  }

  /**
   * After the review.
   *
   * @param user reviewer
   * @param at time
   * @return new trail
   */
  public RequestTrail reviewed(String user, Instant at) {
    return new RequestTrail(
        submittedBy,
        submittedAt,
        user,
        at,
        approvedBy,
        approvedAt,
        hrApprovedBy,
        hrApprovedAt,
        returnReason,
        returnComment);
  }

  /**
   * After the Marketing approval.
   *
   * @param user approver
   * @param at time
   * @return new trail
   */
  public RequestTrail approved(String user, Instant at) {
    return new RequestTrail(
        submittedBy,
        submittedAt,
        reviewedBy,
        reviewedAt,
        user,
        at,
        hrApprovedBy,
        hrApprovedAt,
        returnReason,
        returnComment);
  }

  /**
   * After the HR approval.
   *
   * @param user HR approver
   * @param at time
   * @return new trail
   */
  public RequestTrail hrApproved(String user, Instant at) {
    return new RequestTrail(
        submittedBy,
        submittedAt,
        reviewedBy,
        reviewedAt,
        approvedBy,
        approvedAt,
        user,
        at,
        returnReason,
        returnComment);
  }

  /**
   * After a return (MKT 1.8.0).
   *
   * @param reason reason code
   * @param comment comment
   * @return new trail
   */
  public RequestTrail returned(String reason, String comment) {
    return new RequestTrail(
        submittedBy,
        submittedAt,
        reviewedBy,
        reviewedAt,
        approvedBy,
        approvedAt,
        hrApprovedBy,
        hrApprovedAt,
        reason,
        comment);
  }
}
