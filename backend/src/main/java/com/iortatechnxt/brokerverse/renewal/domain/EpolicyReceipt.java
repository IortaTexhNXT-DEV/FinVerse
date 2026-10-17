package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A receipt of e-policy files (FRRN.033.01, FRRN.033.02): the summary file and the ZIP of e-policy
 * documents of an insurer, uploaded or received by MFT, with its processing outcome.
 */
@Entity
@Table(name = "rnw_epolicy_receipt")
public class EpolicyReceipt extends BaseEntity {

  /** Processing status: processed. */
  public static final String SUCCESSFUL = "SUCCESSFUL";

  /** Processing status: refused. */
  public static final String FAILED = "FAILED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "receipt_no", nullable = false, length = 30, updatable = false)
  private String receiptNo;

  @Column(name = "receipt_type", nullable = false, length = 10, updatable = false)
  private String receiptType;

  @Column(name = "summary_file", length = 200, updatable = false)
  private String summaryFile;

  @Column(name = "zip_file", length = 200, updatable = false)
  private String zipFile;

  @Column(nullable = false, length = 20)
  private String status = SUCCESSFUL;

  @Column(length = 1000)
  private String remarks;

  @Column(nullable = false)
  private int records;

  @Column(nullable = false)
  private int matched;

  /** For JPA. */
  protected EpolicyReceipt() {}

  /**
   * A receipt.
   *
   * @param companyId company
   * @param receiptNo receipt number
   * @param receiptType UPLOAD or MFT
   * @param summaryFile summary file name
   * @param zipFile ZIP file name
   */
  public EpolicyReceipt(
      Long companyId, String receiptNo, String receiptType, String summaryFile, String zipFile) {
    this.companyId = companyId;
    this.receiptNo = receiptNo;
    this.receiptType = receiptType;
    this.summaryFile = summaryFile;
    this.zipFile = zipFile;
  }

  /**
   * Records a refusal of the files.
   *
   * @param reason reason
   */
  public void failed(String reason) {
    this.status = FAILED;
    this.remarks = reason;
  }

  /**
   * Records the outcome.
   *
   * @param total records of the summary file
   * @param matchedRecords matched records
   */
  public void processed(int total, int matchedRecords) {
    this.records = total;
    this.matched = matchedRecords;
    if (matchedRecords < total) {
      this.remarks = (total - matchedRecords) + " of " + total + " records unmatched";
    }
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getReceiptNo() {
    return receiptNo;
  }

  public String getReceiptType() {
    return receiptType;
  }

  public String getSummaryFile() {
    return summaryFile;
  }

  public String getZipFile() {
    return zipFile;
  }

  public String getStatus() {
    return status;
  }

  public String getRemarks() {
    return remarks;
  }

  public int getRecords() {
    return records;
  }

  public int getMatched() {
    return matched;
  }
}
