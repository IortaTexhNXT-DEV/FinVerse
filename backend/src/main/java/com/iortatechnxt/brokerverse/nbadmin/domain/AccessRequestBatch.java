package com.iortatechnxt.brokerverse.nbadmin.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A bulk access request (BRD 1.009): one line request ({@link AccessRequest} with this batch) per
 * row of the uploaded file. The batch is submitted and decided as a whole (UQ10); its status
 * mirrors the statuses of its lines.
 */
@Entity
@Table(name = "nba_access_request_batch")
public class AccessRequestBatch extends BaseEntity {

  @Column(name = "batch_no", nullable = false, length = 30, updatable = false)
  private String batchNo;

  @Column(name = "file_name", length = 255)
  private String fileName;

  @Column(nullable = false)
  private int lines;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccessBatchStatus status = AccessBatchStatus.DRAFT;

  @Column(length = 1000)
  private String remarks;

  protected AccessRequestBatch() {}

  /**
   * Creates an empty draft batch.
   *
   * @param batchNo batch number (the bulk upload number)
   * @param fileName uploaded file
   */
  public AccessRequestBatch(String batchNo, String fileName) {
    this.batchNo = batchNo;
    this.fileName = fileName;
  }

  /** Counts one more line. */
  public void addLine() {
    lines++;
  }

  /**
   * Sets the batch remarks (entered on submission).
   *
   * @param text remarks
   */
  public void setRemarks(String text) {
    this.remarks = text;
  }

  /**
   * Derives the batch status from the statuses of its lines.
   *
   * @param lineStatuses statuses of the lines
   */
  public void mirror(Collection<AccessRequestStatus> lineStatuses) {
    this.lines = lineStatuses.size();
    this.status = statusOf(lineStatuses);
  }

  static AccessBatchStatus statusOf(Collection<AccessRequestStatus> statuses) {
    Set<AccessRequestStatus> distinct =
        statuses.isEmpty()
            ? EnumSet.noneOf(AccessRequestStatus.class)
            : statuses.stream()
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(AccessRequestStatus.class)));
    distinct.remove(AccessRequestStatus.CANCELLED);
    if (distinct.isEmpty()) {
      return statuses.isEmpty() ? AccessBatchStatus.DRAFT : AccessBatchStatus.CANCELLED;
    }
    if (distinct.contains(AccessRequestStatus.DRAFT)) {
      return AccessBatchStatus.DRAFT;
    }
    if (distinct.contains(AccessRequestStatus.PENDING)
        || distinct.contains(AccessRequestStatus.PENDING_SECOND)) {
      return AccessBatchStatus.PENDING;
    }
    if (distinct.contains(AccessRequestStatus.RETURNED)) {
      return AccessBatchStatus.RETURNED;
    }
    return decided(distinct);
  }

  private static AccessBatchStatus decided(Set<AccessRequestStatus> distinct) {
    if (distinct.equals(EnumSet.of(AccessRequestStatus.REJECTED))) {
      return AccessBatchStatus.REJECTED;
    }
    distinct.remove(AccessRequestStatus.SCHEDULED);
    distinct.remove(AccessRequestStatus.APPROVED);
    return distinct.isEmpty() ? AccessBatchStatus.APPROVED : AccessBatchStatus.PARTIAL;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public String getFileName() {
    return fileName;
  }

  public int getLines() {
    return lines;
  }

  public AccessBatchStatus getStatus() {
    return status;
  }

  public String getRemarks() {
    return remarks;
  }
}
