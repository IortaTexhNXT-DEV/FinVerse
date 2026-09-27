package com.iortatechnxt.brokerverse.system.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Editable business parameter (configuration repository). Parameters are seeded by migrations;
 * administrators change values only, never keys or types.
 */
@Entity
@Table(name = "sys_parameter")
public class SystemParameter extends BaseEntity {

  /** Category of the parameters whose change needs a second approval. */
  public static final String SECURITY = "SECURITY";

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

  @Column(name = "pending_value", length = MAX_VALUE_LENGTH)
  private String pendingValue;

  @Column(name = "pending_by", length = 50)
  private String pendingBy;

  @Column(name = "pending_at")
  private Instant pendingAt;

  protected SystemParameter() {}

  /**
   * Changes the value after validating it against the type and bounds.
   *
   * @param newValue new value
   */
  public void changeValue(String newValue) {
    this.value = validated(newValue);
  }

  /**
   * Whether a change of the parameter needs a second approval (the SECURITY category: sign-in,
   * password, session and access settings, V1065).
   *
   * @return true for a security parameter
   */
  public boolean isSecurity() {
    return SECURITY.equals(category);
  }

  /**
   * Keeps a validated change that waits for the approval of another user.
   *
   * @param newValue new value
   * @param by user who asks for the change
   * @param when time
   */
  public void requestChange(String newValue, String by, Instant when) {
    if (pendingValue != null) {
      throw new BusinessRuleException(
          "PARAMETER_CHANGE_PENDING",
          "A change of " + key + " already waits for approval; approve or reject it first");
    }
    this.pendingValue = validated(newValue);
    this.pendingBy = by;
    this.pendingAt = when;
  }

  /**
   * Takes the change that waits, approved by another user than the one who asked for it; the caller
   * applies the returned value.
   *
   * @param approver approving user
   * @return the approved value
   */
  public String approveChange(String approver) {
    requirePending();
    if (Objects.equals(pendingBy, approver)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A change cannot be approved by the user who asked for it");
    }
    String approved = pendingValue;
    clearPending();
    return approved;
  }

  /** Drops the change that waits. */
  public void rejectChange() {
    requirePending();
    clearPending();
  }

  private void requirePending() {
    if (pendingValue == null) {
      throw new BusinessRuleException(
          "PARAMETER_NO_PENDING_CHANGE", "No change of " + key + " waits for approval");
    }
  }

  private void clearPending() {
    this.pendingValue = null;
    this.pendingBy = null;
    this.pendingAt = null;
  }

  private String validated(String newValue) {
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
    return candidate;
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

  public String getPendingValue() {
    return pendingValue;
  }

  public String getPendingBy() {
    return pendingBy;
  }

  public Instant getPendingAt() {
    return pendingAt;
  }
}
