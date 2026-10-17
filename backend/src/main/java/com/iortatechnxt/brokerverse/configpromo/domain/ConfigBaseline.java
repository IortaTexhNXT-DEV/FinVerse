package com.iortatechnxt.brokerverse.configpromo.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A package marked as the configuration baseline of this environment (for example the UAT
 * signed-off configuration); the drift report compares the current configuration with it.
 */
@Entity
@Table(name = "cfp_baseline")
public class ConfigBaseline extends BaseEntity {

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "package_id", nullable = false)
  private Long packageId;

  @Column(name = "environment", nullable = false, length = 40)
  private String environment;

  @Column(name = "remarks", length = 500)
  private String remarks;

  @Column(name = "active", nullable = false)
  private boolean active;

  protected ConfigBaseline() {}

  /**
   * Creates a baseline.
   *
   * @param name name, e.g. "UAT signed-off baseline 2027-11"
   * @param packageId package
   * @param environment environment
   * @param remarks remarks
   */
  public ConfigBaseline(String name, Long packageId, String environment, String remarks) {
    this.name = name;
    this.packageId = packageId;
    this.environment = environment;
    this.remarks = remarks;
    this.active = true;
  }

  /** Retires the baseline (kept for the audit trail). */
  public void retire() {
    this.active = false;
  }

  public String getName() {
    return name;
  }

  public Long getPackageId() {
    return packageId;
  }

  public String getEnvironment() {
    return environment;
  }

  public String getRemarks() {
    return remarks;
  }

  public boolean isActive() {
    return active;
  }
}
