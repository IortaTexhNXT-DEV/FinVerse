package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An entry of the maintained list "Post to Bank Account" (FRS.CSH.02.01.02, 02.02.02): the bank
 * account a receipt is posted to, its currency, whether it is the default of its currency and its
 * GL account (set by Comptrollership; without it the collection account parameter applies).
 */
@Entity
@Table(name = "csh_bank_account")
public class CashBankAccount extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 40, updatable = false)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "gl_account_code", length = 30)
  private String glAccountCode;

  @Column(name = "default_for_currency", nullable = false)
  private boolean defaultForCurrency;

  @Column(nullable = false)
  private boolean active = true;

  protected CashBankAccount() {}

  /**
   * Creates an account of the list.
   *
   * @param companyId company
   * @param code code
   * @param details name, currency, GL account and default flag
   */
  public CashBankAccount(Long companyId, String code, Details details) {
    this.companyId = companyId;
    this.code = code;
    change(details);
  }

  /**
   * Changes the details.
   *
   * @param details name, currency, GL account, default flag and active flag
   */
  public final void change(Details details) {
    this.name = details.name();
    this.currency = details.currency();
    this.glAccountCode = details.glAccountCode();
    this.defaultForCurrency = details.defaultForCurrency();
    this.active = details.active();
  }

  /** Clears the default flag (another account became the default of the currency). */
  public void notDefault() {
    this.defaultForCurrency = false;
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

  public String getCurrency() {
    return currency;
  }

  public String getGlAccountCode() {
    return glAccountCode;
  }

  public boolean isDefaultForCurrency() {
    return defaultForCurrency;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Editable details.
   *
   * @param name name
   * @param currency currency
   * @param glAccountCode GL account, may be null
   * @param defaultForCurrency default of its currency
   * @param active offered on the screens
   */
  public record Details(
      String name,
      String currency,
      String glAccountCode,
      boolean defaultForCurrency,
      boolean active) {}
}
