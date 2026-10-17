package com.iortatechnxt.brokerverse.csf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One field of a contact change: the field and its value before and after (for a refused or
 * referred change, the value the client asked for).
 */
@Embeddable
public class ChangedField {

  @Column(nullable = false, length = 40)
  private String field;

  @Column(name = "old_value", length = 300)
  private String oldValue;

  @Column(name = "new_value", length = 300)
  private String newValue;

  protected ChangedField() {}

  /**
   * A changed field.
   *
   * @param field field code (contact field or list CSF_REFERRAL_FIELD)
   * @param oldValue value before, may be null
   * @param newValue value after or asked for, may be null
   */
  public ChangedField(String field, String oldValue, String newValue) {
    this.field = field;
    this.oldValue = oldValue;
    this.newValue = newValue;
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
}
