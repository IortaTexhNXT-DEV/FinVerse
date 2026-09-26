package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.SafePattern;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Optional;

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

  @Enumerated(EnumType.STRING)
  @Column(name = "rule_type", nullable = false, length = 10)
  private FieldRuleType ruleType = FieldRuleType.REQUIRED;

  @Column(name = "lov_type", length = 40)
  private String lovType;

  @Column(name = "min_value", precision = 19, scale = 2)
  private BigDecimal minValue;

  @Column(name = "max_value", precision = 19, scale = 2)
  private BigDecimal maxValue;

  @Column(length = 200)
  private String pattern;

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

  /**
   * Sets what the rule checks besides presence (BRPM.004); it must be authorized again.
   *
   * @param check rule type and parameters
   */
  public void check(Check check) {
    Check c = check == null ? Check.PRESENCE : check;
    if (!c.complete()) {
      throw new BusinessRuleException(
          "FIELD_RULE_INCOMPLETE", "Complete the parameters of the " + c.type() + " rule");
    }
    this.ruleType = c.type();
    this.lovType = c.type() == FieldRuleType.LOV ? c.lovType() : null;
    this.minValue = c.type() == FieldRuleType.RANGE ? c.min() : null;
    this.maxValue = c.type() == FieldRuleType.RANGE ? c.max() : null;
    this.pattern = c.type() == FieldRuleType.PATTERN ? c.pattern() : null;
    markModified();
  }

  /**
   * The RANGE or PATTERN violation of a given value (LOV rules are checked by the service).
   *
   * @param value value as text, not blank
   * @return message, empty when the value is valid
   */
  public Optional<String> formatViolation(String value) {
    if (ruleType == FieldRuleType.PATTERN && !SafePattern.matches(pattern, value)) {
      return Optional.of(label + " has an invalid format");
    }
    if (ruleType == FieldRuleType.RANGE) {
      BigDecimal number;
      try {
        number = new BigDecimal(value.strip());
      } catch (NumberFormatException e) {
        return Optional.of(label + " must be a number");
      }
      if (minValue != null && number.compareTo(minValue) < 0
          || maxValue != null && number.compareTo(maxValue) > 0) {
        return Optional.of(
            label + " must be between " + bound(minValue) + " and " + bound(maxValue));
      }
    }
    return Optional.empty();
  }

  private static String bound(BigDecimal value) {
    return value == null ? "any" : value.stripTrailingZeros().toPlainString();
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

  public FieldRuleType getRuleType() {
    return ruleType;
  }

  public String getLovType() {
    return lovType;
  }

  public BigDecimal getMinValue() {
    return minValue;
  }

  public BigDecimal getMaxValue() {
    return maxValue;
  }

  public String getPattern() {
    return pattern;
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

  /**
   * What a rule checks (BRPM.004).
   *
   * @param type REQUIRED, LOV, RANGE or PATTERN
   * @param lovType list of values (LOV)
   * @param min minimum (RANGE), null for none
   * @param max maximum (RANGE), null for none
   * @param pattern regular expression (PATTERN)
   */
  public record Check(
      FieldRuleType type, String lovType, BigDecimal min, BigDecimal max, String pattern) {

    /** Presence only (the minimum-field matrix). */
    public static final Check PRESENCE = new Check(FieldRuleType.REQUIRED, null, null, null, null);

    /** A null type means presence only. */
    public Check {
      type = type == null ? FieldRuleType.REQUIRED : type;
    }

    /**
     * Whether the parameters the type needs are given (and the pattern compiles).
     *
     * @return true when complete
     */
    public boolean complete() {
      if (min != null && max != null && max.compareTo(min) < 0) {
        return false;
      }
      return switch (type) {
        case REQUIRED -> true;
        case LOV -> lovType != null && !lovType.isBlank();
        case RANGE -> min != null || max != null;
        case PATTERN -> SafePattern.isValid(pattern);
      };
    }
  }
}
