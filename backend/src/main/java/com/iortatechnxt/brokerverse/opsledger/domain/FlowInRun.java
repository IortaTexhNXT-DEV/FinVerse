package com.iortatechnxt.brokerverse.opsledger.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One run of a feed with its counts and outcome: the fetch log of BRQID.005 (timestamp, status,
 * records read, accepted, duplicates and failures, error detail).
 */
@Entity
@Table(name = "ops_flow_in_run")
public class FlowInRun extends BaseEntity {

  private static final int MAX_MESSAGE = 1000;

  @Column(name = "feed_code", nullable = false, length = 40, updatable = false)
  private String feedCode;

  @Column(name = "run_no", nullable = false, length = 30, updatable = false)
  private String runNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
  private FlowInEnums.Trigger trigger;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private FlowInEnums.RunStatus status = FlowInEnums.RunStatus.RUNNING;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "read_count", nullable = false)
  private int readCount;

  @Column(name = "ok_count", nullable = false)
  private int okCount;

  @Column(name = "duplicate_count", nullable = false)
  private int duplicateCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Column(name = "file_name", length = 255, updatable = false)
  private String fileName;

  @Column(name = "file_sha256", length = 64, updatable = false)
  private String fileSha256;

  @Column(length = MAX_MESSAGE)
  private String message;

  @Column(name = "error_detail", columnDefinition = "text")
  private String errorDetail;

  protected FlowInRun() {}

  /**
   * Starts a run.
   *
   * @param feedCode feed
   * @param runNo run number
   * @param trigger what started it
   * @param file uploaded file name and checksum, both may be null
   * @param at start time
   */
  public FlowInRun(
      String feedCode, String runNo, FlowInEnums.Trigger trigger, FileRef file, Instant at) {
    this.feedCode = feedCode;
    this.runNo = runNo;
    this.trigger = trigger;
    this.fileName = file.fileName();
    this.fileSha256 = file.sha256();
    this.startedAt = at;
  }

  /**
   * Counts one record.
   *
   * @param status accepted or failed; null for a duplicate skipped
   */
  public void count(FlowInEnums.RecordStatus status) {
    readCount++;
    if (status == null) {
      duplicateCount++;
    } else if (status == FlowInEnums.RecordStatus.ACCEPTED) {
      okCount++;
    } else {
      failedCount++;
    }
  }

  /**
   * Ends the run: SUCCEEDED, PARTIAL when some records failed, FAILED when the run broke or every
   * record failed.
   *
   * @param summary message
   * @param error error detail of a broken run, else null
   * @param at end time
   */
  public void finish(String summary, String error, Instant at) {
    this.endedAt = at;
    this.message =
        summary == null || summary.length() <= MAX_MESSAGE
            ? summary
            : summary.substring(0, MAX_MESSAGE);
    this.errorDetail = error;
    boolean nothingProcessed = okCount == 0 && duplicateCount == 0;
    if (error != null || failedCount > 0 && nothingProcessed) {
      status = FlowInEnums.RunStatus.FAILED;
    } else if (failedCount > 0) {
      status = FlowInEnums.RunStatus.PARTIAL;
    } else {
      status = FlowInEnums.RunStatus.SUCCEEDED;
    }
  }

  public String getFeedCode() {
    return feedCode;
  }

  public String getRunNo() {
    return runNo;
  }

  public FlowInEnums.Trigger getTrigger() {
    return trigger;
  }

  public FlowInEnums.RunStatus getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public int getReadCount() {
    return readCount;
  }

  public int getOkCount() {
    return okCount;
  }

  public int getDuplicateCount() {
    return duplicateCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileSha256() {
    return fileSha256;
  }

  public String getMessage() {
    return message;
  }

  public String getErrorDetail() {
    return errorDetail;
  }

  /**
   * An uploaded file of a run.
   *
   * @param fileName file name, null when the run has no file
   * @param sha256 SHA-256, null when the run has no file
   */
  public record FileRef(String fileName, String sha256) {

    /** A run without a file. */
    public static final FileRef NONE = new FileRef(null, null);
  }
}
