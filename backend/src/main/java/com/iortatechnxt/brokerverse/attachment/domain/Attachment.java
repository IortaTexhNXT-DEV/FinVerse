package com.iortatechnxt.brokerverse.attachment.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Metadata of a document attached to any record, identified by entity type and id (for example
 * {@code JournalBatch} / {@code 42}). The file bytes live in {@link AttachmentContent}.
 */
@Entity
@Table(name = "doc_attachment")
public class Attachment extends BaseEntity {

  @Column(name = "entity_type", nullable = false, length = 60)
  private String entityType;

  @Column(name = "entity_id", nullable = false, length = 60)
  private String entityId;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(length = 200)
  private String description;

  @Column(name = "document_type", length = 40)
  private String documentType;

  @Column(nullable = false)
  private boolean deleted;

  @Column(name = "deleted_by", length = 50)
  private String deletedBy;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected Attachment() {}

  /**
   * Creates attachment metadata.
   *
   * @param target record the file belongs to
   * @param file validated file facts
   * @param description optional description
   */
  public Attachment(AttachmentTarget target, StoredFile file, String description) {
    this.entityType = target.entityType();
    this.entityId = target.entityId();
    this.fileName = file.fileName();
    this.contentType = file.contentType();
    this.sizeBytes = file.sizeBytes();
    this.sha256 = file.sha256();
    this.description = description;
  }

  /**
   * Classifies the document (list of values DOCUMENT_TYPE, BRNB.026).
   *
   * @param type document type code, null when unclassified
   */
  public void classify(String type) {
    this.documentType = type;
  }

  /**
   * Removes the attachment logically (retained for audit).
   *
   * @param user user
   * @param when timestamp
   */
  public void markDeleted(String user, Instant when) {
    this.deleted = true;
    this.deletedBy = user;
    this.deletedAt = when;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  public String getDescription() {
    return description;
  }

  public String getDocumentType() {
    return documentType;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public String getDeletedBy() {
    return deletedBy;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
