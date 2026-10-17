package com.iortatechnxt.brokerverse.messaging.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A file attached to an outbound e-mail, stored as sent (protected when requested). The content is
 * in the file store ({@code stored_file}, build step ST1); attachments queued before ST1 keep their
 * bytes in {@code content} until {@code FILE_BYTEA_MIGRATION} copies them.
 */
@Entity
@Table(name = "msg_outbound_attachment")
public class OutboundAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "message_id", nullable = false, updatable = false)
  private Long messageId;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(nullable = false, length = 64)
  private String sha256;

  @Column(name = "protected", nullable = false)
  private boolean passwordProtected;

  @Basic(fetch = FetchType.LAZY)
  @Column(name = "content")
  private byte[] content;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  protected OutboundAttachment() {}

  /**
   * Creates an attachment whose content is in the file store.
   *
   * @param messageId message
   * @param file file as sent
   * @param stored stored file of the content, its checksum and whether it was encrypted
   */
  public OutboundAttachment(Long messageId, MessageFile file, StoredContent stored) {
    this.messageId = messageId;
    this.fileName = file.fileName();
    this.mimeType = file.mimeType();
    this.sizeBytes = file.content().length;
    this.sha256 = stored.sha256();
    this.passwordProtected = stored.passwordProtected();
    this.storedFileId = stored.storedFileId();
  }

  public Long getId() {
    return id;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getMimeType() {
    return mimeType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public String getSha256() {
    return sha256;
  }

  public boolean isPasswordProtected() {
    return passwordProtected;
  }

  /**
   * File bytes of an attachment queued before ST1 (copy).
   *
   * @return content, null when the content is in the file store
   */
  public byte[] getContent() {
    return content == null ? null : content.clone();
  }

  /**
   * The stored file of the content; null for an attachment queued before ST1 and not yet copied.
   *
   * @return stored file id
   */
  public Long getStoredFileId() {
    return storedFileId;
  }

  /**
   * Where the content of an attachment is kept.
   *
   * @param storedFileId stored file
   * @param sha256 checksum of the content sent
   * @param passwordProtected whether the file was encrypted
   */
  public record StoredContent(Long storedFileId, String sha256, boolean passwordProtected) {}
}
