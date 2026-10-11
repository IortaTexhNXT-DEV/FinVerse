package com.iortatechnxt.brokerverse.investment.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A posted month-end accrual or amortization run; unique per company, type and period, so posting a
 * period twice returns the existing run.
 */
@Entity
@Table(name = "inv_run")
public class InvestmentRun extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Enumerated(EnumType.STRING)
  @Column(name = "run_type", nullable = false, length = 20)
  private RunType runType;

  @Column(nullable = false, length = 7)
  private String period;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "holding_count", nullable = false)
  private int holdingCount;

  @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  protected InvestmentRun() {}

  /**
   * Starts a run.
   *
   * @param companyId company
   * @param runType type
   * @param period month
   */
  public InvestmentRun(Long companyId, RunType runType, YearMonth period) {
    this.companyId = companyId;
    this.runType = runType;
    this.period = period.toString();
    this.periodEnd = period.atEndOfMonth();
  }

  /**
   * Counts a posted holding.
   *
   * @param amount amount posted for it
   */
  public void count(BigDecimal amount) {
    holdingCount++;
    totalAmount = totalAmount.add(amount);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public RunType getRunType() {
    return runType;
  }

  public String getPeriod() {
    return period;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public int getHoldingCount() {
    return holdingCount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }
}
