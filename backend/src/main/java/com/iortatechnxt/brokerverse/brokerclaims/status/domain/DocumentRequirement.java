package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A document of the standard claim checklist (FRS BRD-07 FRCLM.006.01): the document type ({@code
 * DOCUMENT_TYPE}) required or optional for a claim type, maintained on Claims Setup.
 */
@Entity
@Table(name = "bcl_document_requirement")
public class DocumentRequirement extends BaseEntity {

  @Column(name = "claim_type", nullable = false, length = 40, updatable = false)
  private String claimType;

  @Column(name = "document_type", nullable = false, length = 40, updatable = false)
  private String documentType;

  @Column(nullable = false)
  private boolean required;

  @Column(nullable = false)
  private boolean active;

  protected DocumentRequirement() {}

  /**
   * A requirement.
   *
   * @param claimType claim type
   * @param documentType document type
   * @param required whether the document must be uploaded
   */
  public DocumentRequirement(String claimType, String documentType, boolean required) {
    this.claimType = claimType;
    this.documentType = documentType;
    this.required = required;
    this.active = true;
  }

  /**
   * Changes the requirement.
   *
   * @param isRequired whether the document must be uploaded
   * @param isActive whether the requirement applies
   */
  public void update(boolean isRequired, boolean isActive) {
    this.required = isRequired;
    this.active = isActive;
  }

  public String getClaimType() {
    return claimType;
  }

  public String getDocumentType() {
    return documentType;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isActive() {
    return active;
  }
}
