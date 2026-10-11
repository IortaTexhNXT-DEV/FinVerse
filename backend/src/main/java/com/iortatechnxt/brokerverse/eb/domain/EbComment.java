package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A comment on a comparative (FR-EB-043): an internal note or reply, or the client's comment
 * received by e-mail and entered by the AO.
 */
@Entity
@Table(name = "eb_comment")
public class EbComment extends BaseEntity {

  /** Written by a BDOI user. */
  public static final String INTERNAL = "INTERNAL";

  /** The client's comment, entered by the AO. */
  public static final String CLIENT = "CLIENT";

  @Column(name = "comparative_id", nullable = false, updatable = false)
  private Long comparativeId;

  @Column(name = "author_kind", nullable = false, length = 10, updatable = false)
  private String authorKind;

  @Column(name = "comment_text", nullable = false, length = 4000, updatable = false)
  private String text;

  @Column(name = "reply_to_id", updatable = false)
  private Long replyToId;

  protected EbComment() {}

  /**
   * Creates a comment.
   *
   * @param comparativeId comparative
   * @param authorKind INTERNAL or CLIENT
   * @param text text
   * @param replyToId comment answered, may be null
   */
  public EbComment(Long comparativeId, String authorKind, String text, Long replyToId) {
    this.comparativeId = comparativeId;
    this.authorKind = authorKind;
    this.text = text;
    this.replyToId = replyToId;
  }

  public Long getComparativeId() {
    return comparativeId;
  }

  public String getAuthorKind() {
    return authorKind;
  }

  public String getText() {
    return text;
  }

  public Long getReplyToId() {
    return replyToId;
  }
}
