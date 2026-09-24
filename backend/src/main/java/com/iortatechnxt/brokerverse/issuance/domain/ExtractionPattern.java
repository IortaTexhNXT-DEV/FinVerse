package com.iortatechnxt.brokerverse.issuance.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A regular expression that reads one field of an insurer's e-policy text (BRNB.104); the first
 * capture group is the value. A pattern without insurer is the default for every insurer.
 */
@Entity
@Table(name = "iss_extraction_pattern")
public class ExtractionPattern extends BaseEntity {

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ExtractionField field;

  @Column(nullable = false, length = 500)
  private String pattern;

  @Column(name = "date_format", length = 40)
  private String dateFormat;

  @Column(nullable = false)
  private int priority;

  @Column(nullable = false)
  private boolean active;

  protected ExtractionPattern() {}

  public String getInsurerCode() {
    return insurerCode;
  }

  public ExtractionField getField() {
    return field;
  }

  public String getPattern() {
    return pattern;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public int getPriority() {
    return priority;
  }

  public boolean isActive() {
    return active;
  }
}
