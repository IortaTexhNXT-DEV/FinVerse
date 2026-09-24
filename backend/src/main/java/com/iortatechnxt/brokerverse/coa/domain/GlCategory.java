package com.iortatechnxt.brokerverse.coa.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** General Ledger Category Master: groups accounts by nature (e.g. Bank, Capital, Premium). */
@Entity
@Table(name = "coa_category")
public class GlCategory extends BaseEntity {

  @Column(nullable = false, unique = true, length = 10)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_class", nullable = false, length = 20)
  private AccountClass accountClass;

  @Column(name = "bank_category", nullable = false)
  private boolean bankCategory;

  protected GlCategory() {}

  /**
   * Creates a category.
   *
   * @param code code
   * @param name name
   * @param accountClass account class
   * @param bankCategory whether accounts in the category are bank accounts
   */
  public GlCategory(String code, String name, AccountClass accountClass, boolean bankCategory) {
    this.code = code;
    this.name = name;
    this.accountClass = accountClass;
    this.bankCategory = bankCategory;
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

  public AccountClass getAccountClass() {
    return accountClass;
  }

  public boolean isBankCategory() {
    return bankCategory;
  }

  public void setBankCategory(boolean bankCategory) {
    this.bankCategory = bankCategory;
  }
}
