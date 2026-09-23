package com.iortatechnxt.finverse.system.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Editable business parameter (configuration repository). Parameters are seeded by migrations;
 * administrators change values only, never keys or types.
 */
@Entity
@Table(name = "sys_parameter")
public class SystemParameter extends BaseEntity {

  private static final int MAX_VALUE_LENGTH = 1000;

  @Column(name = "param_key", nullable = false, length = 60, updatable = false)
  private String key;

  @Column(name = "param_value", nullable = false, length = MAX_VALUE_LENGTH)
  private String value;

  @Enumerated(EnumType.STRING)
  @Column(name = "value_type", nullable = false, length = 15, updatable = false)
  private ParameterValueType valueType;

  @Column(nullable = false, length = 30)
  private String category;

  @Column(nullable = false, length = 300)
  private String description;

  @Column(name = "min_value")
  private Integer minValue;

  @Column(name = "max_value")
  private Integer maxValue;

  protected SystemParameter() {}

  /**
   * Changes the value after validating it against the parameter type.
   *
   * @param newValue new value
   */
  public void changeValue(String newValue) {
    String candidate = newValue == null ? "" : newValue.trim();
    if (candidate.length() > MAX_VALUE_LENGTH) {
      throw new BusinessRuleException("INVALID_PARAMETER_VALUE", key + " is too long");
    }
    valueType
        .validate(candidate, minValue, maxValue)
        .ifPresent(
            error -> {
              throw new BusinessRuleException("INVALID_PARAMETER_VALUE", key + " " + error);
            });
    this.value = candidate;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public ParameterValueType getValueType() {
    return valueType;
  }

  public String getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public Integer getMinValue() {
    return minValue;
  }

  public Integer getMaxValue() {
    return maxValue;
  }
}
