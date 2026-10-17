package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A field of a template (SNSRP-104, 501). Rows are replaced as a whole while the version is a
 * draft.
 */
@Entity
@Table(name = "scr_template_field")
public class TemplateFieldRow extends BaseEntity {

  @Column(name = "template_id", nullable = false, updatable = false)
  private Long templateId;

  @Column(name = "section", nullable = false, length = 100, updatable = false)
  private String section;

  @Column(name = "code", nullable = false, length = 40, updatable = false)
  private String code;

  @Column(name = "label", nullable = false, length = 200, updatable = false)
  private String label;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_type", nullable = false, length = 20, updatable = false)
  private FieldDataType dataType;

  @Column(name = "lov_type", length = 40, updatable = false)
  private String lovType;

  @Column(name = "mandatory", nullable = false, updatable = false)
  private boolean mandatory;

  @Column(name = "help_text", length = 500, updatable = false)
  private String helpText;

  @Column(name = "sort_order", nullable = false, updatable = false)
  private int sortOrder;

  @Column(name = "prefill_source", length = 60, updatable = false)
  private String prefillSource;

  /** For JPA. */
  protected TemplateFieldRow() {}

  /**
   * Creates a row.
   *
   * @param templateId the owning template id
   * @param section section
   * @param code code
   * @param label label
   * @param dataType data type
   * @param lovType list of values
   * @param mandatory mandatory
   * @param helpText help text
   * @param sortOrder order
   * @param prefillSource STR prefill source
   */
  @SuppressWarnings("java:S107")
  public TemplateFieldRow(
      Long templateId,
      String section,
      String code,
      String label,
      FieldDataType dataType,
      String lovType,
      boolean mandatory,
      String helpText,
      int sortOrder,
      String prefillSource) {
    this.templateId = templateId;
    this.section = section;
    this.code = code;
    this.label = label;
    this.dataType = dataType;
    this.lovType = lovType;
    this.mandatory = mandatory;
    this.helpText = helpText;
    this.sortOrder = sortOrder;
    this.prefillSource = prefillSource;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public String getSection() {
    return section;
  }

  public String getCode() {
    return code;
  }

  public String getLabel() {
    return label;
  }

  public FieldDataType getDataType() {
    return dataType;
  }

  public String getLovType() {
    return lovType;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public String getHelpText() {
    return helpText;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public String getPrefillSource() {
    return prefillSource;
  }
}
