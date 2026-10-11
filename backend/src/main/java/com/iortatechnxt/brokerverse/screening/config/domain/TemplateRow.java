package com.iortatechnxt.brokerverse.screening.config.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * The template of a TEMPLATE version (SNSRP-104, 105). Rows are replaced as a whole while the
 * version is a draft.
 */
@Entity
@Table(name = "scr_template")
public class TemplateRow extends BaseEntity {

  @Column(name = "version_id", nullable = false, updatable = false)
  private Long versionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "template_type", nullable = false, length = 30, updatable = false)
  private TemplateType templateType;

  @Column(name = "name", nullable = false, length = 100, updatable = false)
  private String name;

  /** For JPA. */
  protected TemplateRow() {}

  /**
   * Creates a row.
   *
   * @param versionId the owning version id
   * @param templateType template type
   * @param name name
   */
  @SuppressWarnings("java:S107")
  public TemplateRow(Long versionId, TemplateType templateType, String name) {
    this.versionId = versionId;
    this.templateType = templateType;
    this.name = name;
  }

  public Long getVersionId() {
    return versionId;
  }

  public TemplateType getTemplateType() {
    return templateType;
  }

  public String getName() {
    return name;
  }
}
