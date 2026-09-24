package com.iortatechnxt.brokerverse.messaging.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A file attached to an outbound e-mail, stored as sent (protected when requested). */
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
  @Column(nullable = false)
  private byte[] content;

  protected OutboundAttachment() {}

  /**
   * Creates an attachment.
   *
   * @param messageId message
   * @param file file as sent
   * @param sha256 checksum of the content sent
   * @param passwordProtected whether the file was encrypted
   */
  public OutboundAttachment(
      Long messageId, MessageFile file, String sha256, boolean passwordProtected) {
    this.messageId = messageId;
    this.fileName = file.fileName();
    this.mimeType = file.mimeType();
    this.content = file.content().clone();
    this.sizeBytes = file.content().length;
    this.sha256 = sha256;
    this.passwordProtected = passwordProtected;
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
   * File bytes (copy).
   *
   * @return content
   */
  public byte[] getContent() {
    return content.clone();
  }
}
