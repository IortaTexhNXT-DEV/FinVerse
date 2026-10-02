package com.iortatechnxt.brokerverse.payrequest.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Liquidation of a disbursed cash advance (Appendix D Cash Advance Liquidation Form; in scope if
 * confirmed, AQ18): fieldwork days with their expenses, the over / (short) against the cash
 * advanced, and the journal of event {@code PRQ_CA_LIQUIDATION} once posted.
 */
@Entity
@Table(name = "prq_liquidation")
public class Liquidation extends BaseEntity {

  @Column(name = "request_id", nullable = false, updatable = false)
  private Long requestId;

  @Column(name = "liquidation_no", nullable = false, length = 30, updatable = false)
  private String liquidationNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LiquidationStatus status = LiquidationStatus.DRAFT;

  @Column(name = "job_level", length = 40)
  private String jobLevel;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(length = 500)
  private String remarks;

  @Column(name = "total_expenses", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalExpenses = BigDecimal.ZERO;

  @Column(name = "cash_advanced", nullable = false, precision = 19, scale = 2)
  private BigDecimal cashAdvanced;

  @Column(name = "over_short", nullable = false, precision = 19, scale = 2)
  private BigDecimal overShort = BigDecimal.ZERO;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "posted_by", length = 50)
  private String postedBy;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "journal_batch_no", length = 40)
  private String journalBatchNo;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "liquidation_id", nullable = false)
  @OrderBy("lineNo")
  private final List<LiquidationLine> lines = new ArrayList<>();

  protected Liquidation() {}

  /**
   * Opens the liquidation of a cash advance.
   *
   * @param requestId cash-advance request
   * @param liquidationNo number
   * @param cashAdvanced amount advanced
   */
  public Liquidation(Long requestId, String liquidationNo, BigDecimal cashAdvanced) {
    this.requestId = requestId;
    this.liquidationNo = liquidationNo;
    this.cashAdvanced = cashAdvanced;
    this.overShort = cashAdvanced;
  }

  /**
   * Replaces the form content while in draft.
   *
   * @param newJobLevel job level
   * @param newCostCenter cost centre of the expenses
   * @param newRemarks remarks
   * @param newLines fieldwork days
   */
  public void fill(
      String newJobLevel, String newCostCenter, String newRemarks, List<LiquidationLine> newLines) {
    this.jobLevel = newJobLevel;
    this.costCenter = newCostCenter;
    this.remarks = newRemarks;
    lines.clear();
    lines.addAll(newLines);
    this.totalExpenses =
        newLines.stream().map(LiquidationLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    this.overShort = cashAdvanced.subtract(totalExpenses);
  }

  /**
   * Submits the liquidation for checking.
   *
   * @param user employee
   * @param at time
   */
  public void submit(String user, Instant at) {
    this.status = LiquidationStatus.SUBMITTED;
    this.submittedBy = user;
    this.submittedAt = at;
  }

  /**
   * Returns the liquidation to the employee.
   *
   * @param text reason
   */
  public void sendBack(String text) {
    this.status = LiquidationStatus.DRAFT;
    this.remarks = text;
  }

  /**
   * Records the posting.
   *
   * @param user checker
   * @param at time
   * @param batchNo journal batch
   */
  public void posted(String user, Instant at, String batchNo) {
    this.status = LiquidationStatus.POSTED;
    this.postedBy = user;
    this.postedAt = at;
    this.journalBatchNo = batchNo;
  }

  /**
   * Sum of one expense category over the days.
   *
   * @param category category getter
   * @return total
   */
  public BigDecimal sum(Function<LiquidationLine, BigDecimal> category) {
    return lines.stream().map(category).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Long getRequestId() {
    return requestId;
  }

  public String getLiquidationNo() {
    return liquidationNo;
  }

  public LiquidationStatus getStatus() {
    return status;
  }

  public String getJobLevel() {
    return jobLevel;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getRemarks() {
    return remarks;
  }

  public BigDecimal getTotalExpenses() {
    return totalExpenses;
  }

  public BigDecimal getCashAdvanced() {
    return cashAdvanced;
  }

  public BigDecimal getOverShort() {
    return overShort;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getPostedBy() {
    return postedBy;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public String getJournalBatchNo() {
    return journalBatchNo;
  }

  public List<LiquidationLine> getLines() {
    return List.copyOf(lines);
  }
}
