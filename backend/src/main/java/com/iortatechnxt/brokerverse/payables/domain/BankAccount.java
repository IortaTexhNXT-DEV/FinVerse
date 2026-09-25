package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Company bank account (house bank). Master data under maker-checker control, shared by payments,
 * receipts, PDC registers and bank reconciliation.
 *
 * <p>Each bank account maps one-to-one to a postable GL account of a bank category (the {@code
 * BANK} account role of receipt and payment events). The optional PDC clearing account is the
 * liability credited when a post-dated cheque is issued on this account. Other modules read bank
 * accounts through {@code payables.service.BankAccountQueryService}.
 *
 * <p>Disbursement (DIS 2.24.2) tags an account active or inactive with maker-checker: an inactive
 * account is not {@link #isActive() active} and is refused for new payments.
 */
@Entity
@Table(name = "pay_bank_account")
public class BankAccount extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "bank_party_code", length = 30)
  private String bankPartyCode;

  @Column(name = "bank_name", nullable = false, length = 120)
  private String bankName;

  @Column(name = "account_no", nullable = false, length = 40)
  private String accountNo;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "gl_account_code", nullable = false, length = 30)
  private String glAccountCode;

  @Column(name = "pdc_clearing_account_code", length = 30)
  private String pdcClearingAccountCode;

  @Column(name = "branch_id")
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_format", nullable = false, length = 20)
  private NotificationFormat notificationFormat = NotificationFormat.FIXED_WIDTH;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BankAccountStatus status = BankAccountStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(name = "requested_status", length = 20)
  private BankAccountStatus requestedStatus;

  protected BankAccount() {}

  /**
   * Creates a bank account (pending authorization).
   *
   * @param companyId company
   * @param code short code, e.g. BDO-CA
   * @param currency account currency
   */
  public BankAccount(Long companyId, String code, String currency) {
    this.companyId = companyId;
    this.code = code;
    this.currency = currency;
  }

  /**
   * Asks to tag the account active or inactive (DIS 2.24.2): the record returns to pending
   * authorisation and the new status applies once a checker authorises it.
   *
   * @param target requested status
   */
  public void requestStatus(BankAccountStatus target) {
    if (target == status) {
      throw new BusinessRuleException(
          "BANK_ACCOUNT_STATUS_UNCHANGED", "Bank account " + code + " is already " + target);
    }
    requestedStatus = target;
    markModified();
  }

  @Override
  public void authorize(String checker, Instant when) {
    super.authorize(checker, when);
    if (requestedStatus != null) {
      status = requestedStatus;
      requestedStatus = null;
    }
  }

  /**
   * Usable in transactions: authorised and tagged active (DIS 2.24.2).
   *
   * @return true when authorised and active
   */
  @Override
  public boolean isActive() {
    return super.isActive() && status == BankAccountStatus.ACTIVE;
  }

  public BankAccountStatus getStatus() {
    return status;
  }

  public BankAccountStatus getRequestedStatus() {
    return requestedStatus;
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

  public String getBankPartyCode() {
    return bankPartyCode;
  }

  public void setBankPartyCode(String bankPartyCode) {
    this.bankPartyCode = bankPartyCode;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public void setAccountNo(String accountNo) {
    this.accountNo = accountNo;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getGlAccountCode() {
    return glAccountCode;
  }

  public void setGlAccountCode(String glAccountCode) {
    this.glAccountCode = glAccountCode;
  }

  public String getPdcClearingAccountCode() {
    return pdcClearingAccountCode;
  }

  public void setPdcClearingAccountCode(String pdcClearingAccountCode) {
    this.pdcClearingAccountCode = pdcClearingAccountCode;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public NotificationFormat getNotificationFormat() {
    return notificationFormat;
  }

  public void setNotificationFormat(NotificationFormat notificationFormat) {
    this.notificationFormat = notificationFormat;
  }
}
