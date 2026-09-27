package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * An entry of the PACKAGE code map (DMQ36; DATA_MIGRATION_DESIGN section 15.2): a legacy package
 * code and version, optionally narrowed by risk code, insurer or sum-insured band, maps to a BIBS
 * package version, or is marked REJECT (no BIBS package). The migration loads the approved map
 * (source MIGRATION); after go-live it is maintained here (source SETUP, or CHOICE when an approved
 * choice of the processing team is added), with maker-checker.
 */
@Entity
@Table(name = "rnw_package_map")
public class PackageMapEntry extends AuthorizableEntity {

  /** Action of an entry. */
  public static final String MAP = "MAP";

  /** No BIBS package for the legacy package. */
  public static final String REJECT = "REJECT";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "legacy_package_code", nullable = false, length = 40)
  private String legacyPackageCode;

  @Column(name = "legacy_package_version", length = 20)
  private String legacyPackageVersion;

  @Column(name = "risk_code", length = 20)
  private String riskCode;

  @Column(name = "insurer_code", length = 30)
  private String insurerCode;

  @Column(name = "si_from", precision = 19, scale = 2)
  private BigDecimal siFrom;

  @Column(name = "si_to", precision = 19, scale = 2)
  private BigDecimal siTo;

  @Column(nullable = false, length = 10)
  private String action;

  @Column(name = "product_code", length = 20)
  private String productCode;

  @Column(name = "product_version_no")
  private Integer productVersionNo;

  @Column(name = "map_version", nullable = false)
  private int mapVersion = 1;

  @Column(nullable = false, length = 20)
  private String source;

  @Column(length = 200)
  private String remarks;

  protected PackageMapEntry() {}

  /**
   * Creates an entry pending authorization.
   *
   * @param companyId company
   * @param data entry data
   * @param source MIGRATION, SETUP or CHOICE
   */
  public PackageMapEntry(Long companyId, Data data, String source) {
    this.companyId = companyId;
    this.source = source;
    apply(data);
  }

  /**
   * Changes the entry; it must be authorized again.
   *
   * @param data entry data
   */
  public void update(Data data) {
    apply(data);
    this.mapVersion++;
    markModified();
  }

  private void apply(Data data) {
    this.legacyPackageCode = data.legacyPackageCode();
    this.legacyPackageVersion = data.legacyPackageVersion();
    this.riskCode = data.riskCode();
    this.insurerCode = data.insurerCode();
    this.siFrom = data.siFrom();
    this.siTo = data.siTo();
    this.action = data.productCode() == null ? REJECT : MAP;
    this.productCode = data.productCode();
    this.productVersionNo = data.productVersionNo();
    this.remarks = data.remarks();
  }

  /**
   * Whether the entry applies to a migrated policy.
   *
   * @param packageVersion legacy package version of the policy
   * @param risk risk code of the policy
   * @param insurer insurer of the policy
   * @param sumInsured sum insured of the policy
   * @return true when authorized and every qualifier matches
   */
  public boolean appliesTo(
      String packageVersion, String risk, String insurer, BigDecimal sumInsured) {
    return isActive()
        && matches(legacyPackageVersion, packageVersion)
        && matches(riskCode, risk)
        && matches(insurerCode, insurer)
        && inBand(sumInsured);
  }

  private static boolean matches(String qualifier, String value) {
    return qualifier == null || qualifier.equals(value);
  }

  private boolean inBand(BigDecimal sumInsured) {
    if (siFrom == null && siTo == null) {
      return true;
    }
    return sumInsured != null
        && (siFrom == null || sumInsured.compareTo(siFrom) >= 0)
        && (siTo == null || sumInsured.compareTo(siTo) <= 0);
  }

  /**
   * Whether the entry says there is no BIBS package.
   *
   * @return true for REJECT
   */
  public boolean isReject() {
    return REJECT.equals(action);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getLegacyPackageCode() {
    return legacyPackageCode;
  }

  public String getLegacyPackageVersion() {
    return legacyPackageVersion;
  }

  public String getRiskCode() {
    return riskCode;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public BigDecimal getSiFrom() {
    return siFrom;
  }

  public BigDecimal getSiTo() {
    return siTo;
  }

  public String getAction() {
    return action;
  }

  public String getProductCode() {
    return productCode;
  }

  public Integer getProductVersionNo() {
    return productVersionNo;
  }

  public int getMapVersion() {
    return mapVersion;
  }

  public String getSource() {
    return source;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * Data of an entry.
   *
   * @param legacyPackageCode legacy package code
   * @param legacyPackageVersion legacy version, null for any
   * @param riskCode qualifier risk code, null for any
   * @param insurerCode qualifier insurer, null for any
   * @param siFrom qualifier sum insured from, null for none
   * @param siTo qualifier sum insured to, null for none
   * @param productCode BIBS package (risk code), null for REJECT
   * @param productVersionNo BIBS package version, null for REJECT
   * @param remarks remarks
   */
  public record Data(
      String legacyPackageCode,
      String legacyPackageVersion,
      String riskCode,
      String insurerCode,
      BigDecimal siFrom,
      BigDecimal siTo,
      String productCode,
      Integer productVersionNo,
      String remarks) {}
}
