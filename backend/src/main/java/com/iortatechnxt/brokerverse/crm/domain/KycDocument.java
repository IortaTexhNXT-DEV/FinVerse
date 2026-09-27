package com.iortatechnxt.brokerverse.crm.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A KYC document of a client: an attachment of entity type {@code Client} with its document type
 * (list of values DOCUMENT_TYPE), checked against the KYC checklist (BRNB.030/090).
 */
@Entity
@Table(name = "crm_kyc_document")
public class KycDocument extends BaseEntity {

  @Column(name = "client_id", nullable = false, updatable = false)
  private Long clientId;

  @Column(name = "document_type", nullable = false, length = 40, updatable = false)
  private String documentType;

  @Column(name = "attachment_id", nullable = false, updatable = false)
  private Long attachmentId;

  protected KycDocument() {}

  /**
   * Registers an uploaded KYC document.
   *
   * @param clientId client
   * @param documentType document type code
   * @param attachmentId stored attachment
   */
  public KycDocument(Long clientId, String documentType, Long attachmentId) {
    this.clientId = clientId;
    this.documentType = documentType;
    this.attachmentId = attachmentId;
  }

  public Long getClientId() {
    return clientId;
  }

  public String getDocumentType() {
    return documentType;
  }

  public Long getAttachmentId() {
    return attachmentId;
  }
}
