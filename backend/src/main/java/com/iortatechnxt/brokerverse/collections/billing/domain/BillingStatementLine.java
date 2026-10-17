package com.iortatechnxt.brokerverse.collections.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A line of a statement of account (BRCLXN.058/060): the installment of the cycle or an earlier one
 * still unpaid, with the invoice, policy year, coverage period, amount, paid and balance.
 */
@Entity
@Table(name = "clx_billing_statement_line")
public class BillingStatementLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "statement_id", nullable = false, updatable = false)
  private BillingStatement statement;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private LineKind kind;

  @Column(name = "invoice_no", length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "policy_year", nullable = false, updatable = false)
  private int policyYear;

  @Column(name = "installment_seq", nullable = false, updatable = false)
  private int installmentSeq;

  @Column(name = "coverage_from", nullable = false, updatable = false)
  private LocalDate coverageFrom;

  @Column(name = "coverage_to", nullable = false, updatable = false)
  private LocalDate coverageTo;

  @Column(name = "due_date", nullable = false, updatable = false)
  private LocalDate dueDate;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal paid;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal balance;

  protected BillingStatementLine() {}

  BillingStatementLine(BillingStatement statement, int lineNo, Facts facts) {
    this.statement = statement;
    this.lineNo = lineNo;
    this.kind = facts.kind();
    this.invoiceNo = facts.invoiceNo();
    this.policyYear = facts.policyYear();
    this.installmentSeq = facts.installmentSeq();
    this.coverageFrom = facts.coverageFrom();
    this.coverageTo = facts.coverageTo();
    this.dueDate = facts.dueDate();
    this.amount = facts.amount();
    this.paid = facts.paid();
    this.balance = facts.amount().subtract(facts.paid());
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public LineKind getKind() {
    return kind;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public int getPolicyYear() {
    return policyYear;
  }

  public int getInstallmentSeq() {
    return installmentSeq;
  }

  public LocalDate getCoverageFrom() {
    return coverageFrom;
  }

  public LocalDate getCoverageTo() {
    return coverageTo;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getPaid() {
    return paid;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  /** Why a line is on the statement. */
  public enum LineKind {
    /** The installment of the billing cycle. */
    CURRENT,
    /** An earlier installment still unpaid. */
    ARREARS
  }

  /**
   * What a line bills.
   *
   * @param kind current or arrears
   * @param invoiceNo invoice, null for a policy year not yet booked
   * @param policyYear policy year
   * @param installmentSeq installment of the plan
   * @param coverageFrom first day of the coverage billed
   * @param coverageTo last day of the coverage billed
   * @param dueDate due date
   * @param amount installment amount
   * @param paid paid so far
   */
  public record Facts(
      LineKind kind,
      String invoiceNo,
      int policyYear,
      int installmentSeq,
      LocalDate coverageFrom,
      LocalDate coverageTo,
      LocalDate dueDate,
      BigDecimal amount,
      BigDecimal paid) {}
}
