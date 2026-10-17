package com.iortatechnxt.brokerverse.report.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One report of a {@link ReportBatch} with its outcome. */
@Entity
@Table(name = "report_batch_item")
public class ReportBatchItem extends BaseEntity {

  private static final int MAX_ERROR = 1000;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false)
  private ReportBatch batch;

  @Column(name = "item_no", nullable = false)
  private int itemNo;

  @Column(name = "report_code", nullable = false, length = 40)
  private String reportCode;

  @Column(nullable = false, length = 10)
  private String status;

  @Column(name = "row_count", nullable = false)
  private int rowCount;

  @Column(length = MAX_ERROR)
  private String error;

  protected ReportBatchItem() {}

  ReportBatchItem(
      ReportBatch batch, int itemNo, String reportCode, boolean ok, int rowCount, String error) {
    this.batch = batch;
    this.itemNo = itemNo;
    this.reportCode = reportCode;
    this.status = ok ? "OK" : "FAILED";
    this.rowCount = rowCount;
    this.error =
        error == null || error.length() <= MAX_ERROR ? error : error.substring(0, MAX_ERROR);
  }

  public int getItemNo() {
    return itemNo;
  }

  public String getReportCode() {
    return reportCode;
  }

  public String getStatus() {
    return status;
  }

  /**
   * Whether the report was produced.
   *
   * @return true when OK
   */
  public boolean isOk() {
    return "OK".equals(status);
  }

  public int getRowCount() {
    return rowCount;
  }

  public String getError() {
    return error;
  }
}
