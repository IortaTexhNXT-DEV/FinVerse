package com.iortatechnxt.finverse.closing.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * FX revaluation run of a company for one accounting period (unique per period: a period is
 * revalued once). Original transactions are never changed; the difference is posted in a separate
 * REVALUATION journal, optionally reversed on the first day of the next period.
 */
@Entity
@Table(name = "fx_revaluation_run")
public class FxRevaluationRun extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "period_id", nullable = false)
  private Long periodId;

  @Column(name = "period_name", nullable = false, length = 20)
  private String periodName;

  @Column(name = "revaluation_date", nullable = false)
  private LocalDate revaluationDate;

  @Column(name = "gain_loss_account", nullable = false, length = 30)
  private String gainLossAccount;

  @Column(name = "auto_reverse", nullable = false)
  private boolean autoReverse;

  @Column(name = "reversal_date")
  private LocalDate reversalDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FxRevaluationStatus status = FxRevaluationStatus.POSTED;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @Column(name = "reversal_batch_no", length = 40)
  private String reversalBatchNo;

  @Column(name = "total_gain", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalGain = BigDecimal.ZERO;

  @Column(name = "total_loss", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalLoss = BigDecimal.ZERO;

  @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("lineNo")
  private final List<FxRevaluationLine> lines = new ArrayList<>();

  protected FxRevaluationRun() {}

  /**
   * Creates a run.
   *
   * @param header run settings
   */
  public FxRevaluationRun(FxRevaluationHeader header) {
    this.companyId = header.companyId();
    this.periodId = header.periodId();
    this.periodName = header.periodName();
    this.revaluationDate = header.revaluationDate();
    this.gainLossAccount = header.gainLossAccount();
    this.autoReverse = header.autoReverse();
    this.reversalDate = header.reversalDate();
  }

  /**
   * Records the revalued balances and the resulting gain and loss totals.
   *
   * @param items revalued balances
   * @param postedFlags per item: whether a journal line was posted
   * @param batchNo revaluation journal (null when nothing was posted)
   */
  public void record(List<RevaluationItem> items, List<Boolean> postedFlags, String batchNo) {
    lines.clear();
    BigDecimal gain = BigDecimal.ZERO;
    BigDecimal loss = BigDecimal.ZERO;
    for (int i = 0; i < items.size(); i++) {
      RevaluationItem item = items.get(i);
      boolean posted = postedFlags.get(i);
      lines.add(new FxRevaluationLine(this, i + 1, item, posted));
      if (posted && item.difference().signum() > 0) {
        gain = gain.add(item.difference());
      } else if (posted) {
        loss = loss.add(item.difference().negate());
      }
    }
    this.totalGain = gain;
    this.totalLoss = loss;
    this.journalBatchNo = batchNo;
  }

  /**
   * Records the posted auto-reversal.
   *
   * @param batchNo reversal journal
   */
  public void markReversed(String batchNo) {
    this.reversalBatchNo = batchNo;
    this.status = FxRevaluationStatus.REVERSED;
  }

  /**
   * Whether an auto-reversal is still to be posted (next period was not yet open).
   *
   * @return true when pending
   */
  public boolean isReversalPending() {
    return autoReverse && journalBatchNo != null && reversalBatchNo == null;
  }

  /**
   * Net unrealized result of the run.
   *
   * @return gains minus losses
   */
  public BigDecimal netResult() {
    return totalGain.subtract(totalLoss);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getPeriodId() {
    return periodId;
  }

  public String getPeriodName() {
    return periodName;
  }

  public LocalDate getRevaluationDate() {
    return revaluationDate;
  }

  public String getGainLossAccount() {
    return gainLossAccount;
  }

  public boolean isAutoReverse() {
    return autoReverse;
  }

  public LocalDate getReversalDate() {
    return reversalDate;
  }

  public FxRevaluationStatus getStatus() {
    return status;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public String getReversalBatchNo() {
    return reversalBatchNo;
  }

  public BigDecimal getTotalGain() {
    return totalGain;
  }

  public BigDecimal getTotalLoss() {
    return totalLoss;
  }

  public List<FxRevaluationLine> getLines() {
    return lines;
  }
}
