package com.iortatechnxt.brokerverse.screening.str.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * The value of one STR template field (SNSRP-705; FR-SS-070): prefilled from the case and editable
 * while the STR is a draft.
 */
@Entity
@Table(name = "scr_str_field")
public class StrField extends BaseEntity {

  /** Longest value. */
  public static final int MAX_VALUE = 8000;

  @Column(name = "str_id", nullable = false, updatable = false)
  private Long strId;

  @Column(name = "field_code", nullable = false, length = 40, updatable = false)
  private String fieldCode;

  @Column(name = "value_text", length = MAX_VALUE)
  private String valueText;

  /** For JPA. */
  protected StrField() {}

  /**
   * Creates a field value.
   *
   * @param strId the STR
   * @param fieldCode the template field code
   * @param value the value, may be null
   */
  public StrField(Long strId, String fieldCode, String value) {
    this.strId = strId;
    this.fieldCode = fieldCode;
    this.valueText = value;
  }

  /**
   * Replaces the value.
   *
   * @param value the value, may be null
   */
  public void set(String value) {
    this.valueText = value;
  }

  public Long getStrId() {
    return strId;
  }

  public String getFieldCode() {
    return fieldCode;
  }

  public String getValueText() {
    return valueText;
  }
}
