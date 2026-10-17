package com.iortatechnxt.brokerverse.productmaint.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A product master change waiting for, or sent to, the other BDOI systems (BDOI FRS FRPM.029.01):
 * the product, version, kind of change (RELEASED, RETIRED, EXPIRED) and effective date, with the
 * transfer file, the status (PENDING, SENT, FAILED), the attempts and the last error.
 */
@Entity
@Table(name = "pm_master_change")
public class MasterChange extends BaseEntity {

  /** Waiting for the next transfer. */
  public static final String PENDING = "PENDING";

  /** Delivered to the receiving system. */
  public static final String SENT = "SENT";

  /** The last transfer failed. */
  public static final String FAILED = "FAILED";

  private static final int MAX_ERROR = 500;

  @Column(name = "product_code", nullable = false, length = 20, updatable = false)
  private String productCode;

  @Column(name = "version_no", updatable = false)
  private Integer versionNo;

  @Column(name = "change_kind", nullable = false, length = 20, updatable = false)
  private String changeKind;

  @Column(name = "effective_date", updatable = false)
  private LocalDate effectiveDate;

  @Column(name = "source_request_no", length = 40, updatable = false)
  private String sourceRequestNo;

  @Column(nullable = false, length = 20)
  private String status = PENDING;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "file_name", length = 120)
  private String fileName;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(length = MAX_ERROR)
  private String error;

  /** For JPA. */
  protected MasterChange() {}

  /**
   * Records a change.
   *
   * @param productCode product
   * @param versionNo version, null for a retirement
   * @param changeKind RELEASED, RETIRED or EXPIRED
   * @param effectiveDate date the change applies from
   * @param sourceRequestNo package request, may be null
   */
  public MasterChange(
      String productCode,
      Integer versionNo,
      String changeKind,
      LocalDate effectiveDate,
      String sourceRequestNo) {
    this.productCode = productCode;
    this.versionNo = versionNo;
    this.changeKind = changeKind;
    this.effectiveDate = effectiveDate;
    this.sourceRequestNo = sourceRequestNo;
  }

  /**
   * The change was delivered in a file.
   *
   * @param file file name
   * @param when delivery time
   */
  public void sent(String file, Instant when) {
    this.status = SENT;
    this.attempts++;
    this.fileName = file;
    this.sentAt = when;
    this.error = null;
  }

  /**
   * The delivery failed.
   *
   * @param file file name
   * @param message why
   */
  public void failed(String file, String message) {
    this.status = FAILED;
    this.attempts++;
    this.fileName = file;
    this.error =
        message == null || message.length() <= MAX_ERROR
            ? message
            : message.substring(0, MAX_ERROR);
  }

  /** Puts a failed change back in the queue (reprocessing). */
  public void retry() {
    this.status = PENDING;
  }

  public String getProductCode() {
    return productCode;
  }

  public Integer getVersionNo() {
    return versionNo;
  }

  public String getChangeKind() {
    return changeKind;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public String getSourceRequestNo() {
    return sourceRequestNo;
  }

  public String getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public String getFileName() {
    return fileName;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getError() {
    return error;
  }
}
