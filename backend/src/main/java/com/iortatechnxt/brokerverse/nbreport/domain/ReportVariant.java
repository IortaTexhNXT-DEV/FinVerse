package com.iortatechnxt.brokerverse.nbreport.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A saved report variant (BRNB.057): the parameters of a report saved by a user under a name,
 * optionally shared with every user who may run the report. The ad-hoc report builder is parked
 * (Q40).
 */
@Entity
@Table(name = "nbr_report_variant")
public class ReportVariant extends BaseEntity {

  @Column(nullable = false, updatable = false, length = 50)
  private String owner;

  @Column(name = "report_code", nullable = false, updatable = false, length = 40)
  private String reportCode;

  @Column(nullable = false, updatable = false, length = 80)
  private String name;

  @Column(nullable = false, columnDefinition = "text")
  private String parameters;

  @Column(nullable = false)
  private boolean shared;

  protected ReportVariant() {}

  /**
   * Creates a variant.
   *
   * @param owner owner
   * @param reportCode report
   * @param name name
   */
  public ReportVariant(String owner, String reportCode, String name) {
    this.owner = owner;
    this.reportCode = reportCode;
    this.name = name.strip();
  }

  /**
   * Replaces the saved parameters and the sharing.
   *
   * @param parametersJson parameters as a JSON object
   * @param share whether other users see the variant
   */
  public void save(String parametersJson, boolean share) {
    this.parameters = parametersJson;
    this.shared = share;
  }

  public String getOwner() {
    return owner;
  }

  public String getReportCode() {
    return reportCode;
  }

  public String getName() {
    return name;
  }

  public String getParameters() {
    return parameters;
  }

  public boolean isShared() {
    return shared;
  }
}
