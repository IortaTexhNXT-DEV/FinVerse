package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One extraction of expiring policies (BRRN.030; RENEWAL_DESIGN section 4.1): the daily job, a
 * range generated on demand, an upload of legacy policies or the go-live take-over, with its range
 * and counts.
 */
@Entity(name = "RnwExtractionRun")
@Table(name = "rnw_extraction_run")
public class ExtractionRun extends BaseEntity {

  /** Status of a run. */
  public enum Status {
    /** In progress. */
    RUNNING,
    /** Completed. */
    COMPLETED,
    /** Failed; the next run picks up the missed dates. */
    FAILED
  }

  private static final int MESSAGE_LENGTH = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_kind", nullable = false, length = 20, updatable = false)
  private ExtractionTrigger trigger;

  @Column(name = "expiry_from")
  private LocalDate expiryFrom;

  @Column(name = "expiry_to")
  private LocalDate expiryTo;

  @Column(name = "requested_by", nullable = false, length = 50, updatable = false)
  private String requestedBy;

  @Column(name = "read_count", nullable = false)
  private int readCount;

  @Column(name = "new_count", nullable = false)
  private int newCount;

  @Column(name = "existing_count", nullable = false)
  private int existingCount;

  @Column(name = "skipped_count", nullable = false)
  private int skippedCount;

  @Column(name = "urgent_count", nullable = false)
  private int urgentCount;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.RUNNING;

  @Column(length = MESSAGE_LENGTH)
  private String message;

  protected ExtractionRun() {}

  /**
   * Starts a run.
   *
   * @param companyId company
   * @param runNo run number (RXR-yyyy-nnnnnn)
   * @param trigger what started it
   * @param expiryFrom first expiry date
   * @param expiryTo last expiry date
   * @param requestedBy user or job
   * @param startedAt start
   */
  public ExtractionRun(
      Long companyId,
      String runNo,
      ExtractionTrigger trigger,
      LocalDate expiryFrom,
      LocalDate expiryTo,
      String requestedBy,
      Instant startedAt) {
    this.companyId = companyId;
    this.runNo = runNo;
    this.trigger = trigger;
    this.expiryFrom = expiryFrom;
    this.expiryTo = expiryTo;
    this.requestedBy = requestedBy;
    this.startedAt = startedAt;
  }

  /**
   * Completes the run with its counts.
   *
   * @param counts read, new, existing, skipped and urgent
   * @param at end
   */
  public void complete(Counts counts, Instant at) {
    this.readCount = counts.read();
    this.newCount = counts.created();
    this.existingCount = counts.existing();
    this.skippedCount = counts.skipped();
    this.urgentCount = counts.urgent();
    this.status = Status.COMPLETED;
    this.endedAt = at;
    this.message = counts.summary();
  }

  /**
   * Marks the run failed.
   *
   * @param reason error text
   * @param at end
   */
  public void fail(String reason, Instant at) {
    this.status = Status.FAILED;
    this.endedAt = at;
    this.message =
        reason == null || reason.length() <= MESSAGE_LENGTH
            ? reason
            : reason.substring(0, MESSAGE_LENGTH);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRunNo() {
    return runNo;
  }

  public ExtractionTrigger getTrigger() {
    return trigger;
  }

  public LocalDate getExpiryFrom() {
    return expiryFrom;
  }

  public LocalDate getExpiryTo() {
    return expiryTo;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public int getReadCount() {
    return readCount;
  }

  public int getNewCount() {
    return newCount;
  }

  public int getExistingCount() {
    return existingCount;
  }

  public int getSkippedCount() {
    return skippedCount;
  }

  public int getUrgentCount() {
    return urgentCount;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  /**
   * Counts of a run.
   *
   * @param read records read
   * @param created candidates created
   * @param existing already a candidate (duplicate guard)
   * @param skipped excluded or not renewable here (Employee Benefits lines, renewed in legacy)
   * @param urgent created with the urgent flag
   */
  public record Counts(int read, int created, int existing, int skipped, int urgent) {

    /**
     * One-line summary.
     *
     * @return text
     */
    public String summary() {
      return read
          + " read, "
          + created
          + " new, "
          + existing
          + " already extracted, "
          + skipped
          + " skipped"
          + (urgent > 0 ? ", " + urgent + " urgent" : "");
    }
  }
}
