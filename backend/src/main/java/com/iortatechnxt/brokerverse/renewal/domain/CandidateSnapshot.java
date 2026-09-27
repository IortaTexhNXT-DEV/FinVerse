package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The listing snapshot of a renewal candidate (RENEWAL_DESIGN section 4.1): the fields of the
 * expiring list, the status report and the insurer extract, copied from the expiring account and
 * invoice at extraction and refreshed at each evaluation. Money is never read from it for a
 * decision: balances, endorsements and payments are read live (BRRN.011 / 027).
 *
 * @param policyNo expiring policy number
 * @param coverNo cover number
 * @param versionNo package version of the expiring account
 * @param pnNos PN numbers, comma separated
 * @param client client, assured and contact
 * @param product product, line, segment, origin and account type
 * @param sales branch, region, department, sales unit, unit head and account officer
 * @param insurerCode insurer party code
 * @param mortgage mortgaged flag and mortgagee bank
 * @param packaged packaged product
 * @param inceptionDate expiring period start
 * @param expiryDate expiring period end
 * @param premium basic and gross premium, sum insured, rates and currency
 * @param legacyPackageCode legacy package code of a migrated policy (DMQ36)
 * @param legacyPackageVersion legacy package version of a migrated policy
 */
@Embeddable
public record CandidateSnapshot(
    @Column(name = "expiring_policy_no", length = 60) String policyNo,
    @Column(name = "cover_no", length = 40) String coverNo,
    @Column(name = "version_no") Integer versionNo,
    @Column(name = "pn_nos", length = 500) String pnNos,
    SnapshotClient client,
    SnapshotProduct product,
    SnapshotSales sales,
    @Column(name = "insurer_code", length = 30) String insurerCode,
    SnapshotMortgage mortgage,
    @Column(name = "packaged", nullable = false) boolean packaged,
    @Column(name = "inception_date") LocalDate inceptionDate,
    @Column(name = "expiry_date", nullable = false) LocalDate expiryDate,
    SnapshotPremium premium,
    @Column(name = "legacy_package_code", length = 40) String legacyPackageCode,
    @Column(name = "legacy_package_version", length = 20) String legacyPackageVersion) {

  /**
   * Client, assured and contact.
   *
   * @param clientId crm client id
   * @param clientCode client code
   * @param clientName client name
   * @param assuredName assured name
   * @param email registered e-mail of the client or account contact
   */
  @Embeddable
  public record SnapshotClient(
      @Column(name = "client_id") Long clientId,
      @Column(name = "client_code", length = 30) String clientCode,
      @Column(name = "client_name", nullable = false, length = 250) String clientName,
      @Column(name = "assured_name", length = 250) String assuredName,
      @Column(name = "client_email", length = 120) String email) {}

  /**
   * Product classification.
   *
   * @param productCode risk code
   * @param productName risk name
   * @param lineCode product line
   * @param segment market segment
   * @param businessOrigin business origin (source channel)
   * @param accountType account type
   */
  @Embeddable
  public record SnapshotProduct(
      @Column(name = "product_code", length = 20) String productCode,
      @Column(name = "product_name", length = 200) String productName,
      @Column(name = "line_code", length = 30) String lineCode,
      @Column(name = "segment", length = 40) String segment,
      @Column(name = "business_origin", length = 40) String businessOrigin,
      @Column(name = "account_type", length = 40) String accountType) {}

  /**
   * Sales organisation of the account.
   *
   * @param branchCode invoicing branch
   * @param regionCode region
   * @param departmentCode department
   * @param salesUnit sales team
   * @param unitHead unit head user name
   * @param accountOfficer account officer user name
   */
  @Embeddable
  public record SnapshotSales(
      @Column(name = "branch_code", length = 20) String branchCode,
      @Column(name = "region_code", length = 20) String regionCode,
      @Column(name = "department_code", length = 20) String departmentCode,
      @Column(name = "sales_unit", length = 20) String salesUnit,
      @Column(name = "unit_head", length = 50) String unitHead,
      @Column(name = "account_officer", length = 50) String accountOfficer) {}

  /**
   * Mortgage of the risk.
   *
   * @param mortgaged mortgaged to a bank
   * @param bank mortgagee bank
   */
  @Embeddable
  public record SnapshotMortgage(
      @Column(name = "mortgaged", nullable = false) boolean mortgaged,
      @Column(name = "mortgagee_bank", length = 120) String bank) {}

  /**
   * Premium of the expiring term.
   *
   * @param basicPremium basic (net) premium
   * @param grossPremium gross premium
   * @param totalSumInsured total sum insured
   * @param premiumRate premium rate in percent
   * @param commissionRate commission rate in percent
   * @param currency currency
   */
  @Embeddable
  public record SnapshotPremium(
      @Column(name = "basic_premium", precision = 19, scale = 2) BigDecimal basicPremium,
      @Column(name = "gross_premium", precision = 19, scale = 2) BigDecimal grossPremium,
      @Column(name = "total_sum_insured", precision = 19, scale = 2) BigDecimal totalSumInsured,
      @Column(name = "premium_rate", precision = 19, scale = 8) BigDecimal premiumRate,
      @Column(name = "commission_rate", precision = 19, scale = 8) BigDecimal commissionRate,
      @Column(name = "currency", length = 3) String currency) {}

  /**
   * The client name (never null).
   *
   * @return client name
   */
  public String clientName() {
    return client == null ? null : client.clientName();
  }
}
