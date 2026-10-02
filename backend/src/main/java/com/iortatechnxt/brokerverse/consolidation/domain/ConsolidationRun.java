package com.iortatechnxt.brokerverse.consolidation.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consolidation run of a group as of a date: the consolidated ledger (translated member balances,
 * CTA and elimination lines). Company ledgers are never modified.
 */
@Entity
@Table(name = "con_run")
public class ConsolidationRun extends BaseEntity {

  @Column(name = "group_id", nullable = false)
  private Long groupId;

  @Column(name = "run_no", nullable = false, length = 40, unique = true)
  private String runNo;

  @Column(name = "as_of_date", nullable = false)
  private LocalDate asOfDate;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ConsolidationRunStatus status = ConsolidationRunStatus.DRAFT;

  @Column(name = "total_debit", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalDebit = BigDecimal.ZERO;

  @Column(name = "total_credit", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalCredit = BigDecimal.ZERO;

  @Column(name = "finalized_by", length = 50)
  private String finalizedBy;

  @Column(name = "finalized_at")
  private Instant finalizedAt;

  @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<ConsolidationRunLine> lines = new ArrayList<>();

  protected ConsolidationRun() {}

  /**
   * Creates a draft run.
   *
   * @param groupId group
   * @param runNo run number
   * @param asOfDate as-of date
   * @param currency consolidation currency
   */
  public ConsolidationRun(Long groupId, String runNo, LocalDate asOfDate, String currency) {
    this.groupId = groupId;
    this.runNo = runNo;
    this.asOfDate = asOfDate;
    this.currency = currency;
  }

  /**
   * Stores the consolidated ledger and its debit/credit totals.
   *
   * @param values lines
   */
  public void replaceLines(List<ConsolidationLineValues> values) {
    lines.clear();
    BigDecimal debit = BigDecimal.ZERO;
    BigDecimal credit = BigDecimal.ZERO;
    int no = 1;
    for (ConsolidationLineValues v : values) {
      lines.add(new ConsolidationRunLine(this, no++, v));
      if (v.amount().signum() > 0) {
        debit = debit.add(v.amount());
      } else {
        credit = credit.add(v.amount().negate());
      }
    }
    this.totalDebit = debit;
    this.totalCredit = credit;
  }

  /**
   * Locks the run as the final consolidation for its date.
   *
   * @param user user
   * @param when timestamp
   */
  public void finalizeRun(String user, Instant when) {
    requireDraft();
    status = ConsolidationRunStatus.FINAL;
    finalizedBy = user;
    finalizedAt = when;
  }

  /** Cancels a draft run (superseded by a newer run). */
  public void cancel() {
    requireDraft();
    status = ConsolidationRunStatus.CANCELLED;
  }

  /**
   * Whether the consolidated trial balance balances.
   *
   * @return true when total debit equals total credit
   */
  public boolean isBalanced() {
    return totalDebit.compareTo(totalCredit) == 0;
  }

  private void requireDraft() {
    if (status != ConsolidationRunStatus.DRAFT) {
      throw new BusinessRuleException(
          "RUN_NOT_DRAFT", "Consolidation run " + runNo + " is " + status);
    }
  }

  public Long getGroupId() {
    return groupId;
  }

  public String getRunNo() {
    return runNo;
  }

  public LocalDate getAsOfDate() {
    return asOfDate;
  }

  public String getCurrency() {
    return currency;
  }

  public ConsolidationRunStatus getStatus() {
    return status;
  }

  public BigDecimal getTotalDebit() {
    return totalDebit;
  }

  public BigDecimal getTotalCredit() {
    return totalCredit;
  }

  public String getFinalizedBy() {
    return finalizedBy;
  }

  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  public List<ConsolidationRunLine> getLines() {
    return lines;
  }
}
