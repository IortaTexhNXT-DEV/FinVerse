package com.iortatechnxt.brokerverse.collections.billing.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The rendered PDF of a statement of account (BRCLXN.058), kept apart from the statement. The
 * content is in the file store ({@code stored_file}, build step ST1); PDFs rendered before ST1 keep
 * their bytes in {@code content} until {@code FILE_BYTEA_MIGRATION} copies them.
 */
@Entity
@Table(name = "clx_billing_document")
public class BillingDocument extends BaseEntity {

  @Column(name = "statement_id", nullable = false, updatable = false)
  private Long statementId;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "content")
  private byte[] content;

  @Column(name = "stored_file_id")
  private Long storedFileId;

  protected BillingDocument() {}

  /**
   * A stored document.
   *
   * @param statementId statement
   * @param fileName file name
   * @param contentType media type
   * @param storedFileId stored file of the PDF
   */
  public BillingDocument(Long statementId, String fileName, String contentType, Long storedFileId) {
    this.statementId = statementId;
    this.fileName = fileName;
    this.contentType = contentType;
    this.storedFileId = storedFileId;
  }

  public Long getStatementId() {
    return statementId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  /**
   * The bytes of a PDF rendered before ST1 and not yet copied to the file store.
   *
   * @return bytes, null when the PDF is in the file store
   */
  public byte[] getContent() {
    return content == null ? null : content.clone();
  }

  /**
   * The stored file of the PDF; null for a PDF rendered before ST1 and not yet copied.
   *
   * @return stored file id
   */
  public Long getStoredFileId() {
    return storedFileId;
  }
}
