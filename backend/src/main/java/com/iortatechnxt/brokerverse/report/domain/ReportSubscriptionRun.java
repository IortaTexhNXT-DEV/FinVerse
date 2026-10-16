package com.iortatechnxt.brokerverse.report.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** One run of a report subscription on a business date (G04). */
@Entity
@Table(name = "report_subscription_run")
public class ReportSubscriptionRun extends BaseEntity {

  private static final int MAX_DETAIL = 1000;

  @Column(name = "subscription_id", nullable = false, updatable = false)
  private Long subscriptionId;

  @Column(name = "run_date", nullable = false, updatable = false)
  private LocalDate runDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12, updatable = false)
  private Outcome status;

  @Column(name = "report_run_id", updatable = false)
  private Long reportRunId;

  @Column(name = "row_count", nullable = false, updatable = false)
  private int rowCount;

  @Column(name = "message_id", updatable = false)
  private Long messageId;

  @Column(length = MAX_DETAIL, updatable = false)
  private String detail;

  protected ReportSubscriptionRun() {}

  /**
   * Records a run.
   *
   * @param subscriptionId subscription
   * @param runDate business date
   * @param result outcome
   */
  public ReportSubscriptionRun(Long subscriptionId, LocalDate runDate, Result result) {
    this.subscriptionId = subscriptionId;
    this.runDate = runDate;
    this.status = result.status();
    this.reportRunId = result.reportRunId();
    this.rowCount = result.rowCount();
    this.messageId = result.messageId();
    String text = result.detail();
    this.detail =
        text == null || text.length() <= MAX_DETAIL ? text : text.substring(0, MAX_DETAIL);
  }

  public Long getSubscriptionId() {
    return subscriptionId;
  }

  public LocalDate getRunDate() {
    return runDate;
  }

  public Outcome getStatus() {
    return status;
  }

  public Long getReportRunId() {
    return reportRunId;
  }

  public int getRowCount() {
    return rowCount;
  }

  public Long getMessageId() {
    return messageId;
  }

  public String getDetail() {
    return detail;
  }

  /** Outcome of a run. */
  public enum Outcome {
    /** Generated, archived and e-mailed. */
    SUCCEEDED,
    /** The report or the e-mail failed. */
    FAILED,
    /** Refused: the subscriber has lost the report permission or the data scope. */
    DENIED
  }

  /**
   * What a run produced.
   *
   * @param status outcome
   * @param reportRunId archived report run
   * @param rowCount rows of the report
   * @param messageId e-mail
   * @param detail reason of a failure or refusal, or the file sent
   */
  public record Result(
      Outcome status, Long reportRunId, int rowCount, Long messageId, String detail) {}
}
