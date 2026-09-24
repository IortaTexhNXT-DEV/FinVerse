package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.DocumentKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A document stored on a batch at submission (RMTID.011): the remittance schedule (PDF and Excel)
 * and the payment request, with the template version used (BRNB.004 convention).
 */
@Entity
@Table(name = "rem_batch_document")
public class BatchDocument extends BaseEntity {

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30, updatable = false)
  private DocumentKind kind;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(nullable = false)
  private byte[] content;

  @Column(name = "template_version", length = 60)
  private String templateVersion;

  protected BatchDocument() {}

  /**
   * A stored document.
   *
   * @param batchId batch
   * @param kind kind
   * @param file file name, type, content and template version
   */
  public BatchDocument(Long batchId, DocumentKind kind, StoredFile file) {
    this.batchId = batchId;
    this.kind = kind;
    apply(file);
  }

  /**
   * Replaces the content (re-submission after a send back).
   *
   * @param file new file
   */
  public void replace(StoredFile file) {
    apply(file);
  }

  private void apply(StoredFile file) {
    this.fileName = file.fileName();
    this.contentType = file.contentType();
    this.content = file.content();
    this.templateVersion = file.templateVersion();
  }

  public Long getBatchId() {
    return batchId;
  }

  public DocumentKind getKind() {
    return kind;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  /**
   * The file.
   *
   * @return bytes
   */
  public byte[] getContent() {
    return content.clone();
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  /**
   * A rendered file.
   *
   * @param fileName file name
   * @param contentType MIME type
   * @param content bytes
   * @param templateVersion template and version used, may be null
   */
  public record StoredFile(
      String fileName, String contentType, byte[] content, String templateVersion) {

    /** Defensive copy. */
    public StoredFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof StoredFile f
          && fileName.equals(f.fileName)
          && java.util.Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return 31 * fileName.hashCode() + java.util.Arrays.hashCode(content);
    }

    @Override
    public String toString() {
      return "StoredFile[" + fileName + ", " + content.length + " bytes]";
    }
  }
}
