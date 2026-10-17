package com.iortatechnxt.brokerverse.migration.legacy.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * An entry of the package map of the legacy packages (object R06, table {@code mig_package_map};
 * DATA_MIGRATION_DESIGN section 15.2): a legacy package and version, optionally split by risk code,
 * insurer or sum-insured band, mapped to a BIBS package version maintained by TSU or rejected. The
 * entries are handed to the Renewal package map used at Renewal sanitation.
 */
@Entity
@Table(name = "mig_package_map")
public class PackageMapRow extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "legacy_package_code", nullable = false, length = 20)
  private String legacyPackageCode;

  @Column(name = "legacy_package_version", nullable = false)
  private int legacyPackageVersion;

  @Column(name = "legacy_package_name", length = 200)
  private String legacyPackageName;

  @Column(name = "risk_code", length = 40)
  private String riskCode;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(name = "si_from", precision = 19, scale = 2)
  private BigDecimal siFrom;

  @Column(name = "si_to", precision = 19, scale = 2)
  private BigDecimal siTo;

  @Column(nullable = false, length = 10)
  private String action;

  @Column(name = "product_code", length = 40)
  private String productCode;

  @Column(name = "product_version_no")
  private Integer productVersionNo;

  @Column(length = 300)
  private String remarks;

  @Column(name = "batch_id", nullable = false, updatable = false)
  private Long batchId;

  @Column(name = "rolled_back", nullable = false)
  private boolean rolledBack;

  protected PackageMapRow() {}

  /**
   * Creates an entry.
   *
   * @param companyId company
   * @param entry map entry
   * @param batchId loading batch
   */
  public PackageMapRow(Long companyId, PackageMapEntry entry, Long batchId) {
    this.companyId = companyId;
    this.batchId = batchId;
    assign(entry);
  }

  /**
   * Replaces the entry (corrected row).
   *
   * @param entry map entry
   */
  public void apply(PackageMapEntry entry) {
    assign(entry);
  }

  private void assign(PackageMapEntry e) {
    this.legacyPackageCode = e.legacyPackageCode();
    this.legacyPackageVersion = e.legacyPackageVersion();
    this.legacyPackageName = e.legacyPackageName();
    this.riskCode = e.riskCode();
    this.insurerCode = e.insurerCode();
    this.siFrom = e.siFrom();
    this.siTo = e.siTo();
    this.action = e.action();
    this.productCode = e.productCode();
    this.productVersionNo = e.productVersionNo();
    this.remarks = e.remarks();
  }

  /** Withdrawn by the rollback of its batch. */
  public void rollBack() {
    this.rolledBack = true;
  }

  /**
   * The entry.
   *
   * @return entry
   */
  public PackageMapEntry entry() {
    return new PackageMapEntry(
        legacyPackageCode,
        legacyPackageVersion,
        legacyPackageName,
        riskCode,
        insurerCode,
        siFrom,
        siTo,
        action,
        productCode,
        productVersionNo,
        remarks);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBatchId() {
    return batchId;
  }

  public boolean isRolledBack() {
    return rolledBack;
  }

  /**
   * An entry of the package map.
   *
   * @param legacyPackageCode legacy package
   * @param legacyPackageVersion legacy package version
   * @param legacyPackageName legacy package name
   * @param riskCode risk code condition, null for any
   * @param insurerCode insurer condition, null for any
   * @param siFrom lower sum-insured bound, null for none
   * @param siTo upper sum-insured bound, null for none
   * @param action MAP or REJECT
   * @param productCode BIBS product (package) of a MAP entry
   * @param productVersionNo BIBS package version of a MAP entry
   * @param remarks remarks
   */
  public record PackageMapEntry(
      String legacyPackageCode,
      int legacyPackageVersion,
      String legacyPackageName,
      String riskCode,
      String insurerCode,
      BigDecimal siFrom,
      BigDecimal siTo,
      String action,
      String productCode,
      Integer productVersionNo,
      String remarks) {}
}
