package com.iortatechnxt.brokerverse.reserves.domain;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Actuarial valuation of the technical reserves of one company at a month-end date.
 *
 * <p>Life cycle (maker-checker): {@code PREVIEW} (calculated, may be recalculated) → {@code
 * PENDING_APPROVAL} (submitted by the preparer) → {@code APPROVED} (by a different user) → {@code
 * POSTED} (movement journals booked). A checker can reject a submitted run back to preview. Any run
 * can be cancelled; cancelling a posted run reverses its journals.
 */
@Entity
@Table(name = "rsv_valuation_run")
public class ValuationRun extends BaseEntity {

  private static final Set<RunStatus> CANCELLABLE =
      EnumSet.of(
          RunStatus.PREVIEW, RunStatus.PENDING_APPROVAL, RunStatus.APPROVED, RunStatus.POSTED);

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "valuation_date", nullable = false)
  private LocalDate valuationDate;

  @Column(name = "period_name", nullable = false, length = 20)
  private String periodName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RunStatus status = RunStatus.PREVIEW;

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency;

  @Column(name = "previous_run_id")
  private Long previousRunId;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;

  @Column(length = 1000)
  private String remarks;

  @Column(name = "submitted_by", length = 50)
  private String submittedBy;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "rejection_reason", length = 300)
  private String rejectionReason;

  @Column(name = "posted_by", length = 50)
  private String postedBy;

  @Column(name = "posted_at")
  private Instant postedAt;

  @Column(name = "journal_count", nullable = false)
  private int journalCount;

  @Column(name = "cancelled_by", length = 50)
  private String cancelledBy;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "cancel_reason", length = 300)
  private String cancelReason;

  @Column(name = "cancel_date")
  private LocalDate cancelDate;

  @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private final List<RunLine> lines = new ArrayList<>();

  protected ValuationRun() {}

  /**
   * Creates a run in preview.
   *
   * @param companyId company
   * @param valuationDate valuation (month-end) date
   * @param baseCurrency company base currency of all amounts
   * @param when creation time (replaced by each calculation)
   */
  public ValuationRun(Long companyId, LocalDate valuationDate, String baseCurrency, Instant when) {
    this.companyId = companyId;
    this.valuationDate = valuationDate;
    this.periodName = valuationDate.toString().substring(0, "yyyy-MM".length());
    this.baseCurrency = baseCurrency;
    this.calculatedAt = when;
  }

  /**
   * Stores the result of a (re)calculation. Allowed in preview only.
   *
   * @param values reserve lines
   * @param previous run the movements will be measured against, null for the first run
   * @param when calculation time
   * @param notes calculation remarks (missing parameters, absent modules)
   */
  public void calculated(
      Collection<ReserveLineValues> values, Long previous, Instant when, String notes) {
    requireStatus(RunStatus.PREVIEW, "recalculated");
    lines.clear();
    values.forEach(v -> lines.add(new RunLine(this, v)));
    this.previousRunId = previous;
    this.calculatedAt = when;
    this.remarks = notes;
  }

  /**
   * Submits the run for approval (maker).
   *
   * @param user preparer
   * @param when time
   */
  public void submit(String user, Instant when) {
    requireStatus(RunStatus.PREVIEW, "submitted");
    this.status = RunStatus.PENDING_APPROVAL;
    this.submittedBy = user;
    this.submittedAt = when;
    this.rejectionReason = null;
  }

  /**
   * Approves the run (checker, never the preparer).
   *
   * @param user checker
   * @param when time
   */
  public void approve(String user, Instant when) {
    requireStatus(RunStatus.PENDING_APPROVAL, "approved");
    if (Objects.equals(user, submittedBy)) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A valuation run cannot be approved by its preparer");
    }
    this.status = RunStatus.APPROVED;
    this.approvedBy = user;
    this.approvedAt = when;
  }

  /**
   * Sends a submitted run back to preview.
   *
   * @param reason reason
   */
  public void reject(String reason) {
    requireStatus(RunStatus.PENDING_APPROVAL, "rejected");
    this.status = RunStatus.PREVIEW;
    this.rejectionReason = reason;
  }

  /**
   * Records the posting of the movement journals.
   *
   * @param user user
   * @param when time
   * @param journals number of journals posted
   * @param previous posted run the movements were measured against, null for the first one
   */
  public void posted(String user, Instant when, int journals, Long previous) {
    requireStatus(RunStatus.APPROVED, "posted");
    this.previousRunId = previous;
    this.status = RunStatus.POSTED;
    this.postedBy = user;
    this.postedAt = when;
    this.journalCount = journals;
  }

  /**
   * Cancels the run.
   *
   * @param user user
   * @param when time
   * @param reason reason
   * @param reversalDate value date of the reversal journals (posted runs), else null
   */
  public void cancel(String user, Instant when, String reason, LocalDate reversalDate) {
    if (!CANCELLABLE.contains(status)) {
      throw new BusinessRuleException(
          "RUN_NOT_CANCELLABLE", "Valuation run " + periodName + " is already cancelled");
    }
    this.status = RunStatus.CANCELLED;
    this.cancelledBy = user;
    this.cancelledAt = when;
    this.cancelReason = reason;
    this.cancelDate = reversalDate;
  }

  private void requireStatus(RunStatus expected, String action) {
    if (status != expected) {
      throw new BusinessRuleException(
          "RUN_STATUS",
          "Valuation run "
              + periodName
              + " is "
              + status
              + " and cannot be "
              + action
              + " (expected "
              + expected
              + ")");
    }
  }

  /**
   * Whether journals of this run are in the ledger.
   *
   * @return true when posted
   */
  public boolean isPosted() {
    return status == RunStatus.POSTED;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public LocalDate getValuationDate() {
    return valuationDate;
  }

  public String getPeriodName() {
    return periodName;
  }

  public RunStatus getStatus() {
    return status;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public Long getPreviousRunId() {
    return previousRunId;
  }

  public Instant getCalculatedAt() {
    return calculatedAt;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public String getPostedBy() {
    return postedBy;
  }

  public Instant getPostedAt() {
    return postedAt;
  }

  public int getJournalCount() {
    return journalCount;
  }

  public String getCancelledBy() {
    return cancelledBy;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public LocalDate getCancelDate() {
    return cancelDate;
  }

  public List<RunLine> getLines() {
    return lines;
  }
}
