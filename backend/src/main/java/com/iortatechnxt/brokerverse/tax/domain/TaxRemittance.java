package com.iortatechnxt.brokerverse.tax.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment of a filed return (one per return): what was paid, when, from which bank account, the
 * bank / eFPS payment reference and the journal that cleared the tax payable. The remittance
 * register (report TAX-REMIT) compares the payment date with the due date.
 */
@Entity
@Table(name = "tax_remittance")
public class TaxRemittance extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "return_id", nullable = false)
  private Long returnId;

  @Column(name = "form_code", nullable = false, length = 20)
  private String formCode;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "paid_on", nullable = false)
  private LocalDate paidOn;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "payable_cleared", nullable = false, precision = 19, scale = 2)
  private BigDecimal payableCleared;

  @Column(name = "credit_applied", nullable = false, precision = 19, scale = 2)
  private BigDecimal creditApplied;

  @Column(name = "bank_account_code", length = 30)
  private String bankAccountCode;

  @Column(name = "payment_reference", nullable = false, length = 60)
  private String paymentReference;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  protected TaxRemittance() {}

  /**
   * Records the remittance of a return.
   *
   * @param taxReturn the return paid
   * @param payment payment facts
   * @param posting amounts cleared and the journal (null batch when nothing was posted)
   */
  public TaxRemittance(TaxReturn taxReturn, RemittanceFacts payment, RemittancePosting posting) {
    this.companyId = taxReturn.getCompanyId();
    this.returnId = taxReturn.getId();
    this.formCode = taxReturn.getFormCode();
    this.periodStart = taxReturn.getPeriodStart();
    this.periodEnd = taxReturn.getPeriodEnd();
    this.dueDate = taxReturn.getDueDate();
    this.paidOn = payment.paidOn();
    this.bankAccountCode = payment.bankAccountCode();
    this.paymentReference = payment.reference();
    this.amount = posting.amount();
    this.payableCleared = posting.payableCleared();
    this.creditApplied = posting.creditApplied();
    this.journalBatchNo = posting.batchNo();
  }

  /**
   * Whether the payment was made after the due date.
   *
   * @return true when late
   */
  public boolean isLate() {
    return paidOn.isAfter(dueDate);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getReturnId() {
    return returnId;
  }

  public String getFormCode() {
    return formCode;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public LocalDate getPaidOn() {
    return paidOn;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getPayableCleared() {
    return payableCleared;
  }

  public BigDecimal getCreditApplied() {
    return creditApplied;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public String getPaymentReference() {
    return paymentReference;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }
}
