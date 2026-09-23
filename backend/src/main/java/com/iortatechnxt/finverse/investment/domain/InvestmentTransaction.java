package com.iortatechnxt.finverse.investment.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A posted event of a holding, with the holding's position after it. Feeds the interest accrual
 * register and the realized gains report.
 */
@Entity
@Table(name = "inv_transaction")
public class InvestmentTransaction extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @ManyToOne(optional = false)
  @JoinColumn(name = "holding_id", nullable = false)
  private InvestmentHolding holding;

  @Column(name = "run_id")
  private Long runId;

  @Enumerated(EnumType.STRING)
  @Column(name = "txn_type", nullable = false, length = 20)
  private TransactionType txnType;

  @Column(name = "txn_date", nullable = false)
  private LocalDate txnDate;

  @Column(name = "from_date")
  private LocalDate fromDate;

  @Column(nullable = false)
  private int days;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "cash_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal cashAmount = BigDecimal.ZERO;

  @Column(name = "final_tax", nullable = false, precision = 19, scale = 2)
  private BigDecimal finalTax = BigDecimal.ZERO;

  @Column(name = "gain_loss", nullable = false, precision = 19, scale = 2)
  private BigDecimal gainLoss = BigDecimal.ZERO;

  @Column(name = "carrying_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal carryingAfter = BigDecimal.ZERO;

  @Column(name = "accrued_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal accruedAfter = BigDecimal.ZERO;

  @Column(name = "batch_no", length = 40)
  private String batchNo;

  @Column(length = 250)
  private String remarks;

  protected InvestmentTransaction() {}

  /**
   * Creates a transaction.
   *
   * @param holding holding
   * @param type type
   * @param date value date
   * @param amount principal amount of the event (interest, amortization, carrying, ...)
   */
  public InvestmentTransaction(
      InvestmentHolding holding, TransactionType type, LocalDate date, BigDecimal amount) {
    this.companyId = holding.getCompanyId();
    this.holding = holding;
    this.txnType = type;
    this.txnDate = date;
    this.amount = amount;
  }

  /**
   * Sets the period covered (accruals and amortization).
   *
   * @param from start (exclusive)
   * @param dayCount days in the period
   */
  public void covering(LocalDate from, int dayCount) {
    this.fromDate = from;
    this.days = dayCount;
  }

  /**
   * Sets the cash side of the event.
   *
   * @param cash cash received or paid
   * @param tax final tax withheld
   * @param result realized gain (positive) or loss, or income adjustment
   */
  public void settle(BigDecimal cash, BigDecimal tax, BigDecimal result) {
    this.cashAmount = cash;
    this.finalTax = tax;
    this.gainLoss = result;
  }

  /**
   * Records the posting and the holding's position after the event.
   *
   * @param journal journal batch number
   */
  public void posted(String journal) {
    this.batchNo = journal;
    this.carryingAfter = holding.carryingAmount();
    this.accruedAfter = holding.getAccruedInterest();
  }

  public Long getCompanyId() {
    return companyId;
  }

  public InvestmentHolding getHolding() {
    return holding;
  }

  public Long getRunId() {
    return runId;
  }

  public void setRunId(Long runId) {
    this.runId = runId;
  }

  public TransactionType getTxnType() {
    return txnType;
  }

  public LocalDate getTxnDate() {
    return txnDate;
  }

  public LocalDate getFromDate() {
    return fromDate;
  }

  public int getDays() {
    return days;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getCashAmount() {
    return cashAmount;
  }

  public BigDecimal getFinalTax() {
    return finalTax;
  }

  public BigDecimal getGainLoss() {
    return gainLoss;
  }

  public BigDecimal getCarryingAfter() {
    return carryingAfter;
  }

  public BigDecimal getAccruedAfter() {
    return accruedAfter;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }
}
