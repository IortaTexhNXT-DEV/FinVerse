package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Imprest petty cash fund (box) of a branch.
 *
 * <p>Imprest rule: the fund is established at its imprest amount; disbursements reduce the cash in
 * the box and can never exceed it; reimbursements restore the cash but never above the imprest
 * amount. Hence at all times {@code 0 <= cash balance <= imprest}, and cash + vouchers pending
 * reimbursement = imprest.
 */
@Entity
@Table(name = "pay_petty_cash_fund")
public class PettyCashFund extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(nullable = false, length = 20)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(nullable = false, length = 120)
  private String custodian;

  @Column(name = "gl_account_code", nullable = false, length = 30)
  private String glAccountCode;

  @Column(name = "replenish_bank_account_id", nullable = false)
  private Long replenishBankAccountId;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "imprest_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal imprestAmount;

  @Column(name = "cash_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal cashBalance = BigDecimal.ZERO;

  @Column(name = "established_on")
  private LocalDate establishedOn;

  protected PettyCashFund() {}

  /**
   * Creates a fund (pending authorization, not yet established).
   *
   * @param companyId company
   * @param branchId branch
   * @param code code
   * @param currency currency
   */
  public PettyCashFund(Long companyId, Long branchId, String code, String currency) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.code = code;
    this.currency = currency;
  }

  /**
   * Establishes the fund: the imprest amount is drawn from the bank into the box.
   *
   * @param date establishment date
   */
  public void establish(LocalDate date) {
    if (!isActive()) {
      throw new BusinessRuleException(
          "FUND_NOT_ACTIVE", "Petty cash fund " + code + " is not active");
    }
    if (establishedOn != null) {
      throw new BusinessRuleException(
          "FUND_ALREADY_ESTABLISHED",
          "Petty cash fund " + code + " was established on " + establishedOn);
    }
    establishedOn = date;
    cashBalance = imprestAmount;
  }

  /**
   * Pays cash out of the box.
   *
   * @param amount amount
   */
  public void disburse(BigDecimal amount) {
    requireOperational();
    if (amount.compareTo(cashBalance) > 0) {
      throw new BusinessRuleException(
          "PETTY_CASH_INSUFFICIENT",
          "Disbursement "
              + amount
              + " exceeds the cash in fund "
              + code
              + " ("
              + cashBalance
              + ")");
    }
    cashBalance = cashBalance.subtract(amount);
  }

  /**
   * Refills the box from the bank.
   *
   * @param amount amount (the total of the reimbursed vouchers)
   */
  public void replenish(BigDecimal amount) {
    requireOperational();
    if (cashBalance.add(amount).compareTo(imprestAmount) > 0) {
      throw new BusinessRuleException(
          "IMPREST_EXCEEDED",
          "Reimbursement "
              + amount
              + " would raise fund "
              + code
              + " above its imprest "
              + imprestAmount);
    }
    cashBalance = cashBalance.add(amount);
  }

  /**
   * Changes the imprest amount (re-authorization required).
   *
   * @param amount new imprest, not below the cash currently held
   */
  public void changeImprest(BigDecimal amount) {
    if (amount.compareTo(cashBalance) < 0) {
      throw new BusinessRuleException(
          "IMPREST_BELOW_CASH", "Imprest cannot be set below the cash held " + cashBalance);
    }
    imprestAmount = amount;
  }

  private void requireOperational() {
    if (!isActive() || establishedOn == null) {
      throw new BusinessRuleException(
          "FUND_NOT_OPERATIONAL", "Petty cash fund " + code + " is not active and established");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
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

  public String getCustodian() {
    return custodian;
  }

  public void setCustodian(String custodian) {
    this.custodian = custodian;
  }

  public String getGlAccountCode() {
    return glAccountCode;
  }

  public void setGlAccountCode(String glAccountCode) {
    this.glAccountCode = glAccountCode;
  }

  public Long getReplenishBankAccountId() {
    return replenishBankAccountId;
  }

  public void setReplenishBankAccountId(Long replenishBankAccountId) {
    this.replenishBankAccountId = replenishBankAccountId;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getImprestAmount() {
    return imprestAmount;
  }

  public BigDecimal getCashBalance() {
    return cashBalance;
  }

  public LocalDate getEstablishedOn() {
    return establishedOn;
  }
}
