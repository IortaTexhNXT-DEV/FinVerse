package com.iortatechnxt.finverse.organization.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Legal entity keeping its own books (base currency, fiscal calendar and posting windows).
 *
 * <p>{@code backValueDays}/{@code forwardValueDays} implement the company-level value-date window
 * for journals (supplementary / back-dated transaction control).
 */
@Entity
@Table(name = "org_company")
public class Company extends AuthorizableEntity {

  @Column(nullable = false, unique = true, length = 10)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency;

  @Column(name = "tax_id", length = 30)
  private String taxId;

  @Column(length = 300)
  private String address;

  @Column(name = "fiscal_year_start_month", nullable = false)
  private int fiscalYearStartMonth = 1;

  @Column(name = "back_value_days", nullable = false)
  private int backValueDays;

  @Column(name = "forward_value_days", nullable = false)
  private int forwardValueDays;

  @Column(name = "retained_earnings_account", length = 30)
  private String retainedEarningsAccount;

  protected Company() {}

  /**
   * Creates a company.
   *
   * @param code unique company code
   * @param name legal name
   * @param baseCurrency ISO 4217 base currency
   */
  public Company(String code, String name, String baseCurrency) {
    this.code = code;
    this.name = name;
    this.baseCurrency = baseCurrency;
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

  public String getBaseCurrency() {
    return baseCurrency;
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

  public int getFiscalYearStartMonth() {
    return fiscalYearStartMonth;
  }

  public void setFiscalYearStartMonth(int fiscalYearStartMonth) {
    this.fiscalYearStartMonth = fiscalYearStartMonth;
  }

  public int getBackValueDays() {
    return backValueDays;
  }

  public void setBackValueDays(int backValueDays) {
    this.backValueDays = backValueDays;
  }

  public int getForwardValueDays() {
    return forwardValueDays;
  }

  public void setForwardValueDays(int forwardValueDays) {
    this.forwardValueDays = forwardValueDays;
  }

  public String getRetainedEarningsAccount() {
    return retainedEarningsAccount;
  }

  public void setRetainedEarningsAccount(String retainedEarningsAccount) {
    this.retainedEarningsAccount = retainedEarningsAccount;
  }
}
