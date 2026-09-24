package com.iortatechnxt.brokerverse.attachment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** File bytes of an {@link Attachment}, stored separately so listings never load them. */
@Entity
@Table(name = "doc_attachment_content")
public class AttachmentContent {

  @Id
  @Column(name = "attachment_id")
  private Long attachmentId;

  @Column(nullable = false)
  private byte[] content;

  protected AttachmentContent() {}

  /**
   * Creates the content row.
   *
   * @param attachmentId owning attachment
   * @param content file bytes
   */
  public AttachmentContent(Long attachmentId, byte[] content) {
    this.attachmentId = attachmentId;
    this.content = content.clone();
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  /**
   * Returns a copy of the file bytes.
   *
   * @return bytes
   */
  public byte[] getContent() {
    return content.clone();
  }
}
