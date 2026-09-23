package com.iortatechnxt.finverse.receivables.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bank deposit (pay-in) slip grouping cash and cheque receipts taken to one bank account. The bank
 * usually shows one credit per slip, which the reconciliation matches to the slip's receipts.
 */
@Entity
@Table(name = "rcv_deposit_slip")
public class DepositSlip extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "slip_no", nullable = false, length = 40)
  private String slipNo;

  @Column(name = "slip_date", nullable = false)
  private LocalDate slipDate;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "receipt_count", nullable = false)
  private int receiptCount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DepositSlipStatus status = DepositSlipStatus.PREPARED;

  @Column(name = "deposited_on")
  private LocalDate depositedOn;

  @Column(name = "deposited_by", length = 50)
  private String depositedBy;

  protected DepositSlip() {}

  /**
   * Creates a prepared slip.
   *
   * @param companyId company
   * @param branchId branch
   * @param slipNo slip number
   * @param slipDate slip date
   * @param bankAccountCode bank GL account
   * @param currency currency of the deposited receipts
   */
  public DepositSlip(
      Long companyId,
      Long branchId,
      String slipNo,
      LocalDate slipDate,
      String bankAccountCode,
      String currency) {
    this.companyId = companyId;
    this.branchId = branchId;
    this.slipNo = slipNo;
    this.slipDate = slipDate;
    this.bankAccountCode = bankAccountCode;
    this.currency = currency;
  }

  /**
   * Adds a receipt amount to the slip totals.
   *
   * @param value receipt amount
   */
  public void add(BigDecimal value) {
    requirePrepared();
    totalAmount = totalAmount.add(value);
    receiptCount++;
  }

  /**
   * Confirms that the slip was deposited at the bank.
   *
   * @param date deposit date
   * @param user confirming user
   */
  public void deposit(LocalDate date, String user) {
    requirePrepared();
    if (date.isBefore(slipDate)) {
      throw new BusinessRuleException(
          "INVALID_DEPOSIT_DATE", "Deposit date cannot precede the slip date " + slipDate);
    }
    this.status = DepositSlipStatus.DEPOSITED;
    this.depositedOn = date;
    this.depositedBy = user;
  }

  /** Cancels a slip that was not deposited. */
  public void cancel() {
    requirePrepared();
    this.status = DepositSlipStatus.CANCELLED;
  }

  private void requirePrepared() {
    if (status != DepositSlipStatus.PREPARED) {
      throw new BusinessRuleException(
          "SLIP_NOT_PREPARED", "Deposit slip " + slipNo + " is already " + status);
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getSlipNo() {
    return slipNo;
  }

  public LocalDate getSlipDate() {
    return slipDate;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public int getReceiptCount() {
    return receiptCount;
  }

  public DepositSlipStatus getStatus() {
    return status;
  }

  public LocalDate getDepositedOn() {
    return depositedOn;
  }

  public String getDepositedBy() {
    return depositedBy;
  }
}
