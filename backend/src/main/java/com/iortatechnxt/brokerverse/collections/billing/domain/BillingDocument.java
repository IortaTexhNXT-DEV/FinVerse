package com.iortatechnxt.brokerverse.collections.billing.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** The rendered PDF of a statement of account (BRCLXN.058), kept apart from the statement. */
@Entity
@Table(name = "clx_billing_document")
public class BillingDocument extends BaseEntity {

  @Column(name = "statement_id", nullable = false, updatable = false)
  private Long statementId;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(nullable = false)
  private byte[] content;

  protected BillingDocument() {}

  /**
   * A stored document.
   *
   * @param statementId statement
   * @param fileName file name
   * @param contentType media type
   * @param content bytes
   */
  public BillingDocument(Long statementId, String fileName, String contentType, byte[] content) {
    this.statementId = statementId;
    this.fileName = fileName;
    this.contentType = contentType;
    this.content = content.clone();
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

  public byte[] getContent() {
    return content.clone();
  }
}
