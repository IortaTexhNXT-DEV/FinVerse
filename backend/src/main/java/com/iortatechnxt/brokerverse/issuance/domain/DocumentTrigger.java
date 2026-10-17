package com.iortatechnxt.brokerverse.issuance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A document trigger rule (BRNB.105): uploading a document of a type starts a business action, e.g.
 * an e-policy opens an extraction review task. The automatic booking that follows the policy issue
 * belongs to the booking module, which listens for the POLICY_ISSUED stage.
 */
@Entity
@Table(name = "iss_document_trigger")
public class DocumentTrigger extends BaseEntity {

  @Column(name = "document_type", nullable = false, length = 40)
  private String documentType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TriggerAction action;

  @Column(nullable = false, length = 200)
  private String description;

  @Column(nullable = false)
  private boolean active;

  protected DocumentTrigger() {}

  public String getDocumentType() {
    return documentType;
  }

  public TriggerAction getAction() {
    return action;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }
}
