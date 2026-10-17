package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One field change of a masterlist record with its source (BRIDSP-29). The user and time are the
 * audit columns.
 */
@Entity
@Table(name = "sbm_policy_history")
public class SbmPolicyHistory extends BaseEntity {

  private static final int MAX = 1000;

  @Column(name = "policy_id", nullable = false, updatable = false)
  private Long policyId;

  @Column(nullable = false, updatable = false, length = 40)
  private String field;

  @Column(name = "old_value", updatable = false, length = MAX)
  private String oldValue;

  @Column(name = "new_value", updatable = false, length = MAX)
  private String newValue;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false, length = 20)
  private SbmHistorySource source;

  @Column(updatable = false, length = 40)
  private String reference;

  protected SbmPolicyHistory() {}

  /**
   * A change.
   *
   * @param policyId record
   * @param field field name
   * @param change old and new value
   * @param source what changed it
   * @param reference run, intake, extraction or document reference, may be null
   */
  public SbmPolicyHistory(
      Long policyId, String field, Change change, SbmHistorySource source, String reference) {
    this.policyId = policyId;
    this.field = field;
    this.oldValue = cut(change.oldValue());
    this.newValue = cut(change.newValue());
    this.source = source;
    this.reference = reference;
  }

  private static String cut(String value) {
    return value == null || value.length() <= MAX ? value : value.substring(0, MAX);
  }

  public Long getPolicyId() {
    return policyId;
  }

  public String getField() {
    return field;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public SbmHistorySource getSource() {
    return source;
  }

  public String getReference() {
    return reference;
  }

  /**
   * Old and new value of a field.
   *
   * @param oldValue before
   * @param newValue after
   */
  public record Change(String oldValue, String newValue) {}
}
