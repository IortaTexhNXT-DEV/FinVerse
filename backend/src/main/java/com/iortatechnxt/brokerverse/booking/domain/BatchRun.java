package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One booking batch run (BRNB.036): who or what started it, the business date and the result of
 * every account (partial success: each account is booked in its own transaction).
 */
@Entity
@Table(name = "bkg_batch_run")
public class BatchRun extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, length = 40, updatable = false)
  private String runNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
  private BatchTrigger trigger;

  @Column(name = "business_date", nullable = false, updatable = false)
  private LocalDate businessDate;

  @Column(name = "started_by", nullable = false, length = 50, updatable = false)
  private String startedBy;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "booked_count", nullable = false)
  private int bookedCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @ElementCollection
  @CollectionTable(name = "bkg_batch_row", joinColumns = @JoinColumn(name = "run_id"))
  @OrderColumn(name = "row_index")
  private final List<BatchRow> rows = new ArrayList<>();

  protected BatchRun() {}

  /**
   * Starts a run.
   *
   * @param companyId company
   * @param runNo run number
   * @param trigger what started it
   * @param businessDate business date
   * @param user user (or SYSTEM)
   * @param when start time
   */
  public BatchRun(
      Long companyId,
      String runNo,
      BatchTrigger trigger,
      LocalDate businessDate,
      String user,
      Instant when) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.trigger = trigger;
    this.businessDate = businessDate;
    this.startedBy = user;
    this.startedAt = when;
  }

  /**
   * Records the results and closes the run.
   *
   * @param results one row per account
   * @param when end time
   */
  public void finish(List<BatchRow> results, Instant when) {
    rows.addAll(results);
    bookedCount = (int) results.stream().filter(BatchRow::isBooked).count();
    failedCount = results.size() - bookedCount;
    finishedAt = when;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public BatchTrigger getTrigger() {
    return trigger;
  }

  public LocalDate getBusinessDate() {
    return businessDate;
  }

  public String getStartedBy() {
    return startedBy;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public int getBookedCount() {
    return bookedCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public List<BatchRow> getRows() {
    return List.copyOf(rows);
  }
}
