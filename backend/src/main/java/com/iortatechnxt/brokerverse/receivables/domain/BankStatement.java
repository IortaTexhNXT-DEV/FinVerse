package com.iortatechnxt.brokerverse.receivables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Imported bank statement of one GL bank account. */
@Entity
@Table(name = "brs_statement")
public class BankStatement extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(name = "statement_ref", nullable = false, length = 60)
  private String statementRef;

  @Column(name = "period_from", nullable = false)
  private LocalDate periodFrom;

  @Column(name = "period_to", nullable = false)
  private LocalDate periodTo;

  @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal openingBalance;

  @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal closingBalance;

  @Column(name = "total_debit", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalDebit;

  @Column(name = "total_credit", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalCredit;

  @Column(name = "line_count", nullable = false)
  private int lineCount;

  @Column(name = "file_name", length = 200)
  private String fileName;

  protected BankStatement() {}

  /**
   * Creates a statement header.
   *
   * @param companyId company
   * @param bankAccountCode GL bank account
   * @param statementRef statement reference (unique per bank account)
   * @param fileName uploaded file name
   * @param summary period, balances and totals of the lines
   */
  public BankStatement(
      Long companyId,
      String bankAccountCode,
      String statementRef,
      String fileName,
      StatementSummary summary) {
    this.companyId = companyId;
    this.bankAccountCode = bankAccountCode;
    this.statementRef = statementRef;
    this.fileName = fileName;
    this.periodFrom = summary.periodFrom();
    this.periodTo = summary.periodTo();
    this.openingBalance = summary.openingBalance();
    this.closingBalance = summary.closingBalance();
    this.totalDebit = summary.totalDebit();
    this.totalCredit = summary.totalCredit();
    this.lineCount = summary.lineCount();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public String getStatementRef() {
    return statementRef;
  }

  public LocalDate getPeriodFrom() {
    return periodFrom;
  }

  public LocalDate getPeriodTo() {
    return periodTo;
  }

  public BigDecimal getOpeningBalance() {
    return openingBalance;
  }

  public BigDecimal getClosingBalance() {
    return closingBalance;
  }

  public BigDecimal getTotalDebit() {
    return totalDebit;
  }

  public BigDecimal getTotalCredit() {
    return totalCredit;
  }

  public int getLineCount() {
    return lineCount;
  }

  public String getFileName() {
    return fileName;
  }

  /**
   * Period, balances and totals of a statement.
   *
   * @param periodFrom first value date
   * @param periodTo last value date
   * @param openingBalance balance before the first line
   * @param closingBalance balance after the last line
   * @param totalDebit sum of withdrawals
   * @param totalCredit sum of deposits
   * @param lineCount number of lines
   */
  public record StatementSummary(
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal openingBalance,
      BigDecimal closingBalance,
      BigDecimal totalDebit,
      BigDecimal totalCredit,
      int lineCount) {}
}
