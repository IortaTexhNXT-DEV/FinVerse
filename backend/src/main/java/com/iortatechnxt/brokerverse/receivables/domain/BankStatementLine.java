package com.iortatechnxt.brokerverse.receivables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One line of a bank statement. Debit = money leaving the account (cheque paid, charges), credit =
 * money received (deposit, transfer in), from the bank's point of view.
 */
@Entity
@Table(name = "brs_statement_line")
public class BankStatementLine extends BaseEntity {

  @Column(name = "statement_id", nullable = false)
  private Long statementId;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "bank_account_code", nullable = false, length = 30)
  private String bankAccountCode;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "value_date", nullable = false)
  private LocalDate valueDate;

  @Column(length = 250)
  private String description;

  @Column(length = 60)
  private String reference;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal debit;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal credit;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Column(name = "match_id")
  private Long matchId;

  protected BankStatementLine() {}

  /**
   * Creates a line.
   *
   * @param statement statement
   * @param line parsed values
   */
  public BankStatementLine(BankStatement statement, ParsedLine line) {
    this.statementId = statement.getId();
    this.companyId = statement.getCompanyId();
    this.bankAccountCode = statement.getBankAccountCode();
    this.lineNo = line.lineNo();
    this.valueDate = line.valueDate();
    this.description = line.description();
    this.reference = line.reference();
    this.debit = line.debit();
    this.credit = line.credit();
    this.balance = line.balance();
  }

  /**
   * Book-side signed amount: deposits positive (a debit in the company's bank GL account),
   * withdrawals negative.
   *
   * @return credit minus debit
   */
  public BigDecimal signedAmount() {
    return credit.subtract(debit);
  }

  /**
   * Marks the line as reconciled.
   *
   * @param id match
   */
  public void match(Long id) {
    if (matchId != null) {
      throw new BusinessRuleException(
          "LINE_ALREADY_MATCHED", "Statement line " + lineNo + " is already reconciled");
    }
    this.matchId = id;
  }

  /** Marks the line as not reconciled. */
  public void unmatch() {
    this.matchId = null;
  }

  public Long getStatementId() {
    return statementId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getBankAccountCode() {
    return bankAccountCode;
  }

  public int getLineNo() {
    return lineNo;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public String getDescription() {
    return description;
  }

  public String getReference() {
    return reference;
  }

  public BigDecimal getDebit() {
    return debit;
  }

  public BigDecimal getCredit() {
    return credit;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public Long getMatchId() {
    return matchId;
  }

  /**
   * Parsed statement line.
   *
   * @param lineNo line number in the statement
   * @param valueDate value date
   * @param description description
   * @param reference reference / cheque number
   * @param debit withdrawal (zero when a deposit)
   * @param credit deposit (zero when a withdrawal)
   * @param balance running balance after the line
   */
  public record ParsedLine(
      int lineNo,
      LocalDate valueDate,
      String description,
      String reference,
      BigDecimal debit,
      BigDecimal credit,
      BigDecimal balance) {}
}
