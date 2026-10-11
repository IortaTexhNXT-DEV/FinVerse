package com.iortatechnxt.brokerverse.attachment.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Makes an uploaded file part of another record's documents without copying it (BRNB.026: link one
 * file to several accounts). The file keeps its owner, checksum and audit trail. The link may carry
 * the process it was made for (BRID-025).
 */
@Entity
@Table(name = "doc_attachment_link")
public class AttachmentLink extends BaseEntity {

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  @Column(name = "entity_type", nullable = false, length = 60, updatable = false)
  private String entityType;

  @Column(name = "entity_id", nullable = false, length = 60, updatable = false)
  private String entityId;

  @Column(name = "process_tag", length = 40, updatable = false)
  private String processTag;

  protected AttachmentLink() {}

  /**
   * Links a file to a record.
   *
   * @param attachmentId file
   * @param target record
   */
  public AttachmentLink(Long attachmentId, AttachmentTarget target) {
    this(attachmentId, target, null);
  }

  /**
   * Links a file to a record in the context of a process (BRID-025 process tag).
   *
   * @param attachmentId file
   * @param target record
   * @param processTag process code of the owning module, null when none
   */
  public AttachmentLink(Long attachmentId, AttachmentTarget target, String processTag) {
    this.attachmentId = attachmentId;
    this.entityType = target.entityType();
    this.entityId = target.entityId();
    this.processTag = processTag;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getProcessTag() {
    return processTag;
  }
}
