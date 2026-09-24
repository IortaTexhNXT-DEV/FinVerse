package com.iortatechnxt.brokerverse.bulk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;

/** One uploaded row as sanitised, with its outcome. */
@Entity
@Table(name = "bulk_row")
public class BulkRowRecord {

  private static final int MAX_MESSAGES = 2000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_id", nullable = false, updatable = false)
  private Long jobId;

  @Column(name = "row_no", nullable = false, updatable = false)
  private int rowNo;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String data;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BulkRowStatus status;

  @Column(length = MAX_MESSAGES)
  private String messages;

  @Column(name = "result_ref", length = 100)
  private String resultRef;

  protected BulkRowRecord() {}

  /**
   * Creates a validated row.
   *
   * @param jobId job
   * @param rowNo row number in the file (header = 1)
   * @param data row values as JSON
   * @param errors validation errors (empty = valid)
   */
  public BulkRowRecord(Long jobId, int rowNo, String data, List<String> errors) {
    this.jobId = jobId;
    this.rowNo = rowNo;
    this.data = data;
    this.status = errors.isEmpty() ? BulkRowStatus.VALID : BulkRowStatus.INVALID;
    this.messages = errors.isEmpty() ? null : join(errors);
  }

  /**
   * Records a successful commit.
   *
   * @param reference created or updated record
   */
  public void committed(String reference) {
    this.status = BulkRowStatus.COMMITTED;
    this.resultRef = reference;
  }

  /**
   * Records a failed commit.
   *
   * @param error reason
   */
  public void failed(String error) {
    this.status = BulkRowStatus.FAILED;
    this.messages = join(List.of(error == null ? "Unexpected error" : error));
  }

  private static String join(List<String> errors) {
    String text = String.join("; ", errors);
    return text.length() <= MAX_MESSAGES ? text : text.substring(0, MAX_MESSAGES);
  }

  public Long getId() {
    return id;
  }

  public Long getJobId() {
    return jobId;
  }

  public int getRowNo() {
    return rowNo;
  }

  public String getData() {
    return data;
  }

  public BulkRowStatus getStatus() {
    return status;
  }

  public String getMessages() {
    return messages;
  }

  public String getResultRef() {
    return resultRef;
  }
}
