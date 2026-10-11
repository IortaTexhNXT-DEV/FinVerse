package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A ManCom decision on the requirements of a package request (BRPM.015, PQ07): signed off (the
 * request goes to MBS) or returned to TSU; the signed sheet is an optional attachment.
 */
@Entity
@Table(name = "pm_signoff")
public class Signoff extends BaseEntity {

  /** Decision "signed off". */
  public static final String SIGNED = "SIGNED";

  /** Decision "returned to TSU". */
  public static final String RETURNED = "RETURNED";

  private static final String MANCOM = "MANCOM";

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(nullable = false, length = 20, updatable = false)
  private String kind;

  @Column(nullable = false, length = 20, updatable = false)
  private String decision;

  @Column(nullable = false, length = 40, updatable = false)
  private String reference;

  @Column(name = "signed_by", nullable = false, length = 50, updatable = false)
  private String signedBy;

  @Column(name = "signed_at", nullable = false, updatable = false)
  private Instant signedAt;

  @Column(length = 1000, updatable = false)
  private String comment;

  @Column(name = "signed_sheet_attachment_id")
  private Long signedSheetAttachmentId;

  protected Signoff() {}

  /**
   * A ManCom decision.
   *
   * @param requestId request
   * @param decision SIGNED or RETURNED
   * @param reference sign-off reference
   * @param user ManCom member
   * @param when time
   * @param comment comment
   */
  public Signoff(
      Long requestId,
      String decision,
      String reference,
      String user,
      Instant when,
      String comment) {
    this.requestId = requestId;
    this.kind = MANCOM;
    this.decision = decision;
    this.reference = reference;
    this.signedBy = user;
    this.signedAt = when;
    this.comment = comment;
  }

  /**
   * Links the signed sheet (or the generated sign-off record).
   *
   * @param attachmentId attachment
   */
  public void attachSheet(Long attachmentId) {
    this.signedSheetAttachmentId = attachmentId;
  }

  public Long getRequestId() {
    return requestId;
  }

  public String getKind() {
    return kind;
  }

  public String getDecision() {
    return decision;
  }

  public String getReference() {
    return reference;
  }

  public String getSignedBy() {
    return signedBy;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  public String getComment() {
    return comment;
  }

  public Long getSignedSheetAttachmentId() {
    return signedSheetAttachmentId;
  }
}
