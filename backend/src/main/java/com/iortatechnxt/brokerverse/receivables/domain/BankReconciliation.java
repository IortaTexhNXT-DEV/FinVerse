package com.iortatechnxt.brokerverse.receivables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Bank reconciliation of one GL bank account as of a statement date: the saved Bank Reconciliation
 * Statement figures. It can be finalized only when the unexplained difference is zero.
 */
@Entity
@Table(name = "brs_reconciliation")
public class BankReconciliation extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(name = "as_of_date", nullable = false)
  private LocalDate asOfDate;

  @Column(name = "book_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal bookBalance;

  @Column(name = "book_debits_not_in_bank", nullable = false, precision = 19, scale = 2)
  private BigDecimal bookDebitsNotInBank;

  @Column(name = "book_credits_not_in_bank", nullable = false, precision = 19, scale = 2)
  private BigDecimal bookCreditsNotInBank;

  @Column(name = "bank_debits_not_in_book", nullable = false, precision = 19, scale = 2)
  private BigDecimal bankDebitsNotInBook;

  @Column(name = "bank_credits_not_in_book", nullable = false, precision = 19, scale = 2)
  private BigDecimal bankCreditsNotInBook;

  @Column(name = "computed_bank_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal computedBankBalance;

  @Column(name = "statement_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal statementBalance;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal difference;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReconciliationStatus status = ReconciliationStatus.IN_PROGRESS;

  @Column(name = "finalized_by", length = 50)
  private String finalizedBy;

  @Column(name = "finalized_at")
  private Instant finalizedAt;

  protected BankReconciliation() {}

  /**
   * Creates a reconciliation in progress.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param asOfDate statement date
   * @param figures current BRS figures
   */
  public BankReconciliation(
      Long companyId, String bankAccountCode, LocalDate asOfDate, BrsFigures figures) {
    this.companyId = companyId;
    this.bankAccountCode = bankAccountCode;
    this.asOfDate = asOfDate;
    apply(figures);
  }

  /**
   * Stores the latest BRS figures.
   *
   * @param f figures
   */
  public void refresh(BrsFigures f) {
    requireInProgress();
    apply(f);
  }

  private void apply(BrsFigures f) {
    this.bookBalance = f.bookBalance();
    this.bookDebitsNotInBank = f.bookDebitsNotInBank();
    this.bookCreditsNotInBank = f.bookCreditsNotInBank();
    this.bankDebitsNotInBook = f.bankDebitsNotInBook();
    this.bankCreditsNotInBook = f.bankCreditsNotInBook();
    this.computedBankBalance = f.computedBankBalance();
    this.statementBalance = f.statementBalance();
    this.difference = f.difference();
  }

  /**
   * Finalizes the reconciliation.
   *
   * @param user user
   * @param when timestamp
   */
  public void finalizeReconciliation(String user, Instant when) {
    requireInProgress();
    if (difference.signum() != 0) {
      throw new BusinessRuleException(
          "RECONCILIATION_DIFFERENCE",
          "Unexplained difference of " + difference + " must be cleared before finalizing");
    }
    this.status = ReconciliationStatus.FINALIZED;
    this.finalizedBy = user;
    this.finalizedAt = when;
  }

  private void requireInProgress() {
    if (status != ReconciliationStatus.IN_PROGRESS) {
      throw new BusinessRuleException(
          "RECONCILIATION_FINALIZED",
          "Reconciliation of " + bankAccountCode + " as of " + asOfDate + " is finalized");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public LocalDate getAsOfDate() {
    return asOfDate;
  }

  public BigDecimal getBookBalance() {
    return bookBalance;
  }

  public BigDecimal getBookDebitsNotInBank() {
    return bookDebitsNotInBank;
  }

  public BigDecimal getBookCreditsNotInBank() {
    return bookCreditsNotInBank;
  }

  public BigDecimal getBankDebitsNotInBook() {
    return bankDebitsNotInBook;
  }

  public BigDecimal getBankCreditsNotInBook() {
    return bankCreditsNotInBook;
  }

  public BigDecimal getComputedBankBalance() {
    return computedBankBalance;
  }

  public BigDecimal getStatementBalance() {
    return statementBalance;
  }

  public BigDecimal getDifference() {
    return difference;
  }

  public ReconciliationStatus getStatus() {
    return status;
  }

  public String getFinalizedBy() {
    return finalizedBy;
  }

  public Instant getFinalizedAt() {
    return finalizedAt;
  }
}
