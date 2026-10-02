package com.iortatechnxt.brokerverse.migration.mapping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A column of a layout version: name as in the header row, type, length, mandatory (Y, N or C for
 * conditional), allowed values, the code map set of a coded column, format, example, BIBS target
 * and the validation rule, as published in the data requirements workbook.
 */
@Entity
@Table(name = "mig_layout_column")
public class LayoutColumn {

  /** Data type of a column. */
  public enum DataType {
    TEXT,
    CODE,
    DATE,
    TIMESTAMP,
    AMOUNT,
    INTEGER,
    DECIMAL,
    FLAG
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "layout_id", nullable = false, updatable = false)
  private Long layoutId;

  @Column(nullable = false)
  private int seq;

  @Column(nullable = false, length = 60)
  private String name;

  @Column(nullable = false, length = 500)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_type", nullable = false, length = 12)
  private DataType dataType;

  @Column(length = 10)
  private String length;

  @Column(nullable = false, length = 1)
  private String mandatory;

  @Column(name = "allowed_values", length = 300)
  private String allowedValues;

  @Column(name = "map_set", length = 60)
  private String mapSet;

  @Column(length = 120)
  private String format;

  @Column(length = 120)
  private String example;

  @Column(length = 200)
  private String target;

  @Column(length = 500)
  private String validation;

  @Column(name = "source_hint", length = 200)
  private String sourceHint;

  protected LayoutColumn() {}

  /**
   * Maximum length of a text or code value, null when the column has no length (dates, amounts).
   *
   * @return length
   */
  public Integer maxLength() {
    if (length == null || length.isBlank() || length.contains(",")) {
      return null;
    }
    try {
      return Integer.valueOf(length.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Allowed values when the column lists them (a closed list such as {@code I, C}).
   *
   * @return values, empty when the column is open or coded through a map set
   */
  public List<String> allowed() {
    if (allowedValues == null
        || allowedValues.isBlank()
        || mapSet != null
        || allowedValues.startsWith("-")) {
      return List.of();
    }
    return Arrays.stream(allowedValues.split(","))
        .map(v -> v.trim().toUpperCase(Locale.ROOT))
        .filter(v -> !v.isEmpty() && v.matches("[A-Z0-9_]+"))
        .toList();
  }

  public boolean isMandatory() {
    return "Y".equals(mandatory);
  }

  public Long getId() {
    return id;
  }

  public Long getLayoutId() {
    return layoutId;
  }

  public int getSeq() {
    return seq;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public DataType getDataType() {
    return dataType;
  }

  public String getLength() {
    return length;
  }

  public String getMandatory() {
    return mandatory;
  }

  public String getAllowedValues() {
    return allowedValues;
  }

  public String getMapSet() {
    return mapSet;
  }

  public String getFormat() {
    return format;
  }

  public String getExample() {
    return example;
  }

  public String getTarget() {
    return target;
  }

  public String getValidation() {
    return validation;
  }

  public String getSourceHint() {
    return sourceHint;
  }
}
