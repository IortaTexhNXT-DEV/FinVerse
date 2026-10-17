package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A bank account of a payee (DIS 2.2.0, AQ11): bank, branch, account number (masked in lists
 * without {@code DISB_PAYEE_VIEW_FULL}), account name, currency and the mode it serves (credit to
 * account, telegraphic transfer, online banking).
 */
@Entity
@Table(name = "dsb_payee_account")
public class PayeeAccount extends BaseEntity {

  private static final int VISIBLE_DIGITS = 4;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "payee_id", nullable = false, updatable = false)
  private Payee payee;

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DisbursementMode mode;

  @Column(name = "primary_account", nullable = false)
  private boolean primaryAccount;

  @Column(nullable = false)
  private boolean active = true;

  protected PayeeAccount() {}

  /**
   * A bank account.
   *
   * @param payee payee
   * @param details account details
   */
  public PayeeAccount(Payee payee, AccountDetails details) {
    this.payee = payee;
    this.bankName = details.bankName();
    this.bankBranch = details.bankBranch();
    this.accountNo = details.accountNo();
    this.accountName = details.accountName();
    this.currency = details.currency();
    this.mode = details.mode();
    this.primaryAccount = details.primary();
  }

  /**
   * The account number with all but the last four digits hidden (DIS 2.2.8 addendum).
   *
   * @param number account number
   * @return masked number
   */
  public static String mask(String number) {
    if (number == null || number.length() <= VISIBLE_DIGITS) {
      return number;
    }
    return "*".repeat(number.length() - VISIBLE_DIGITS)
        + number.substring(number.length() - VISIBLE_DIGITS);
  }

  /** Deactivates the account (kept for history). */
  public void deactivate() {
    active = false;
    primaryAccount = false;
  }

  public Payee getPayee() {
    return payee;
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

  public DisbursementMode getMode() {
    return mode;
  }

  public boolean isPrimaryAccount() {
    return primaryAccount;
  }

  public void setPrimaryAccount(boolean primaryAccount) {
    this.primaryAccount = primaryAccount;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Bank account details.
   *
   * @param bankName bank
   * @param bankBranch branch
   * @param accountNo account number
   * @param accountName account name
   * @param currency currency
   * @param mode mode served (CTA, TT, ONLINE_BANKING)
   * @param primary primary account of the payee
   */
  public record AccountDetails(
      String bankName,
      String bankBranch,
      String accountNo,
      String accountName,
      String currency,
      DisbursementMode mode,
      boolean primary) {}
}
