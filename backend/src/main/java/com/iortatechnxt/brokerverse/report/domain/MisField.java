package com.iortatechnxt.brokerverse.report.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.report.core.MisDataElement;
import com.iortatechnxt.brokerverse.report.core.MisDataElement.MisEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * An MIS field of the catalogue (FR-CR-062, V2912): the business label of a data element of a core
 * entity, offered with the same label for grouping in the Layout panel of the reports. Maintained
 * by the System Administrator under maker-checker; the changes are in the Master Data Change Log.
 */
@Entity
@Table(name = "rpt_mis_field")
public class MisField extends AuthorizableEntity {

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 80)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private MisEntity entity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40, updatable = false)
  private MisDataElement source;

  @Column(name = "lov_type", length = 60)
  private String lovType;

  protected MisField() {}

  /**
   * A new field, pending authorization.
   *
   * @param code unique code
   * @param label business label
   * @param entity core entity
   * @param source data element read
   * @param lovType list of values of the field, may be null
   */
  public MisField(
      String code, String label, MisEntity entity, MisDataElement source, String lovType) {
    this.code = code;
    this.label = label;
    this.entity = entity;
    this.source = source;
    this.lovType = lovType;
  }

  /**
   * Changes the label and list of values; the field waits for its authorization again.
   *
   * @param newLabel business label
   * @param newLovType list of values, may be null
   */
  public void change(String newLabel, String newLovType) {
    this.label = newLabel;
    this.lovType = newLovType;
    markModified();
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public MisEntity getEntity() {
    return entity;
  }

  public MisDataElement getSource() {
    return source;
  }

  public String getLovType() {
    return lovType;
  }
}
