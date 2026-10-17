package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A column of an STR layout (SNSRP-105, 706). Rows are replaced as a whole while the version is a
 * draft.
 */
@Entity
@Table(name = "scr_str_layout_column")
public class StrLayoutColumnRow extends BaseEntity {

  @Column(name = "layout_id", nullable = false, updatable = false)
  private Long layoutId;

  @Column(name = "sort_order", nullable = false, updatable = false)
  private int sortOrder;

  @Column(name = "field_code", length = 40, updatable = false)
  private String fieldCode;

  @Column(name = "fixed_value", length = 200, updatable = false)
  private String fixedValue;

  @Column(name = "header", length = 100, updatable = false)
  private String header;

  @Column(name = "length", updatable = false)
  private Integer length;

  @Column(name = "pad", length = 10, updatable = false)
  private String pad;

  @Column(name = "code_map", nullable = false, length = 1000, updatable = false)
  private String codeMap;

  /** For JPA. */
  protected StrLayoutColumnRow() {}

  /**
   * Creates a row.
   *
   * @param layoutId the owning layout id
   * @param sortOrder position
   * @param fieldCode STR field
   * @param fixedValue fixed value
   * @param header header
   * @param length width
   * @param pad padding
   * @param codeMap code mapping CODE=AMLC;...
   */
  @SuppressWarnings("java:S107")
  public StrLayoutColumnRow(
      Long layoutId,
      int sortOrder,
      String fieldCode,
      String fixedValue,
      String header,
      Integer length,
      String pad,
      String codeMap) {
    this.layoutId = layoutId;
    this.sortOrder = sortOrder;
    this.fieldCode = fieldCode;
    this.fixedValue = fixedValue;
    this.header = header;
    this.length = length;
    this.pad = pad;
    this.codeMap = codeMap;
  }

  public Long getLayoutId() {
    return layoutId;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getFieldCode() {
    return fieldCode;
  }

  public String getFixedValue() {
    return fixedValue;
  }

  public String getHeader() {
    return header;
  }

  public Integer getLength() {
    return length;
  }

  public String getPad() {
    return pad;
  }

  public String getCodeMap() {
    return codeMap;
  }
}
