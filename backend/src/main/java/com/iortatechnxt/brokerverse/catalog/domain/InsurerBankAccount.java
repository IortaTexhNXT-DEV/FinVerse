package com.iortatechnxt.brokerverse.catalog.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Bank account of an insurer: where premiums are remitted, refunds or commissions come from;
 * maker-checker.
 */
@Entity
@Table(name = "cat_insurer_bank_account")
public class InsurerBankAccount extends AuthorizableEntity implements CatalogRecord {

  @Column(name = "insurer_id", nullable = false, updatable = false)
  private Long insurerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccountPurpose purpose;

  @Column(name = "bank_name", nullable = false, length = 120)
  private String bankName;

  @Column(name = "bank_branch", length = 120)
  private String bankBranch;

  @Column(name = "account_no", nullable = false, length = 40)
  private String accountNo;

  @Column(name = "account_name", nullable = false, length = 250)
  private String accountName;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "primary_account", nullable = false)
  private boolean primaryAccount;

  protected InsurerBankAccount() {}

  /**
   * Creates an account, pending authorization.
   *
   * @param insurerId insurer profile
   * @param details account
   */
  public InsurerBankAccount(Long insurerId, BankAccountDetails details) {
    this.insurerId = insurerId;
    apply(details);
  }

  /**
   * Changes the account; it must be authorized again.
   *
   * @param details account
   */
  public void update(BankAccountDetails details) {
    apply(details);
    markModified();
  }

  private void apply(BankAccountDetails d) {
    this.purpose = d.purpose();
    this.bankName = d.bankName();
    this.bankBranch = d.bankBranch();
    this.accountNo = d.accountNo();
    this.accountName = d.accountName();
    this.currency = d.currency();
    this.primaryAccount = d.primaryAccount();
  }

  @Override
  public String catalogReference() {
    return insurerId + "/" + accountNo;
  }

  @Override
  public String catalogDescription() {
    return bankName + " " + accountNo + " (" + purpose.name() + ")";
  }

  public Long getInsurerId() {
    return insurerId;
  }

  public AccountPurpose getPurpose() {
    return purpose;
  }

  public String getBankName() {
    return bankName;
  }

  public String getBankBranch() {
    return bankBranch;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public String getAccountName() {
    return accountName;
  }

  public String getCurrency() {
    return currency;
  }

  public boolean isPrimaryAccount() {
    return primaryAccount;
  }

  /** What an insurer bank account is used for. */
  public enum AccountPurpose {
    /** Premiums remitted to the insurer. */
    REMITTANCE,
    /** Refunds from the insurer. */
    REFUND,
    /** Commissions paid by the insurer. */
    COMMISSION
  }

  /**
   * Maintainable attributes of a bank account.
   *
   * @param purpose use of the account
   * @param bankName bank
   * @param bankBranch bank branch
   * @param accountNo account number
   * @param accountName account name
   * @param currency currency
   * @param primaryAccount primary account for its purpose
   */
  public record BankAccountDetails(
      AccountPurpose purpose,
      String bankName,
      String bankBranch,
      String accountNo,
      String accountName,
      String currency,
      boolean primaryAccount) {}
}
