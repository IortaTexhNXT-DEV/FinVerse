package com.iortatechnxt.brokerverse.closing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Revalued balance recorded in a revaluation run (FX Revaluation Register). */
@Entity
@Table(name = "fx_revaluation_line")
public class FxRevaluationLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "run_id", nullable = false)
  private FxRevaluationRun run;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "fc_balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal fcBalance;

  @Column(name = "booked_base", nullable = false, precision = 19, scale = 2)
  private BigDecimal bookedBase;

  @Column(name = "closing_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal closingRate;

  @Column(name = "revalued_base", nullable = false, precision = 19, scale = 2)
  private BigDecimal revaluedBase;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal difference;

  @Column(nullable = false)
  private boolean posted;

  protected FxRevaluationLine() {}

  FxRevaluationLine(FxRevaluationRun run, int lineNo, RevaluationItem item, boolean posted) {
    this.run = run;
    this.lineNo = lineNo;
    this.branchId = item.branchId();
    this.accountId = item.accountId();
    this.accountCode = item.accountCode();
    this.currency = item.currency();
    this.fcBalance = item.fcBalance();
    this.bookedBase = item.bookedBase();
    this.closingRate = item.closingRate();
    this.revaluedBase = item.revaluedBase();
    this.difference = item.difference();
    this.posted = posted;
  }

  public Long getId() {
    return id;
  }

  public FxRevaluationRun getRun() {
    return run;
  }

  public int getLineNo() {
    return lineNo;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getFcBalance() {
    return fcBalance;
  }

  public BigDecimal getBookedBase() {
    return bookedBase;
  }

  public BigDecimal getClosingRate() {
    return closingRate;
  }

  public BigDecimal getRevaluedBase() {
    return revaluedBase;
  }

  public BigDecimal getDifference() {
    return difference;
  }

  public boolean isPosted() {
    return posted;
  }
}
