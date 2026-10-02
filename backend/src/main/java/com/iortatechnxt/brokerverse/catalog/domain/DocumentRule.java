package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Mandatory document (DOCUMENT_TYPE list of values) of every product, a line or one product,
 * checked when Marketing submits an account (BRNB.026/051, e.g. the IDF for motor).
 */
@Entity
@Table(name = "cat_document_rule")
public class DocumentRule extends AuthorizableEntity implements CatalogRecord {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private RuleScope scope;

  @Column(name = "scope_code", nullable = false, length = 30, updatable = false)
  private String scopeCode;

  @Column(name = "document_type", nullable = false, length = 40, updatable = false)
  private String documentType;

  @Column(nullable = false)
  private boolean required;

  protected DocumentRule() {}

  /**
   * Creates a rule, pending authorization.
   *
   * @param scope scope
   * @param scopeCode "*" for ALL, line code or product code
   * @param documentType document type code
   * @param required whether the document is mandatory (false relaxes a wider rule)
   */
  public DocumentRule(RuleScope scope, String scopeCode, String documentType, boolean required) {
    this.scope = scope;
    this.scopeCode = scopeCode;
    this.documentType = documentType;
    this.required = required;
  }

  /**
   * Changes the mandatory flag; the rule must be authorized again.
   *
   * @param newRequired mandatory flag
   */
  public void update(boolean newRequired) {
    this.required = newRequired;
    markModified();
  }

  @Override
  public String catalogReference() {
    return scope + ":" + scopeCode + ":" + documentType;
  }

  @Override
  public String catalogDescription() {
    return documentType + (required ? " mandatory" : " optional");
  }

  public RuleScope getScope() {
    return scope;
  }

  public String getScopeCode() {
    return scopeCode;
  }

  public String getDocumentType() {
    return documentType;
  }

  public boolean isRequired() {
    return required;
  }
}
