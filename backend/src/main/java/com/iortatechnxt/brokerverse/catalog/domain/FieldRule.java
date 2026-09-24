package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One row of the minimum-field matrix (BRNB.002/003/093): a field of the account or of every risk
 * item that is mandatory for every product, a product line or one product. A field key made of
 * alternatives separated by {@code |} is satisfied by any one of them (plate number or conduction
 * sticker). A product row overrides a line or global row with the same target and key.
 */
@Entity
@Table(name = "cat_field_rule")
public class FieldRule extends AuthorizableEntity implements CatalogRecord {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private RuleScope scope;

  @Column(name = "scope_code", nullable = false, length = 30, updatable = false)
  private String scopeCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private FieldTarget target;

  @Column(name = "field_key", nullable = false, length = 80, updatable = false)
  private String fieldKey;

  @Column(nullable = false, length = 120)
  private String label;

  @Column(nullable = false)
  private boolean required;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  protected FieldRule() {}

  /**
   * Creates a rule, pending authorization.
   *
   * @param key scope, target and field key
   * @param label label shown in messages
   * @param required whether the field is mandatory (false relaxes a wider rule)
   * @param sortOrder display order
   */
  public FieldRule(RuleKey key, String label, boolean required, int sortOrder) {
    this.scope = key.scope();
    this.scopeCode = key.scopeCode();
    this.target = key.target();
    this.fieldKey = key.fieldKey();
    this.label = label;
    this.required = required;
    this.sortOrder = sortOrder;
  }

  /**
   * Changes the label, mandatory flag or order; the rule must be authorized again.
   *
   * @param newLabel label
   * @param newRequired mandatory flag
   * @param newSortOrder order
   */
  public void update(String newLabel, boolean newRequired, int newSortOrder) {
    this.label = newLabel;
    this.required = newRequired;
    this.sortOrder = newSortOrder;
    markModified();
  }

  @Override
  public String catalogReference() {
    return scope + ":" + scopeCode + ":" + target + ":" + fieldKey;
  }

  @Override
  public String catalogDescription() {
    return label + (required ? " (mandatory)" : " (optional)");
  }

  public RuleScope getScope() {
    return scope;
  }

  public String getScopeCode() {
    return scopeCode;
  }

  public FieldTarget getTarget() {
    return target;
  }

  public String getFieldKey() {
    return fieldKey;
  }

  public String getLabel() {
    return label;
  }

  public boolean isRequired() {
    return required;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  /**
   * Identity of a field rule.
   *
   * @param scope scope
   * @param scopeCode "*" for ALL, line code or product code
   * @param target account or item
   * @param fieldKey field key (alternatives separated by |)
   */
  public record RuleKey(RuleScope scope, String scopeCode, FieldTarget target, String fieldKey) {}
}
