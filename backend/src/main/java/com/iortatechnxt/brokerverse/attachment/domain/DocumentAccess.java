package com.iortatechnxt.brokerverse.attachment.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Access class row of a document type (BRID-025; cross-BRD work item P3, V1031): a permission that
 * may list and download documents of the type. A type without rows is open to every holder of
 * {@code ATTACHMENT_VIEW}, as before.
 */
@Entity
@Table(name = "att_document_access")
public class DocumentAccess extends BaseEntity {

  @Column(name = "document_type", nullable = false, length = 40, updatable = false)
  private String documentType;

  @Column(nullable = false, length = 50, updatable = false)
  private String permission;

  @Column(name = "access_class", nullable = false, length = 20)
  private String accessClass;

  protected DocumentAccess() {}

  /**
   * Creates a row.
   *
   * @param documentType document type (list DOCUMENT_TYPE)
   * @param permission permission that may see the type
   * @param accessClass department of the row (MARKETING, PROCESSING, COLLECTION, ...)
   */
  public DocumentAccess(String documentType, String permission, String accessClass) {
    this.documentType = documentType;
    this.permission = permission;
    this.accessClass = accessClass;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getPermission() {
    return permission;
  }

  public String getAccessClass() {
    return accessClass;
  }
}
