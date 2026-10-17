package com.iortatechnxt.brokerverse.migration.mapping.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A data-quality rule of the catalogue (DATA_MIGRATION_DESIGN section 8; workbook rules DQ-001 to
 * DQ-056): layouts it applies to, columns, kind, severity and the message shown on the issue. The
 * Data Steward may switch a rule off or change its severity; the check itself is implemented by the
 * rule engine under the rule code.
 */
@Entity
@Table(name = "mig_rule")
public class MigRule extends BaseEntity {

  @Column(nullable = false, length = 10, updatable = false)
  private String code;

  @Column(name = "layout_scope", nullable = false, length = 40)
  private String layoutScope;

  @Column(nullable = false, length = 200)
  private String columns;

  @Column(nullable = false, length = 20)
  private String kind;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(nullable = false, length = 10)
  private String severity;

  @Column(nullable = false, length = 300)
  private String message;

  @Column(name = "fixed_by", length = 120)
  private String fixedBy;

  @Column(nullable = false)
  private boolean active = true;

  protected MigRule() {}

  /**
   * Changes the severity and activation.
   *
   * @param newSeverity ERROR or WARNING
   * @param isActive active
   */
  public void configure(String newSeverity, boolean isActive) {
    this.severity = newSeverity;
    this.active = isActive;
  }

  public String getCode() {
    return code;
  }

  public String getLayoutScope() {
    return layoutScope;
  }

  public String getColumns() {
    return columns;
  }

  public String getKind() {
    return kind;
  }

  public String getDescription() {
    return description;
  }

  public String getSeverity() {
    return severity;
  }

  public String getMessage() {
    return message;
  }

  public String getFixedBy() {
    return fixedBy;
  }

  public boolean isActive() {
    return active;
  }
}
