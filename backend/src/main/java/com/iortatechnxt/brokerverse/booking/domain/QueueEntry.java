package com.iortatechnxt.brokerverse.booking.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * An account queued for the next booking batch (BRNB.036/076), with the optional booking date and
 * cost center the user set before the batch runs.
 */
@Entity
@Table(name = "bkg_queue")
public class QueueEntry extends BaseEntity {

  private static final int MAX_ERROR = 1000;

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(name = "account_id", nullable = false, updatable = false)
  private Long accountId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private QueueStatus status = QueueStatus.QUEUED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private QueueSource source;

  @Column(name = "booking_date")
  private LocalDate bookingDate;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(name = "queued_by", nullable = false, length = 50, updatable = false)
  private String queuedBy;

  @Column(name = "queued_at", nullable = false, updatable = false)
  private Instant queuedAt;

  @Column(name = "last_error", length = MAX_ERROR)
  private String lastError;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "batch_run_no", length = 40)
  private String batchRunNo;

  protected QueueEntry() {}

  /**
   * Queues an account.
   *
   * @param companyId company
   * @param arn account
   * @param accountId account id
   * @param source who queued it
   * @param user user
   * @param when time
   */
  public QueueEntry(
      Long companyId, String arn, Long accountId, QueueSource source, String user, Instant when) {
    this.companyId = companyId;
    this.arn = arn;
    this.accountId = accountId;
    this.source = source;
    this.queuedBy = user;
    this.queuedAt = when;
  }

  /**
   * Sets the booking date and cost center to use when the batch books the account.
   *
   * @param date booking date, null for the batch date
   * @param center cost center, null for the account's
   */
  public void edit(LocalDate date, String center) {
    requireQueued();
    this.bookingDate = date;
    this.costCenter = center == null || center.isBlank() ? null : center.strip();
  }

  /** Removes the account from the queue. */
  public void remove() {
    requireQueued();
    this.status = QueueStatus.REMOVED;
  }

  /** A failure is cleared once the account is queued again or booked another way. */
  public void clearFailure() {
    if (status == QueueStatus.FAILED) {
      this.status = QueueStatus.REMOVED;
    }
  }

  /**
   * The batch booked the account.
   *
   * @param runNo batch run
   * @param number invoice number
   */
  public void booked(String runNo, String number) {
    this.status = QueueStatus.BOOKED;
    this.batchRunNo = runNo;
    this.invoiceNo = number;
    this.lastError = null;
  }

  /**
   * The batch could not book the account.
   *
   * @param runNo batch run
   * @param error reason
   */
  public void failed(String runNo, String error) {
    this.status = QueueStatus.FAILED;
    this.batchRunNo = runNo;
    this.lastError =
        error == null || error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR);
  }

  private void requireQueued() {
    if (status != QueueStatus.QUEUED) {
      throw new BusinessRuleException(
          "QUEUE_ENTRY_CLOSED", "Account " + arn + " is no longer queued (" + status + ")");
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getArn() {
    return arn;
  }

  public Long getAccountId() {
    return accountId;
  }

  public QueueStatus getStatus() {
    return status;
  }

  public QueueSource getSource() {
    return source;
  }

  public LocalDate getBookingDate() {
    return bookingDate;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getQueuedBy() {
    return queuedBy;
  }

  public Instant getQueuedAt() {
    return queuedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getBatchRunNo() {
    return batchRunNo;
  }
}
