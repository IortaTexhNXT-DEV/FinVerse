package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The log of one ingestion of a list file (SNSRP-201): source, trigger, file, records received,
 * added, updated, delisted, unchanged and failed, status and reason for failure. A run of a manual
 * upload stages its changes for approval ({@link #isPendingApproval()}); a scheduled run of the
 * official feed applies them at once.
 */
@Entity
@Table(name = "scr_ingestion_run")
public class IngestionRun extends BaseEntity {

  private static final int MAX_ERROR = 2000;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Column(name = "source_id", nullable = false, updatable = false)
  private Long sourceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_code", nullable = false, length = 20, updatable = false)
  private IngestionTrigger trigger;

  @Column(name = "file_name", length = 255, updatable = false)
  private String fileName;

  @Column(name = "file_attachment_id")
  private Long fileAttachmentId;

  @Column(name = "received", nullable = false)
  private int received;

  @Column(name = "added", nullable = false)
  private int added;

  @Column(name = "updated", nullable = false)
  private int updated;

  @Column(name = "delisted", nullable = false)
  private int delisted;

  @Column(name = "unchanged", nullable = false)
  private int unchanged;

  @Column(name = "failed", nullable = false)
  private int failed;

  @Column(name = "pending_approval", nullable = false, updatable = false)
  private boolean pendingApproval;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RunStatus status = RunStatus.RUNNING;

  @Column(name = "error", length = MAX_ERROR)
  private String error;

  @Column(name = "job_run_id")
  private Long jobRunId;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  /** For JPA. */
  protected IngestionRun() {}

  /**
   * Starts a run.
   *
   * @param runNo run number
   * @param sourceId source
   * @param trigger trigger
   * @param fileName file name, may be {@code null}
   * @param pendingApproval whether the changes wait for a checker (manual upload)
   * @param startedAt start time
   */
  public IngestionRun(
      String runNo,
      Long sourceId,
      IngestionTrigger trigger,
      String fileName,
      boolean pendingApproval,
      Instant startedAt) {
    this.runNo = runNo;
    this.sourceId = sourceId;
    this.trigger = trigger;
    this.fileName = fileName;
    this.pendingApproval = pendingApproval;
    this.startedAt = startedAt;
  }

  /**
   * Links the stored file (attachment) and the job run.
   *
   * @param attachmentId attachment id, may be {@code null}
   * @param jobRun job run id, may be {@code null}
   */
  public void link(Long attachmentId, Long jobRun) {
    this.fileAttachmentId = attachmentId;
    this.jobRunId = jobRun;
  }

  /**
   * Records the counts and ends the run: SUCCESS, or PARTIAL when some records failed.
   *
   * @param counts the counts
   * @param when end time
   */
  public void complete(RunCounts counts, Instant when) {
    this.received = counts.received();
    this.added = counts.added();
    this.updated = counts.updated();
    this.delisted = counts.delisted();
    this.unchanged = counts.unchanged();
    this.failed = counts.failed();
    this.status = counts.failed() > 0 ? RunStatus.PARTIAL : RunStatus.SUCCESS;
    this.endedAt = when;
  }

  /**
   * Ends the run as FAILED with the reason (file missing or unreadable).
   *
   * @param reason reason
   * @param when end time
   */
  public void fail(String reason, Instant when) {
    this.status = RunStatus.FAILED;
    this.error =
        reason == null || reason.length() <= MAX_ERROR ? reason : reason.substring(0, MAX_ERROR);
    this.endedAt = when;
  }

  public String getRunNo() {
    return runNo;
  }

  public Long getSourceId() {
    return sourceId;
  }

  public IngestionTrigger getTrigger() {
    return trigger;
  }

  public String getFileName() {
    return fileName;
  }

  public Long getFileAttachmentId() {
    return fileAttachmentId;
  }

  public int getReceived() {
    return received;
  }

  public int getAdded() {
    return added;
  }

  public int getUpdated() {
    return updated;
  }

  public int getDelisted() {
    return delisted;
  }

  public int getUnchanged() {
    return unchanged;
  }

  public int getFailed() {
    return failed;
  }

  public boolean isPendingApproval() {
    return pendingApproval;
  }

  public RunStatus getStatus() {
    return status;
  }

  public String getError() {
    return error;
  }

  public Long getJobRunId() {
    return jobRunId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }
}
