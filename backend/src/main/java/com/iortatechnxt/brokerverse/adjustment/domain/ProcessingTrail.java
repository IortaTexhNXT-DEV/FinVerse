package com.iortatechnxt.brokerverse.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/**
 * Who moved an endorsement request and when (ADJID.021 aging, ADJID.022 history, four eyes).
 *
 * @param submittedBy requester who submitted it
 * @param submittedAt submission time (start of the aging)
 * @param validatedBy Adjustment processor who validated it
 * @param validatedAt validation time
 * @param approvedBy team leader who approved it
 * @param approvedAt approval time
 * @param postedBy user who posted it
 * @param postedAt posting time
 * @param completedAt time the request was completed or cancelled (end of the aging)
 */
@Embeddable
public record ProcessingTrail(
    @Column(name = "submitted_by", length = 50) String submittedBy,
    @Column(name = "submitted_at") Instant submittedAt,
    @Column(name = "validated_by", length = 50) String validatedBy,
    @Column(name = "validated_at") Instant validatedAt,
    @Column(name = "approved_by", length = 50) String approvedBy,
    @Column(name = "approved_at") Instant approvedAt,
    @Column(name = "posted_by", length = 50) String postedBy,
    @Column(name = "posted_at") Instant postedAt,
    @Column(name = "completed_at") Instant completedAt) {

  /** Nothing done yet. */
  public static final ProcessingTrail NONE =
      new ProcessingTrail(null, null, null, null, null, null, null, null, null);

  ProcessingTrail submitted(String by, Instant at) {
    return new ProcessingTrail(by, at, null, null, null, null, null, null, null);
  }

  ProcessingTrail validated(String by, Instant at) {
    return new ProcessingTrail(submittedBy, submittedAt, by, at, null, null, null, null, null);
  }

  ProcessingTrail approved(String by, Instant at) {
    return new ProcessingTrail(
        submittedBy, submittedAt, validatedBy, validatedAt, by, at, null, null, null);
  }

  ProcessingTrail posted(String by, Instant at, boolean complete) {
    return new ProcessingTrail(
        submittedBy,
        submittedAt,
        validatedBy,
        validatedAt,
        approvedBy,
        approvedAt,
        by,
        at,
        complete ? at : null);
  }

  ProcessingTrail completed(Instant at) {
    return new ProcessingTrail(
        submittedBy,
        submittedAt,
        validatedBy,
        validatedAt,
        approvedBy,
        approvedAt,
        postedBy,
        postedAt,
        at);
  }
}
