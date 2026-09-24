package com.iortatechnxt.brokerverse.party.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Business partner master: policyholders, intermediaries, reinsurers, coinsurers and suppliers.
 *
 * <p>The party {@code code} is the sub-ledger key carried on journal lines ({@code party_code}) and
 * open items, so control account balances can be analysed by party.
 */
@Entity
@Table(name = "pty_party")
public class Party extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "party_type", nullable = false, length = 30)
  private PartyType partyType;

  @Column(name = "tax_id", length = 30)
  private String taxId;

  @Column(length = 300)
  private String address;

  @Column(length = 120)
  private String email;

  @Column(length = 40)
  private String phone;

  @Column(name = "default_currency", nullable = false, length = 3)
  private String defaultCurrency;

  @Column(name = "credit_days", nullable = false)
  private int creditDays;

  @Column(name = "commission_rate", precision = 7, scale = 4)
  private BigDecimal commissionRate;

  @Column(name = "withholding_tax_rate", precision = 7, scale = 4)
  private BigDecimal withholdingTaxRate;

  @Column(name = "licence_no", length = 40)
  private String licenceNo;

  @Column(name = "bank_name", length = 120)
  private String bankName;

  @Column(name = "bank_account_no", length = 40)
  private String bankAccountNo;

  @Column(name = "branch_id")
  private Long branchId;

  protected Party() {}

  /**
   * Creates a party.
   *
   * @param companyId company
   * @param code unique code within the company
   * @param name name
   * @param partyType type
   * @param defaultCurrency default transaction currency
   */
  public Party(
      Long companyId, String code, String name, PartyType partyType, String defaultCurrency) {
    this.companyId = companyId;
    this.code = code;
    this.name = name;
    this.partyType = partyType;
    this.defaultCurrency = defaultCurrency;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PartyType getPartyType() {
    return partyType;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getDefaultCurrency() {
    return defaultCurrency;
  }

  public void setDefaultCurrency(String defaultCurrency) {
    this.defaultCurrency = defaultCurrency;
  }

  public int getCreditDays() {
    return creditDays;
  }

  public void setCreditDays(int creditDays) {
    this.creditDays = creditDays;
  }

  public BigDecimal getCommissionRate() {
    return commissionRate;
  }

  public void setCommissionRate(BigDecimal commissionRate) {
    this.commissionRate = commissionRate;
  }

  public BigDecimal getWithholdingTaxRate() {
    return withholdingTaxRate;
  }

  public void setWithholdingTaxRate(BigDecimal withholdingTaxRate) {
    this.withholdingTaxRate = withholdingTaxRate;
  }

  public String getLicenceNo() {
    return licenceNo;
  }

  public void setLicenceNo(String licenceNo) {
    this.licenceNo = licenceNo;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getBankAccountNo() {
    return bankAccountNo;
  }

  public void setBankAccountNo(String bankAccountNo) {
    this.bankAccountNo = bankAccountNo;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }
}
