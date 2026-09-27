package com.iortatechnxt.brokerverse.account.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The legacy header of an account imported from a legacy system (object P01, BRD-13; table {@code
 * acc_account_legacy}, V823): source system, legacy policy reference, policy and cover numbers, the
 * legacy package kept as given until Renewal sanitation remaps it, the legacy status and the
 * migration batch that loaded it.
 */
@Entity
@Table(name = "acc_account_legacy")
public class AccountLegacyHeader extends BaseEntity {

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "source_system", nullable = false, length = 10)
  private String sourceSystem;

  @Column(name = "legacy_ref", nullable = false, length = 80)
  private String legacyRef;

  @Column(name = "policy_no", nullable = false, length = 60)
  private String policyNo;

  @Column(name = "cover_no", nullable = false, length = 40)
  private String coverNo;

  @Column(name = "cover_version")
  private Integer coverVersion;

  @Column(name = "legacy_client_no", nullable = false, length = 80)
  private String legacyClientNo;

  @Column(name = "legacy_package_code", length = 20)
  private String legacyPackageCode;

  @Column(name = "legacy_package_version")
  private Integer legacyPackageVersion;

  @Column(name = "legacy_status", length = 20)
  private String legacyStatus;

  @Column(name = "assured_name", length = 200)
  private String assuredName;

  @Column(name = "migration_batch", nullable = false, length = 20)
  private String migrationBatch;

  @Column(name = "rolled_back_at")
  private Instant rolledBackAt;

  protected AccountLegacyHeader() {}

  /**
   * Creates the header of an imported account.
   *
   * @param accountId account
   * @param companyId company
   * @param legacy legacy data of the header
   */
  public AccountLegacyHeader(Long accountId, Long companyId, LegacyPolicy legacy) {
    this.accountId = accountId;
    this.companyId = companyId;
    assign(legacy);
  }

  /**
   * Applies a legacy delta of the header before the freeze.
   *
   * @param legacy legacy data
   */
  public void apply(LegacyPolicy legacy) {
    assign(legacy);
  }

  private void assign(LegacyPolicy legacy) {
    this.sourceSystem = legacy.sourceSystem();
    this.legacyRef = legacy.legacyRef();
    this.policyNo = legacy.policyNo();
    this.coverNo = legacy.coverNo();
    this.coverVersion = legacy.coverVersion();
    this.legacyClientNo = legacy.legacyClientNo();
    this.legacyPackageCode = legacy.packageCode();
    this.legacyPackageVersion = legacy.packageVersion();
    this.legacyStatus = legacy.legacyStatus();
    this.assuredName = legacy.assuredName();
    this.migrationBatch = legacy.migrationBatch();
  }

  /**
   * The migration batch of the import was rolled back.
   *
   * @param when time
   */
  public void rolledBack(Instant when) {
    this.rolledBackAt = when;
  }

  public Long getAccountId() {
    return accountId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public String getLegacyRef() {
    return legacyRef;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getCoverNo() {
    return coverNo;
  }

  public Integer getCoverVersion() {
    return coverVersion;
  }

  public String getLegacyClientNo() {
    return legacyClientNo;
  }

  public String getLegacyPackageCode() {
    return legacyPackageCode;
  }

  public Integer getLegacyPackageVersion() {
    return legacyPackageVersion;
  }

  public String getLegacyStatus() {
    return legacyStatus;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getMigrationBatch() {
    return migrationBatch;
  }

  public Instant getRolledBackAt() {
    return rolledBackAt;
  }

  /**
   * The legacy data of a header.
   *
   * @param sourceSystem legacy source system
   * @param legacyRef legacy policy reference (cover number and version)
   * @param policyNo policy number
   * @param coverNo cover number
   * @param coverVersion cover version
   * @param legacyClientNo legacy client code
   * @param packageCode legacy package, kept as given
   * @param packageVersion legacy package version
   * @param legacyStatus legacy policy status
   * @param assuredName name of the assured
   * @param migrationBatch loading batch
   */
  public record LegacyPolicy(
      String sourceSystem,
      String legacyRef,
      String policyNo,
      String coverNo,
      Integer coverVersion,
      String legacyClientNo,
      String packageCode,
      Integer packageVersion,
      String legacyStatus,
      String assuredName,
      String migrationBatch) {}
}
