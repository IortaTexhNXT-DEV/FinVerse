package com.iortatechnxt.brokerverse.migration.mapping.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A masking rule (DATA_MIGRATION_DESIGN section 5.3): a column of a layout whose values are masked
 * before staging when the environment is not production.
 */
@Entity
@Table(name = "mig_masking_rule")
public class MaskingRule extends BaseEntity {

  /** How a value is masked. */
  public enum Kind {
    /** A full person name. */
    PERSON_NAME,
    /** A last name. */
    LAST_NAME,
    /** A first name. */
    FIRST_NAME,
    /** A corporate name. */
    CORPORATE_NAME,
    /** A street address. */
    ADDRESS,
    /** Digits keeping length and separators (TIN, ID, account, phone). */
    DIGITS,
    /** An e-mail address. */
    EMAIL,
    /** A birth date shifted by a keyed offset. */
    BIRTH_DATE,
    /** Free text replaced by a keyed token. */
    FREE_TEXT
  }

  @Column(name = "layout_code", nullable = false, length = 10)
  private String layoutCode;

  @Column(name = "column_name", nullable = false, length = 60)
  private String columnName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Kind rule;

  @Column(nullable = false)
  private boolean active = true;

  protected MaskingRule() {}

  /**
   * A new rule.
   *
   * @param layoutCode layout
   * @param columnName column
   * @param rule masking kind
   */
  public MaskingRule(String layoutCode, String columnName, Kind rule) {
    this.layoutCode = layoutCode;
    this.columnName = columnName;
    this.rule = rule;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getLayoutCode() {
    return layoutCode;
  }

  public String getColumnName() {
    return columnName;
  }

  public Kind getRule() {
    return rule;
  }

  public boolean isActive() {
    return active;
  }
}
